package com.ovais.panoramacv

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ovais.nativecore.NativeLib
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

@OptIn(ExperimentalGetImage::class)
class PanoramaCVActivity : ComponentActivity(), SensorEventListener {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    
    private val isRecording = mutableStateOf(false)
    private var lastYaw = 0f
    private val capturedFrames = mutableStateListOf<Bitmap>()
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
        
        // Capture frame every 3.5 degrees for dense overlap
        if (abs(yaw - lastYaw) > 3.5f || capturedFrames.isEmpty()) {
            currentFrame?.let {
                capturedFrames.add(it)
                lastYaw = yaw
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @Composable
    fun PanoramaCaptureScreen() {
        val context = LocalContext.current
        var isStitching by remember { mutableStateOf(false) }
        val previewView = remember { PreviewView(context) }
        val scrollState = rememberScrollState()

        LaunchedEffect(Unit) {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                imageProxy.image?.let { mediaImage ->
                    val bitmap = YuvConverter.toBitmap(mediaImage)
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                    // CRITICAL: Rotate the bitmap so it is upright. 
                    // This fixes the "going upward" issue.
                    currentFrame = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }
                imageProxy.close()
            }

            cameraProvider.bindToLifecycle(context as ComponentActivity, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
        }

        // Auto-scroll the filmstrip as frames are added
        LaunchedEffect(capturedFrames.size) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView({ previewView }, modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Status
                Surface(
                    modifier = Modifier.padding(top = 48.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (isRecording.value) "PANORAMA ACTIVE" else "READY",
                        color = if (isRecording.value) Color.Yellow else Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.sp
                    )
                }

                // Middle: The "Live Captured Filmstrip" (iPhone-like preview)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (capturedFrames.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray.copy(alpha = 0.5f))
                                .horizontalScroll(scrollState),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            capturedFrames.forEach { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxHeight().width(60.dp).padding(horizontal = 1.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        
                        // Center Guide Line
                        Box(modifier = Modifier.fillMaxHeight(0.8f).width(2.dp).background(Color.Yellow))
                    } else if (isRecording.value) {
                        Text("Move slowly to capture...", color = Color.White.copy(alpha = 0.7f))
                    }
                }

                // Bottom Controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isStitching) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 4.dp)
                    } else {
                        Button(
                            onClick = {
                                if (!isRecording.value) {
                                    capturedFrames.clear()
                                    isRecording.value = true
                                    lastYaw = 0f
                                } else {
                                    isRecording.value = false
                                    if (capturedFrames.size > 1) {
                                        isStitching = true
                                        cameraExecutor.execute {
                                            val result = NativeLib.stitchBitmaps(capturedFrames.toList())
                                            runOnUiThread {
                                                isStitching = false
                                                if (result != null) {
                                                    PanoramaResultHolder.bitmap = result
                                                    startActivity(Intent(context, PanoramaResultActivity::class.java))
                                                } else {
                                                    Toast.makeText(context, "Stitching failed. Try rotating more slowly.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording.value) Color.Red else Color.White
                            ),
                            contentPadding = PaddingValues(0.dp),
                            border = if (isRecording.value) null else BorderStroke(4.dp, Color.LightGray)
                        ) {
                            if (isRecording.value) {
                                Box(Modifier.size(30.dp).background(Color.White, RoundedCornerShape(4.dp)))
                            }
                        }
                    }
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