package com.ovais.panorama_strip

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class PanoramaStripActivity : ComponentActivity(), SensorEventListener {

    private lateinit var cameraExecutor: ExecutorService
    private val isCapturing = mutableStateOf(false)
    private val panoramaBitmap = mutableStateOf<Bitmap?>(null)
    private lateinit var processor: PanoramaProcessor

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private var lastYaw: Float = 0f
    private var isFirstYaw = true
    
    // Pixels per degree of rotation. 
    // This value controls the "stretch". If it's too high, the image is stretched. 
    // If too low, it's squashed. ~15-20 is typical for mobile cameras.
    private val pixelsPerDegree = 18f 

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        // We use a high height for quality. The processor will handle scaling.
        processor = PanoramaProcessor(1080)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            MaterialTheme {
                PanoramaCaptureScreen()
            }
        }
    }

    @Composable
    fun PanoramaCaptureScreen() {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            CameraPreview(
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Live Preview Strip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .height(130.dp)
                            .background(Color.DarkGray.copy(alpha = 0.3f))
                    )

                    panoramaBitmap.value?.let { it ->
                        val scrollState = rememberScrollState()
                        LaunchedEffect(it.width) {
                            scrollState.animateScrollTo(it.width)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .height(130.dp)
                                .horizontalScroll(scrollState)
                        ) {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxHeight(),
                                contentScale = ContentScale.FillHeight
                            )
                        }
                    }

                    // Guide Line
                    Box(modifier = Modifier.fillMaxHeight(0.75f).width(2.dp).background(Color.Yellow))
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Shutter
                Button(
                    onClick = { 
                        if (isCapturing.value) {
                            isCapturing.value = false 
                            saveAndShowPanorama()
                        } else {
                            processor.reset()
                            panoramaBitmap.value = null
                            isCapturing.value = true
                            isFirstYaw = true
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCapturing.value) Color.Red else Color.White
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    if (isCapturing.value) {
                        Box(Modifier.size(30.dp).background(Color.White, shape = RoundedCornerShape(4.dp)))
                    }
                }
            }
        }
    }

    @Composable
    fun CameraPreview(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val previewView = remember { PreviewView(context) }

        AndroidView(
            factory = { previewView },
            modifier = modifier
        ) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also { analyzer ->
                        analyzer.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                            val bitmap = imageProxy.toBitmap()
                            if (bitmap != null) {
                                val rotation = imageProxy.imageInfo.rotationDegrees
                                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                currentFrame = Bitmap.createBitmap(
                                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                                )
                            }
                            imageProxy.close()
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
                    Log.e("PanoramaStrip", "Binding failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    private var currentFrame: Bitmap? = null

    override fun onSensorChanged(event: SensorEvent) {
        if (!isCapturing.value || event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)
        
        val yaw = Math.toDegrees(orientation[0].toDouble()).toFloat()

        if (isFirstYaw) {
            lastYaw = yaw
            isFirstYaw = false
            return
        }

        val yawDiff = yaw - lastYaw
        // We only care about movement in one direction for now (e.g. left to right)
        // Or we can handle both. Let's handle just absolute movement for simplicity.
        val absDiff = abs(yawDiff)
        
        // Threshold to avoid noise
        if (absDiff > 0.05f) {
            val pixelsToAdd = (absDiff * pixelsPerDegree).toInt()
            if (pixelsToAdd >= 1) {
                currentFrame?.let { frame ->
                    val updated = processor.processFrame(frame, pixelsToAdd)
                    if (updated != null) {
                        panoramaBitmap.value = updated
                    } else if (processor.isFull()) {
                        isCapturing.value = false
                        saveAndShowPanorama()
                    }
                }
                lastYaw = yaw
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveAndShowPanorama() {
        val bitmap = panoramaBitmap.value ?: return
        PanoramaResultHolder.bitmap = bitmap
        
        lifecycleScope.launch(Dispatchers.IO) {
            val success = saveBitmapToGallery(this@PanoramaStripActivity, bitmap, "Panorama_${System.currentTimeMillis()}")
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@PanoramaStripActivity, "Panorama saved!", Toast.LENGTH_SHORT).show()
                }
                startActivity(Intent(this@PanoramaStripActivity, PanoramaViewActivity::class.java))
            }
        }
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Panorama")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        return try {
            uri?.let {
                resolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
                true
            } ?: false
        } catch (e: Exception) {
            false
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
