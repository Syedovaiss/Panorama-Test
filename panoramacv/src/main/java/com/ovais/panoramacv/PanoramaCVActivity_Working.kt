//package com.ovais.panoramacv
//
//import android.Manifest
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.graphics.Bitmap
//import android.graphics.Matrix
//import android.hardware.Sensor
//import android.hardware.SensorEvent
//import android.hardware.SensorEventListener
//import android.hardware.SensorManager
//import android.os.Bundle
//import android.util.Size
//import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.compose.setContent
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.annotation.OptIn
//import androidx.camera.core.*
//import androidx.camera.core.resolutionselector.AspectRatioStrategy
//import androidx.camera.core.resolutionselector.ResolutionSelector
//import androidx.camera.core.resolutionselector.ResolutionStrategy
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.camera.view.PreviewView
//import androidx.compose.animation.*
//import androidx.compose.animation.core.animateFloatAsState
//import androidx.compose.foundation.*
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.asImageBitmap
//import androidx.compose.ui.graphics.graphicsLayer
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.compose.ui.viewinterop.AndroidView
//import androidx.compose.ui.window.Dialog
//import androidx.core.content.ContextCompat
//import androidx.core.graphics.scale
//import androidx.lifecycle.LifecycleOwner
//import com.ovais.nativecore.NativeLib
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors
//import kotlin.math.abs
//import kotlin.math.roundToInt
//
//@OptIn(ExperimentalGetImage::class)
//class PanoramaCVActivity_Working : ComponentActivity(), SensorEventListener {
//
//    private lateinit var cameraExecutor: ExecutorService
//    private lateinit var sensorManager: SensorManager
//    private var rotationSensor: Sensor? = null
//
//    private val isRecording = mutableStateOf(false)
//    private var currentYaw = 0f
//    private var lastCapturedYaw = 0f
//    private val capturedFrames = mutableStateListOf<Bitmap>()
//    private val thumbFrames = mutableStateListOf<Bitmap>()
//    private var hasNewFrameRequest = false
//
//    private val maxFrames = 60
//    private val targetFramesToProcess = 21
//    private val captureDegree = 360 / maxFrames
//
//    private val res720p = Size(1280, 720)
//
//    private val pitchState = mutableFloatStateOf(0f)
//    private val rollState = mutableFloatStateOf(0f)
//
//    // ✅ FIX: Correct angular difference across ±180°
//    private fun angularDelta(a: Float, b: Float): Float {
//        var diff = a - b
//        while (diff > 180f) diff -= 360f
//        while (diff < -180f) diff += 360f
//        return abs(diff)
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        cameraExecutor = Executors.newSingleThreadExecutor()
//        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
//        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
//
//        setContent {
//            MaterialTheme(colorScheme = darkColorScheme()) {
//                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
//                    CameraPermissionWrapper {
//                        PanoramaCaptureScreen()
//                    }
//                }
//            }
//        }
//    }
//
//    override fun onSensorChanged(event: SensorEvent) {
//        if (!isRecording.value || event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
//
//        val rotationMatrix = FloatArray(9)
//        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
//        val orientation = FloatArray(3)
//        SensorManager.getOrientation(rotationMatrix, orientation)
//
//        currentYaw = Math.toDegrees(orientation[0].toDouble()).toFloat()
//
//        // ✅ FIXED: wrap‑safe comparison
//        if (
//            capturedFrames.isEmpty() ||
//            angularDelta(currentYaw, lastCapturedYaw) > captureDegree
//        ) {
//            hasNewFrameRequest = true
//        }
//    }
//
//    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
//
//    @Composable
//    fun CameraPermissionWrapper(content: @Composable () -> Unit) {
//        val context = LocalContext.current
//        var hasPermission by remember {
//            mutableStateOf(
//                ContextCompat.checkSelfPermission(
//                    context,
//                    Manifest.permission.CAMERA
//                ) == PackageManager.PERMISSION_GRANTED
//            )
//        }
//
//        val launcher = rememberLauncherForActivityResult(
//            contract = ActivityResultContracts.RequestPermission()
//        ) { granted ->
//            hasPermission = granted
//            if (!granted) {
//                Toast.makeText(
//                    context,
//                    "Camera permission is required",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//
//        LaunchedEffect(Unit) {
//            if (!hasPermission) {
//                launcher.launch(Manifest.permission.CAMERA)
//            }
//        }
//
//        if (hasPermission) content() else {
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
//                    Text("Grant Camera Permission")
//                }
//            }
//        }
//    }
//
//    @Composable
//    fun PanoramaCaptureScreen() {
//        val context = LocalContext.current
//        val previewView = remember { PreviewView(context) }
//        val scrollState = rememberScrollState()
//        var isStitching by remember { mutableStateOf(false) }
//        var message by remember { mutableStateOf("Processing…") }
//
//        // ✅ FIX: Non‑blocking CameraX setup + safe rebind
//        LaunchedEffect(Unit) {
//            val cameraProviderFuture =
//                ProcessCameraProvider.getInstance(context)
//
//            cameraProviderFuture.addListener({
//                val cameraProvider = cameraProviderFuture.get()
//                cameraProvider.unbindAll()
//
//                val resolutionSelector = ResolutionSelector.Builder()
//                    .setAspectRatioStrategy(
//                        AspectRatioStrategy(
//                            AspectRatio.RATIO_16_9,
//                            AspectRatioStrategy.FALLBACK_RULE_AUTO
//                        )
//                    )
//                    .setResolutionStrategy(
//                        ResolutionStrategy(
//                            res720p,
//                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER
//                        )
//                    )
//                    .build()
//
//                val preview = Preview.Builder().build().also {
//                    it.surfaceProvider = previewView.surfaceProvider
//                }
//
//                val analysis = ImageAnalysis.Builder()
//                    .setBackpressureStrategy(
//                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
//                    )
//                    .setOutputImageFormat(
//                        ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
//                    )
//                    .setResolutionSelector(resolutionSelector)
//                    .build()
//
//                analysis.setAnalyzer(cameraExecutor) { proxy ->
//                    if (isRecording.value && hasNewFrameRequest) {
//                        proxy.image?.let { img ->
//                            val bmp = YuvConverter.toBitmap(img)
//                            val m = Matrix().apply {
//                                postRotate(
//                                    proxy.imageInfo.rotationDegrees.toFloat()
//                                )
//                            }
//
//                            val rotated = Bitmap.createBitmap(
//                                bmp, 0, 0,
//                                bmp.width, bmp.height, m, true
//                            )
//
//                            val thumb = rotated.scale(
//                                rotated.width / 8,
//                                rotated.height / 8
//                            )
//
//                            runOnUiThread {
//                                capturedFrames += rotated
//                                thumbFrames += thumb
//                                lastCapturedYaw = currentYaw
//                                hasNewFrameRequest = false
//                            }
//                        }
//                    }
//                    proxy.close()
//                }
//
//                cameraProvider.bindToLifecycle(
//                    context as LifecycleOwner,
//                    CameraSelector.DEFAULT_BACK_CAMERA,
//                    preview,
//                    analysis
//                )
//            }, ContextCompat.getMainExecutor(context))
//        }
//
//        Box(Modifier.fillMaxSize()) {
//            AndroidView({ previewView }, Modifier.fillMaxSize())
//
//            Column(
//                modifier = Modifier.fillMaxSize(),
//                verticalArrangement = Arrangement.SpaceBetween,
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//
//                Surface(
//                    modifier = Modifier.padding(top = 56.dp),
//                    shape = CircleShape,
//                    color = Color.Black.copy(alpha = 0.6f)
//                ) {
//                    Text(
//                        text = if (isRecording.value)
//                            "PANORAMA • ${capturedFrames.size}"
//                        else "READY",
//                        modifier = Modifier.padding(12.dp),
//                        color = if (isRecording.value)
//                            Color.Red else Color.White
//                    )
//                }
//
//                Column(
//                    modifier = Modifier
//                        .padding(bottom = 48.dp)
//                        .fillMaxWidth(),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth(0.9f)
//                            .height(60.dp)
//                            .horizontalScroll(scrollState)
//                    ) {
//                        thumbFrames.forEach {
//                            Image(
//                                bitmap = it.asImageBitmap(),
//                                contentDescription = null,
//                                modifier = Modifier
//                                    .width(35.dp)
//                                    .fillMaxHeight(),
//                                contentScale = ContentScale.Crop
//                            )
//                        }
//                    }
//
//                    Spacer(Modifier.height(16.dp))
//
//                    Button(
//                        onClick = {
//                            if (!isRecording.value) {
//                                capturedFrames.clear()
//                                thumbFrames.clear()
//                                lastCapturedYaw = currentYaw // ✅ FIX
//                                isRecording.value = true
//                            } else {
//                                isRecording.value = false
//                                if (capturedFrames.size > 1) {
//                                    isStitching = true
//                                    cameraExecutor.execute {
//                                        val result =
//                                            NativeLib.stitchBitmaps(
//                                                capturedFrames.take(
//                                                    targetFramesToProcess
//                                                )
//                                            ) {
//                                                runOnUiThread {
//                                                    message = it
//                                                }
//                                            }
//
//                                        runOnUiThread {
//                                            isStitching = false
//                                            if (result != null) {
//                                                PanoramaResultHolder.bitmap =
//                                                    result
//                                                startActivity(
//                                                    Intent(
//                                                        context,
//                                                        PanoramaResultActivity::class.java
//                                                    )
//                                                )
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        },
//                        modifier = Modifier.size(72.dp),
//                        shape = CircleShape,
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = if (isRecording.value)
//                                Color.Red else Color.White
//                        )
//                    ) {}
//                }
//            }
//        }
//
//        if (isRecording.value) {
//            HorizonGuide(pitchState.floatValue, rollState.floatValue)
//            CenterLockDots(pitchState.floatValue, rollState.floatValue)
//        }
//
//        if (isStitching) {
//            Dialog(onDismissRequest = {}) {
//                Surface(shape = RoundedCornerShape(16.dp)) {
//                    Column(
//                        Modifier.padding(24.dp),
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        CircularProgressIndicator()
//                        Spacer(Modifier.height(16.dp))
//                        Text(message)
//                    }
//                }
//            }
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        capturedFrames.clear()
//        thumbFrames.clear()
//        isRecording.value = false
//        lastCapturedYaw = currentYaw // ✅ FIX
//        rotationSensor?.let {
//            sensorManager.registerListener(
//                this, it, SensorManager.SENSOR_DELAY_UI
//            )
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        sensorManager.unregisterListener(this)
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        cameraExecutor.shutdown()
//    }
//}
//
//@Composable
//fun HorizonGuide(pitch: Float, roll: Float) {
//    val snap = if (abs(roll) < 2.5f) 0f else -roll
//    val animatedRoll by animateFloatAsState(snap, label = "snap")
//
//    Box(Modifier.fillMaxSize(), Alignment.Center) {
//        Box(
//            Modifier
//                .offset(y = (-pitch * 6).coerceIn(-80f, 80f).dp)
//                .width(180.dp)
//                .height(3.dp)
//                .graphicsLayer { rotationZ = animatedRoll }
//                .background(
//                    if (abs(pitch) < 2 && abs(roll) < 2) Color.Green else Color.Red,
//                    RoundedCornerShape(2.dp)
//                )
//        )
//    }
//}
//
//// ---------- ✅ CENTER DOTS ----------
//@Composable
//fun CenterLockDots(pitch: Float, roll: Float) {
//    val x = (roll * 4).coerceIn(-40f, 40f)
//    val y = (-pitch * 4).coerceIn(-40f, 40f)
//
//    Box(Modifier.fillMaxSize(), Alignment.Center) {
//        Box(Modifier.size(8.dp).background(Color.White, CircleShape))
//        Box(
//            Modifier
//                .offset(x.dp, y.dp)
//                .size(8.dp)
//                .background(
//                    if (abs(pitch) < 2 && abs(roll) < 2)
//                        Color.Green else Color.White,
//                    CircleShape
//                )
//        )
//    }
//}