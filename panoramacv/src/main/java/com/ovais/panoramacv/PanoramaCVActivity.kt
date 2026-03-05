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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
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
    private var currentYaw = 0f
    private var lastCapturedYaw = 0f
    private val capturedFrames = mutableStateListOf<Bitmap>()
    private val thumbFrames = mutableStateListOf<Bitmap>() // For smooth UI
    private var hasNewFrameRequest = false

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

        currentYaw = Math.toDegrees(orientation[0].toDouble()).toFloat()
        
        // request a new frame every 6.0 degrees to reduce frame count
        if (capturedFrames.isEmpty() || abs(currentYaw - lastCapturedYaw) > 6.0f) {
            hasNewFrameRequest = true
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @Composable
    fun PanoramaCaptureScreen() {
        val context = LocalContext.current
        var isStitching by remember { mutableStateOf(false) }
        var showTips by remember { mutableStateOf(true) }
        var processingMessage by remember { mutableStateOf("Initializing...") }
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
                if (isRecording.value && hasNewFrameRequest) {
                    imageProxy.image?.let { mediaImage ->
                        val bitmap = YuvConverter.toBitmap(mediaImage)
                        val rotation = imageProxy.imageInfo.rotationDegrees
                        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        
                        // Small thumb for UI performance
                        val thumb = Bitmap.createScaledBitmap(rotatedBitmap, rotatedBitmap.width / 8, rotatedBitmap.height / 8, true)
                        
                        runOnUiThread {
                            capturedFrames.add(rotatedBitmap)
                            thumbFrames.add(thumb)
                            lastCapturedYaw = currentYaw
                            hasNewFrameRequest = false
                        }
                    }
                }
                imageProxy.close()
            }

            cameraProvider.bindToLifecycle(context as androidx.lifecycle.LifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
        }

        // Keep filmstrip scrolled to end
        LaunchedEffect(capturedFrames.size) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }

        if (showTips) {
            TipsDialog(onDismiss = { showTips = false })
        }

        if (isStitching) {
            ProcessingDialog(message = processingMessage)
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView({ previewView }, modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Status Pill
                Surface(
                    modifier = Modifier.padding(top = 56.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (isRecording.value) "PANORAMA • ${capturedFrames.size}" else "READY",
                        color = if (isRecording.value) Color.Red else Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bottom UI: Filmstrip + Shutter
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Smooth Filmstrip (placed above the button)
                    AnimatedVisibility(
                        visible = thumbFrames.isNotEmpty(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .padding(bottom = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .horizontalScroll(scrollState),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                thumbFrames.forEach { thumb ->
                                    Image(
                                        bitmap = thumb.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(35.dp) 
                                            .padding(horizontal = 0.5.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            // Indicator line
                            Box(modifier = Modifier.fillMaxHeight(0.6f).width(2.dp).background(Color.Yellow))
                        }
                    }

                    // Shutter Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                if (!isRecording.value) {
                                    capturedFrames.clear()
                                    thumbFrames.clear()
                                    isRecording.value = true
                                    lastCapturedYaw = currentYaw
                                } else {
                                    isRecording.value = false
                                    if (capturedFrames.size > 1) {
                                        isStitching = true
                                        cameraExecutor.execute {
                                            // SMART PRUNING: Limit to max 60 high-quality frames for speed
                                            val framesToStitch = if (capturedFrames.size > 60) {
                                                val step = capturedFrames.size / 60
                                                capturedFrames.filterIndexed { index, _ -> index % step == 0 }.take(60)
                                            } else {
                                                capturedFrames.toList()
                                            }

                                            val result = NativeLib.stitchBitmaps(framesToStitch, object : NativeLib.StitchProgressListener {
                                                override fun onProgressUpdate(message: String) {
                                                    runOnUiThread { processingMessage = message }
                                                }
                                            })
                                            
                                            runOnUiThread {
                                                isStitching = false
                                                if (result != null) {
                                                    PanoramaResultHolder.bitmap = result
                                                    startActivity(Intent(context, PanoramaResultActivity::class.java))
                                                } else {
                                                    Toast.makeText(context, "Stitching failed.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording.value) Color.Red else Color.White
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            if (isRecording.value) {
                                Box(Modifier.size(28.dp).background(Color.White, RoundedCornerShape(4.dp)))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun TipsDialog(onDismiss: () -> Unit) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💡",
                        fontSize = 48.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Tips for Perfect Panoramas",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    val tips = listOf(
                        "📱 Hold your phone vertically (Portrait mode).",
                        "🔄 Rotate your entire body slowly and steadily.",
                        "🌅 Keep the camera level with the horizon.",
                        "🚶 Move only once around—don't overlap circles.",
                        "✨ Avoid moving objects like people or cars."
                    )
                    
                    tips.forEach { tip ->
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            textAlign = TextAlign.Start
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("GOT IT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    @Composable
    fun ProcessingDialog(message: String) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        capturedFrames.clear()
        thumbFrames.clear()
        isRecording.value = false
        lastCapturedYaw = 0f

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