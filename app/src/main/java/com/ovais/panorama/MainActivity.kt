package com.ovais.panorama

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private val capturedBitmaps = mutableListOf<Bitmap>()
    private var isCapturing by mutableStateOf(false)
    private var lastCaptureTime = 0L
    private val captureInterval = 500L

    private var lastYaw = 0f
    private var currentYaw = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        System.loadLibrary("opencv_java4")
        setContent { CameraScreen() }
    }

    @Composable
    fun CameraScreen() {
        var stitchedBitmap by remember { mutableStateOf<Bitmap?>(null) }
        val capturing = isCapturing

        PanoramaTheme {
            Box(Modifier.fillMaxSize()) {

                AndroidView(
                    factory = {
                        previewView = PreviewView(it)
                        startCamera { bmp ->
                            // Update live stitched bitmap on main thread
                            stitchedBitmap = bmp
                        }
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = { startCapturing() }, enabled = !capturing) {
                        Text("Start")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { stopAndSave(stitchedBitmap) }, enabled = capturing) {
                        Text("Stop & Save")
                    }
                }

                stitchedBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
    private fun startCamera(onFrameCaptured: (Bitmap?) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(1280, 960))
                .build()

            // Use official converter
            val yuvToRgbConverter = YuvToRgbConverter(this)

            imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(this)) { image ->
                if (isCapturing) {
                    val now = System.currentTimeMillis()
                    if (now - lastCaptureTime >= captureInterval) {
                        lastCaptureTime = now
                        val bmp = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
                        yuvToRgbConverter.yuvToRgb(image, bmp)
                        capturedBitmaps.add(bmp)

                        lifecycleScope.launch(Dispatchers.Default) {
                            val stitched = stitchBitmapsOpenCV(capturedBitmaps)
                            withContext(Dispatchers.Main) { onFrameCaptured(stitched) }
                        }
                    }
                }
                image.close()
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e("Panorama", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }
    private fun startCapturing() {
        capturedBitmaps.clear()
        isCapturing = true
        lastCaptureTime = 0L
        Toast.makeText(this, "Capturing started", Toast.LENGTH_SHORT).show()
    }

    private fun stopAndSave(stitched: Bitmap?) {
        isCapturing = false
        if (capturedBitmaps.isEmpty() || stitched == null) {
            Toast.makeText(this, "No frames captured!", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            saveBitmapToGallery(stitched)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Panorama saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ImageProxy.toBitmap(): Bitmap? {
        try {
            val planes = planes
            val ySize = planes[0].buffer.remaining()
            val uSize = planes[1].buffer.remaining()
            val vSize = planes[2].buffer.remaining()
            val nv21 = ByteArray(ySize + uSize + vSize)
            planes[0].buffer.get(nv21, 0, ySize)
            planes[2].buffer.get(nv21, ySize, vSize)
            planes[1].buffer.get(nv21, ySize + vSize, uSize)

            val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
            yuvMat.put(0, 0, nv21)
            val bgrMat = Mat()
            Imgproc.cvtColor(yuvMat, bgrMat, Imgproc.COLOR_YUV2BGR_NV21)
            yuvMat.release()
            val bmp = Bitmap.createBitmap(bgrMat.cols(), bgrMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(bgrMat, bmp)
            bgrMat.release()
            return bmp
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun stitchBitmapsOpenCV(bitmaps: List<Bitmap>): Bitmap? {
        if (bitmaps.isEmpty()) return null
        var resultMat = Mat()
        Utils.bitmapToMat(bitmaps[0], resultMat)
        for (i in 1 until bitmaps.size) {
            val nextMat = Mat()
            Utils.bitmapToMat(bitmaps[i], nextMat)
            val stitched = stitchTwoImages(resultMat, nextMat) ?: continue
            resultMat.release()
            resultMat = stitched
        }
        val bmp = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resultMat, bmp)
        resultMat.release()
        return bmp
    }

    private fun stitchTwoImages(img1: Mat, img2: Mat): Mat? {
        try {
            val bgr1 = Mat()
            val bgr2 = Mat()
            Imgproc.cvtColor(img1, bgr1, Imgproc.COLOR_RGBA2BGR)
            Imgproc.cvtColor(img2, bgr2, Imgproc.COLOR_RGBA2BGR)

            val gray1 = Mat()
            val gray2 = Mat()
            Imgproc.cvtColor(bgr1, gray1, Imgproc.COLOR_BGR2GRAY)
            Imgproc.cvtColor(bgr2, gray2, Imgproc.COLOR_BGR2GRAY)

            val orb = ORB.create(2000)
            val kp1 = MatOfKeyPoint()
            val kp2 = MatOfKeyPoint()
            val des1 = Mat()
            val des2 = Mat()
            orb.detectAndCompute(gray1, Mat(), kp1, des1)
            orb.detectAndCompute(gray2, Mat(), kp2, des2)
            if (des1.empty() || des2.empty()) return null

            val matcher = BFMatcher.create(BFMatcher.BRUTEFORCE_HAMMING, false)
            val knnMatches = mutableListOf<MatOfDMatch>()
            matcher.knnMatch(des1, des2, knnMatches, 2)
            val goodMatches = mutableListOf<DMatch>()
            for (m in knnMatches) {
                val arr = m.toArray()
                if (arr.size >= 2 && arr[0].distance < 0.75 * arr[1].distance) goodMatches.add(arr[0])
            }
            if (goodMatches.size < 4) return null

            val pts1 = goodMatches.map { kp1.toArray()[it.queryIdx].pt }
            val pts2 = goodMatches.map { kp2.toArray()[it.trainIdx].pt }

            val homography = Calib3d.findHomography(
                MatOfPoint2f(*pts2.toTypedArray()),
                MatOfPoint2f(*pts1.toTypedArray()),
                Calib3d.RANSAC, 5.0
            ) ?: return null

            val corners = arrayOf(
                Point(0.0, 0.0),
                Point(bgr2.cols().toDouble(), 0.0),
                Point(bgr2.cols().toDouble(), bgr2.rows().toDouble()),
                Point(0.0, bgr2.rows().toDouble())
            )
            val transformedCorners = MatOfPoint2f(*corners).apply { Core.perspectiveTransform(this, this, homography) }.toArray()
            val allX = transformedCorners.map { it.x } + listOf(0.0, bgr1.cols().toDouble())
            val allY = transformedCorners.map { it.y } + listOf(0.0, bgr1.rows().toDouble())
            val minX = allX.minOrNull()!!.coerceAtMost(0.0)
            val minY = allY.minOrNull()!!.coerceAtMost(0.0)
            val maxX = allX.maxOrNull()!!
            val maxY = allY.maxOrNull()!!
            val width = (maxX - minX).toInt()
            val height = (maxY - minY).toInt()
            val shiftX = -minX
            val shiftY = -minY

            val result = Mat.zeros(height, width, CvType.CV_8UC3)
            bgr1.copyTo(result.submat(Rect(shiftX.toInt(), shiftY.toInt(), bgr1.cols(), bgr1.rows())))
            val translation = Mat.eye(3, 3, CvType.CV_64F)
            translation.put(0, 2, shiftX)
            translation.put(1, 2, shiftY)
            val homographyShifted = Mat()
            Core.gemm(translation, homography, 1.0, Mat(), 0.0, homographyShifted)
            Imgproc.warpPerspective(
                bgr2, result, homographyShifted,
                Size(width.toDouble(), height.toDouble()),
                Imgproc.INTER_LINEAR, Core.BORDER_TRANSPARENT, Scalar(0.0, 0.0, 0.0)
            )

            val rgbaResult = Mat()
            Imgproc.cvtColor(result, rgbaResult, Imgproc.COLOR_BGR2RGBA)
            return rgbaResult
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val filename = "Panorama_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(uri)?.use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
        }
    }
}

class YuvToRgbConverter(context: Context) {
    private val renderScript = RenderScript.create(context)
    private val yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(renderScript, Element.U8_4(renderScript))
    private var yuvByteArray = ByteArray(0)
    private var inputAllocation: Allocation? = null
    private var outputAllocation: Allocation? = null

    fun yuvToRgb(image: ImageProxy, output: Bitmap) {
        val yuvBytes = getYuvBytes(image)
        if (yuvBytes != null) {
            if (inputAllocation == null || inputAllocation!!.bytesSize != yuvBytes.size) {
                inputAllocation = Allocation.createSized(renderScript, Element.U8(renderScript), yuvBytes.size)
                outputAllocation = Allocation.createFromBitmap(renderScript, output)
            }
            inputAllocation!!.copyFrom(yuvBytes)
            yuvToRgbIntrinsic.setInput(inputAllocation)
            yuvToRgbIntrinsic.forEach(outputAllocation)
            outputAllocation!!.copyTo(output)
        }
    }

    private fun getYuvBytes(image: ImageProxy): ByteArray? {
        val format = image.format
        if (format != ImageFormat.YUV_420_888) return null

        val yPlane = image.planes[0].buffer
        val uPlane = image.planes[1].buffer
        val vPlane = image.planes[2].buffer
        val ySize = yPlane.remaining()
        val uSize = uPlane.remaining()
        val vSize = vPlane.remaining()
        val totalSize = ySize + uSize + vSize
        if (yuvByteArray.size != totalSize) yuvByteArray = ByteArray(totalSize)

        yPlane.get(yuvByteArray, 0, ySize)
        vPlane.get(yuvByteArray, ySize, vSize)
        uPlane.get(yuvByteArray, ySize + vSize, uSize)

        return yuvByteArray
    }
}