package com.ovais.camera_360
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.get
import kotlin.math.abs

class Camera360Activity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private val capturedBitmaps = mutableListOf<Bitmap>()

    private lateinit var sensorManager: SensorManager
    private var sensorListener: SensorEventListener? = null

    private var lastYaw = 0f
    private val rotationThreshold = 25f
    private var lastCaptureTime = 0L
    private val captureDelay = 700L

    private var isCapturing by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Camera360Screen() }
        startCamera()
    }

    @Composable
    fun Camera360Screen() {
        val capturing = isCapturing
        Column(Modifier.fillMaxSize()) {
            AndroidView(
                factory = {
                    previewView = PreviewView(it)
                    previewView
                },
                modifier = Modifier.weight(1f)
            )
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { startCapturing() }, enabled = !capturing) {
                    Text("Start")
                }
                Button(onClick = { stopAndStitch() }, enabled = capturing) {
                    Text("Stop & Stitch")
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

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )

                setupSensor()
            } catch (e: Exception) {
                Log.e("Camera360", "Camera binding failed", e)
                Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupSensor() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!isCapturing) return

                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientationAngles = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                val yaw = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                val currentTime = System.currentTimeMillis()

                if (abs(yaw - lastYaw) >= rotationThreshold &&
                    currentTime - lastCaptureTime >= captureDelay
                ) {
                    lastYaw = yaw
                    lastCaptureTime = currentTime
                    captureFrame()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        rotationSensor?.let {
            sensorManager.registerListener(
                sensorListener,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    private fun startCapturing() {
        capturedBitmaps.clear()
        isCapturing = true
        lastYaw = 0f
        lastCaptureTime = 0L
        Toast.makeText(this, "Capturing started", Toast.LENGTH_SHORT).show()
    }

    private fun stopAndStitch() {
        isCapturing = false

        if (capturedBitmaps.isEmpty()) {
            Toast.makeText(this, "No frames captured!", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val stitchedBitmap = stitchBitmapsSimple(capturedBitmaps)

            stitchedBitmap?.let {
                PanoramaViewHolder.bitmap = it
                withContext(Dispatchers.Main) {
                    startActivity(
                        Intent(this@Camera360Activity, PanoramaViewActivity::class.java)
                    )
                }
            } ?: withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@Camera360Activity,
                    "Stitching failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun captureFrame() {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    bitmap?.let { capturedBitmaps.add(it) }
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("Camera360", "Capture error: ${exception.message}")
                }
            })
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            image.close()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorListener?.let { sensorManager.unregisterListener(it) }
    }

    /** Simple horizontal stitch (no OpenCV) **/
    private fun stitchBitmapsSimple(bitmaps: List<Bitmap>): Bitmap? {
        if (bitmaps.isEmpty()) return null
        if (bitmaps.size == 1) return bitmaps[0]

        val width = bitmaps.sumOf { it.width }
        val height = bitmaps.maxOf { it.height }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        var currentX = 0
        for (bmp in bitmaps) {
            canvas.drawBitmap(bmp, currentX.toFloat(), 0f, null)
            currentX += bmp.width
        }
        return result
    }
}