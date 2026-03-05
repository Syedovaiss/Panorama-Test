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
import android.view.WindowManager
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
class PanoramaCVActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var rotationSensor: RotationSensor

    private val isRecording = mutableStateOf(false)
    private val capturedFrames = mutableStateListOf<Bitmap>()
    private var currentFrame: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Rotation sensor setup: frames only captured if recording
        rotationSensor = RotationSensor(this) {
            if (isRecording.value) {
                currentFrame?.let { bitmap ->
                    capturedFrames.add(bitmap)
                }
            }
        }

        setContent {
            PanoramaCaptureScreen()
        }
    }

    override fun onResume() {
        super.onResume()
        rotationSensor.start()
    }

    override fun onPause() {
        super.onPause()
        rotationSensor.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    @Composable
    fun PanoramaCaptureScreen() {
        val context = LocalContext.current
        var isStitching by remember { mutableStateOf(false) }
        val previewView = remember { PreviewView(context) }
        val scrollState = rememberScrollState()

        // CameraX setup
        LaunchedEffect(Unit) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll() // remove old bindings

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                imageProxy.image?.let { mediaImage ->
                    val bitmap = YuvConverter.toBitmap(mediaImage)
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                    currentFrame = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }
                imageProxy.close()
            }

            try {
                cameraProvider.bindToLifecycle(
                    context as ComponentActivity,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Camera binding failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Auto-scroll filmstrip
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
                // Top status
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

                // Filmstrip
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
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(60.dp)
                                        .padding(horizontal = 1.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        // Center guide
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.8f)
                                .width(2.dp)
                                .background(Color.Yellow)
                        )
                    } else if (isRecording.value) {
                        Text("Move slowly to capture...", color = Color.White.copy(alpha = 0.7f))
                    }
                }

                // Bottom controls
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
                                    rotationSensor.resetYaw()
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
                                                    Toast.makeText(
                                                        context,
                                                        "Stitching failed. Try rotating more slowly.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
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
                            contentPadding = PaddingValues(0.dp)
                        ) {}
                    }
                }
            }
        }
    }
}