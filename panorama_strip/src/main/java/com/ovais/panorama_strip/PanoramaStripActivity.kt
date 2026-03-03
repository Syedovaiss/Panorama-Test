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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var isFirstYaw = true

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

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
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

                    panoramaBitmap.value?.let { bitmap ->
                        val scrollState = rememberScrollState()

                        LaunchedEffect(bitmap.width) {
                            if (!processor.isFull()) {
                                scrollState.animateScrollTo(bitmap.width)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .height(130.dp)
                                .horizontalScroll(scrollState)
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxHeight(),
                                contentScale = ContentScale.FillHeight
                            )
                        }
                    }

                    // Guide Line
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.75f)
                            .width(2.dp)
                            .background(Color.Yellow)
                    )
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
                        Box(
                            Modifier
                                .size(30.dp)
                                .background(Color.White, shape = RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun CameraPreview(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
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
                        analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                            val bitmap = imageProxy.toBitmap()
                            bitmap?.let {
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

    private var lastYaw = 0f
    private var lastPitch = 0f
    private var isFirstReading = true
    private var smoothedYaw = 0f
    private var smoothedPitch = 0f
    private val alpha = 0.08f   // low-pass smoothing factor

    override fun onSensorChanged(event: SensorEvent) {
        if (!isCapturing.value || event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)

        var yaw = Math.toDegrees(orientation[0].toDouble()).toFloat()
        val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()

        // Wrap yaw
        if (yaw - lastYaw > 180f) yaw -= 360f
        if (yaw - lastYaw < -180f) yaw += 360f

        // Low-pass smoothing
        smoothedYaw = alpha * yaw + (1 - alpha) * smoothedYaw
        smoothedPitch = alpha * pitch + (1 - alpha) * smoothedPitch

        if (isFirstReading) {
            smoothedYaw = yaw
            smoothedPitch = pitch
            isFirstReading = false
            lastYaw = yaw
            lastPitch = pitch
            return
        }

        val yawDiff = smoothedYaw - lastYaw
        lastYaw = smoothedYaw
        lastPitch = smoothedPitch

        // Only move when enough rotation
        if (abs(yawDiff) > 0.2f) {
            currentFrame?.let { frame ->
                // Map pitch to vertical offset (-5px to +5px)
                val maxOffset = 5
                val verticalOffset = ((smoothedPitch / 10f) * maxOffset).toInt()

                val updated = processor.processFrame(frame, verticalOffset)
                if (updated != null) panoramaBitmap.value = updated

                if (processor.isFull()) {
                    isCapturing.value = false
                    saveAndShowPanorama()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveAndShowPanorama() {
        val bitmap = panoramaBitmap.value ?: return
        PanoramaResultHolder.bitmap = bitmap

        lifecycleScope.launch(Dispatchers.IO) {
            val success = saveBitmapToGallery(
                this@PanoramaStripActivity,
                bitmap,
                "Panorama_${System.currentTimeMillis()}"
            )
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(
                        this@PanoramaStripActivity,
                        "Panorama saved!",
                        Toast.LENGTH_SHORT
                    ).show()
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
            Log.e(PanoramaStripActivity::class.java.simpleName, e.stackTraceToString())
            false
        }
    }

    override fun onResume() {
        super.onResume()
        rotationSensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
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
