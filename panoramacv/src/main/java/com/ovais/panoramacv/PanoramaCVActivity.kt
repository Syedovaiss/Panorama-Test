package com.ovais.panoramacv

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.ovais.nativecore.NativeLib
import java.io.OutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

@OptIn(ExperimentalGetImage::class)
class PanoramaCVActivity : ComponentActivity(), SensorEventListener {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    
    private var isRecording = mutableStateOf(false)
    private var lastYaw = 0f
    private val frames = mutableStateListOf<Bitmap>()
    private var currentFrame: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        setContent {
            PanoramaCaptureScreen()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isRecording.value || event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)

        val yaw = Math.toDegrees(orientation[0].toDouble()).toFloat()
        
        // Capture frame every 5 degrees of rotation for optimal overlap
        if (abs(yaw - lastYaw) > 5.0f || frames.isEmpty()) {
            currentFrame?.let {
                frames.add(it)
                lastYaw = yaw
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val filename = "PANO_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Panorama")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            val outputStream: OutputStream? = contentResolver.openOutputStream(it)
            outputStream?.use { stream -> bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(it, contentValues, null, null)
            }
        }
    }

    @Composable
    fun PanoramaCaptureScreen() {
        val context = LocalContext.current
        var isStitching by remember { mutableStateOf(false) }
        var stitchedBitmap by remember { mutableStateOf<Bitmap?>(null) }
        val previewView = remember { PreviewView(context) }

        LaunchedEffect(Unit) {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                imageProxy.image?.let { mediaImage ->
                    currentFrame = YuvConverter.toBitmap(mediaImage)
                }
                imageProxy.close()
            }

            cameraProvider.bindToLifecycle(context as ComponentActivity, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView({ previewView }, modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(color = Color.Black.copy(alpha = 0.6f), shape = CircleShape) {
                    Text(
                        text = if (isRecording.value) "Capturing: ${frames.size} frames" else "Ready",
                        color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (isStitching) CircularProgressIndicator(color = Color.White)
                    else stitchedBitmap?.let {
                        Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth().height(200.dp), contentScale = ContentScale.Fit)
                    }
                }

                Button(
                    onClick = {
                        if (!isRecording.value) {
                            frames.clear()
                            stitchedBitmap = null
                            isRecording.value = true
                            lastYaw = 0f
                        } else {
                            isRecording.value = false
                            if (frames.size > 1) {
                                isStitching = true
                                cameraExecutor.execute {
                                    val result = NativeLib.stitchBitmaps(frames.toList())
                                    runOnUiThread {
                                        stitchedBitmap = result
                                        isStitching = false
                                        if (result != null) {
                                            saveBitmapToGallery(result)
                                            Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.height(64.dp).fillMaxWidth(0.7f),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRecording.value) Color.White else Color.Red)
                ) {
                    Text(if (isRecording.value) "STOP & STITCH" else "START CAPTURE", color = if (isRecording.value) Color.Black else Color.White)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}