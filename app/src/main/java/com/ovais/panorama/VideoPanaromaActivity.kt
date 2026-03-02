package com.ovais.panorama

import android.content.ContentValues
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.lifecycle.lifecycleScope
import com.ovais.panorama.ui.theme.PanoramaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CaptureVideoPanoramaActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private var videoFileUri: android.net.Uri? = null
    private var recording: Recording? = null
    private var isProcessing by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.loadLibrary("opencv_java4")

        setContent {
            PanoramaTheme {
                Box(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AndroidView(factory = {
                            previewView = PreviewView(it)
                            startCamera()
                            previewView
                        }, modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp))

                        Spacer(Modifier.height(16.dp))

                        var isRecording by remember { mutableStateOf(false) }

                        Button(
                            onClick = {
                                if (!isRecording) {
                                    startRecording { uri ->
                                        videoFileUri = uri
                                        isRecording = false
                                        processVideo(uri)
                                    }
                                    isRecording = true
                                } else {
                                    stopRecording()
                                }
                            }
                        ) {
                            Text(if (isRecording) "Stop Recording" else "Start Recording")
                        }
                    }
                }
            }
        }
    }

    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var recorder: Recorder
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    videoCapture
                )
            } catch (e: Exception) {
                Log.e("Panorama", "Camera binding failed", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun startRecording(onComplete: (android.net.Uri) -> Unit) {
        val filename =
            "VIDEO_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        // Correct Builder usage
        val outputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = videoCapture.output
            .prepareRecording(this, outputOptions)
            .start(ContextCompat.getMainExecutor(this)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (!event.hasError()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                contentValues.clear()
                                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                                contentResolver.update(
                                    event.outputResults.outputUri,
                                    contentValues,
                                    null,
                                    null
                                )
                            }
                            onComplete(event.outputResults.outputUri)
                        } else {
                            Toast.makeText(
                                this,
                                "Recording error: ${event.error}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
    }

    private fun processVideo(uri: android.net.Uri) {
        isProcessing = true
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(this@CaptureVideoPanoramaActivity, uri)
                val duration =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLong()
                        ?: 0L
                val interval = 500L // extract frames every 0.5 sec
                val frames = mutableListOf<Bitmap>()

                for (timeMs in 0 until duration step interval) {
                    val frame = retriever.getFrameAtTime(
                        timeMs * 1000,
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                    frame?.let { frames.add(it) }
                }
                retriever.release()

                if (frames.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@CaptureVideoPanoramaActivity,
                            "No frames extracted!",
                            Toast.LENGTH_SHORT
                        ).show()
                        isProcessing = false
                    }
                    return@launch
                }

                val stitchedBitmap = stitchBitmapsOpenCV(frames)
                stitchedBitmap?.let { bmp ->
                    saveBitmapToGallery(bmp)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@CaptureVideoPanoramaActivity,
                            "Panorama saved!",
                            Toast.LENGTH_SHORT
                        ).show()
                        isProcessing = false
                    }
                } ?: withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CaptureVideoPanoramaActivity,
                        "Stitching failed!",
                        Toast.LENGTH_SHORT
                    ).show()
                    isProcessing = false
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CaptureVideoPanoramaActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    isProcessing = false
                }
            }
        }
    }

    private fun stitchBitmapsOpenCV(bitmaps: List<Bitmap>): Bitmap? {
        if (bitmaps.isEmpty()) return null
        if (bitmaps.size == 1) return bitmaps[0]

        var resultMat = Mat()
        Utils.bitmapToMat(bitmaps[0].downscale(600), resultMat)

        for (i in 1 until bitmaps.size) {
            val nextMat = Mat()
            Utils.bitmapToMat(bitmaps[i].downscale(600), nextMat)
            val stitched = stitchTwoImages(resultMat, nextMat) ?: continue
            resultMat.release()
            resultMat = stitched
        }

        val resultBitmap = createBitmap(resultMat.cols(), resultMat.rows())
        Utils.matToBitmap(resultMat, resultBitmap)
        resultMat.release()
        return resultBitmap
    }

    private fun stitchTwoImages(img1: Mat, img2: Mat): Mat? {
        return try {
            val bgr1 = Mat()
            val bgr2 = Mat()
            Imgproc.cvtColor(img1, bgr1, Imgproc.COLOR_RGBA2BGR)
            Imgproc.cvtColor(img2, bgr2, Imgproc.COLOR_RGBA2BGR)

            val gray1 = Mat()
            val gray2 = Mat()
            Imgproc.cvtColor(bgr1, gray1, Imgproc.COLOR_BGR2GRAY)
            Imgproc.cvtColor(bgr2, gray2, Imgproc.COLOR_BGR2GRAY)

            val orb = ORB.create(3000)
            val kp1 = MatOfKeyPoint()
            val kp2 = MatOfKeyPoint()
            val des1 = Mat()
            val des2 = Mat()
            orb.detectAndCompute(gray1, Mat(), kp1, des1)
            orb.detectAndCompute(gray2, Mat(), kp2, des2)

            if (des1.empty() || des2.empty()) return null

            val matcher = BFMatcher.create(BFMatcher.BRUTEFORCE_HAMMING, true)
            val matches = MatOfDMatch()
            matcher.match(des1, des2, matches)

            val goodMatches = matches.toList().sortedBy { it.distance }.take(50)
            if (goodMatches.size < 4) return null

            val pts1 = goodMatches.map { kp1.toArray()[it.queryIdx].pt }
            val pts2 = goodMatches.map { kp2.toArray()[it.trainIdx].pt }

            val homography = Calib3d.findHomography(
                MatOfPoint2f(*pts2.toTypedArray()),
                MatOfPoint2f(*pts1.toTypedArray()),
                Calib3d.RANSAC,
                5.0
            ) ?: return null

            val corners = arrayOf(
                Point(0.0, 0.0),
                Point(bgr2.cols().toDouble(), 0.0),
                Point(bgr2.cols().toDouble(), bgr2.rows().toDouble()),
                Point(0.0, bgr2.rows().toDouble())
            )
            val transformedCorners = MatOfPoint2f(*corners)
            Core.perspectiveTransform(MatOfPoint2f(*corners), transformedCorners, homography)
            val allX =
                transformedCorners.toArray().map { it.x } + listOf(0.0, bgr1.cols().toDouble())
            val allY =
                transformedCorners.toArray().map { it.y } + listOf(0.0, bgr1.rows().toDouble())
            val minX = allX.minOrNull()!!.coerceAtMost(0.0)
            val maxX = allX.maxOrNull()!!
            val minY = allY.minOrNull()!!.coerceAtMost(0.0)
            val maxY = allY.maxOrNull()!!

            val shiftX = -minX
            val shiftY = -minY
            val width = (maxX - minX).toInt()
            val height = (maxY - minY).toInt()

            val result = Mat.zeros(height, width, CvType.CV_8UC3)
            bgr1.copyTo(
                result.submat(
                    Rect(
                        shiftX.toInt(),
                        shiftY.toInt(),
                        bgr1.cols(),
                        bgr1.rows()
                    )
                )
            )

            val translation = Mat.eye(3, 3, CvType.CV_64F)
            translation.put(0, 2, shiftX)
            translation.put(1, 2, shiftY)
            val homographyShifted = Mat()
            Core.gemm(translation, homography, 1.0, Mat(), 0.0, homographyShifted)

            Imgproc.warpPerspective(
                bgr2,
                result,
                homographyShifted,
                Size(width.toDouble(), height.toDouble()),
                Imgproc.INTER_LINEAR,
                Core.BORDER_CONSTANT,
                Scalar(0.0, 0.0, 0.0)
            )

            val rgbaResult = Mat()
            Imgproc.cvtColor(result, rgbaResult, Imgproc.COLOR_BGR2RGBA)
            rgbaResult
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun Bitmap.downscale(maxWidth: Int): Bitmap {
        val ratio = maxWidth.toFloat() / width
        return this.scale(maxWidth, (height * ratio).toInt())
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val filename =
            "Panorama_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(uri)
                ?.use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
        }
    }
}