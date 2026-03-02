package com.ovais.panorama

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import org.opencv.core.*
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream

class LivePanoramaActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private val capturedFrames = mutableListOf<Bitmap>()
    private var isCapturing by mutableStateOf(false)

    private var lastCaptureTime = 0L
    private val captureInterval = 500L // capture frame every 0.5 sec

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.loadLibrary("opencv_java4") // Load OpenCV

        setContent {
            PanoramaTheme {
                Box(Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = {
                            previewView = PreviewView(it)
                            startCamera()
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    var capturing by remember { mutableStateOf(false) }

                    Button(
                        onClick = {
                            if (capturing) {
                                // Stop capturing & stitch
                                capturing = false
                                isCapturing = false
                                if (capturedFrames.isNotEmpty()) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val panorama = stitchBitmapsOpenCV(capturedFrames)
                                        panorama?.let { bmp ->
                                            saveBitmapToGallery(bmp)
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    this@LivePanoramaActivity,
                                                    "Panorama saved!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } ?: withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                this@LivePanoramaActivity,
                                                "Stitching failed!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        capturedFrames.clear()
                                    }
                                } else {
                                    Toast.makeText(
                                        this@LivePanoramaActivity,
                                        "No frames captured!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                // Start capturing
                                capturing = true
                                isCapturing = true
                                capturedFrames.clear()
                                lastCaptureTime = 0L
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Text(if (capturing) "Stop & Stitch" else "Start Capturing")
                    }
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analyzer ->
                    analyzer.setAnalyzer(ContextCompat.getMainExecutor(this)) { image ->
                        if (isCapturing) {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastCaptureTime > captureInterval) {
                                val bmp = image.toPanoramaBitmap()?.downscale(600)
                                bmp?.let { capturedFrames.add(it) }
                                lastCaptureTime = currentTime
                            }
                        }
                        image.close()
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun ImageProxy.toPanoramaBitmap(): Bitmap? {
        return try {
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            val nv21 = ByteArray(ySize + uSize + vSize)

            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = android.graphics.YuvImage(
                nv21,
                android.graphics.ImageFormat.NV21,
                width,
                height,
                null
            )
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 90, out)
            android.graphics.BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun Bitmap.downscale(maxWidth: Int): Bitmap {
        val ratio = maxWidth.toFloat() / width
        return this.scale(maxWidth, (height * ratio).toInt())
    }

    private fun stitchBitmapsOpenCV(bitmaps: List<Bitmap>): Bitmap? {
        if (bitmaps.isEmpty()) return null
        if (bitmaps.size == 1) return bitmaps[0]

        var resultMat = Mat()
        Utils.bitmapToMat(bitmaps[0], resultMat)

        for (i in 1 until bitmaps.size) {
            val nextMat = Mat()
            Utils.bitmapToMat(bitmaps[i], nextMat)
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

            val allX = transformedCorners.toArray().map { it.x } + listOf(0.0, bgr1.cols().toDouble())
            val allY = transformedCorners.toArray().map { it.y } + listOf(0.0, bgr1.rows().toDouble())
            val minX = allX.minOrNull()!!.coerceAtMost(0.0)
            val maxX = allX.maxOrNull()!!
            val minY = allY.minOrNull()!!.coerceAtMost(0.0)
            val maxY = allY.maxOrNull()!!

            val shiftX = -minX
            val shiftY = -minY
            val width = (maxX - minX).toInt()
            val height = (maxY - minY).toInt()

            val result = Mat.zeros(height, width, CvType.CV_8UC3)
            bgr1.copyTo(result.submat(Rect(shiftX.toInt(), shiftY.toInt(), bgr1.cols(), bgr1.rows())))

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

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val filename = "Panorama_${System.currentTimeMillis()}.jpg"
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = contentResolver
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(it, contentValues, null, null)
            }
        }
    }
}