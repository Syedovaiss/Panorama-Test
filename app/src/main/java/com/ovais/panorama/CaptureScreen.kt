package com.ovais.panorama

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
private const val YAW_THRESHOLD = 15f // degrees

@androidx.annotation.RequiresPermission(android.Manifest.permission.CAMERA)
@Composable
fun CaptureScreen(navToViewer: (Uri) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Persist TextureView across recompositions
    val textureView = remember { TextureView(context) }

    val panoramaEngine = remember { PanoramaEngine() }
    val repository = remember { ImageRepository(context) }

    // Latest frame data
    var latestFrame by remember { mutableStateOf<ByteArray?>(null) }
    var latestFrameWidth by remember { mutableIntStateOf(0) }
    var latestFrameHeight by remember { mutableIntStateOf(0) }
    var lastAddedYaw by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { textureView },
            modifier = Modifier.fillMaxSize()
        )

        LaunchedEffect(Unit) {
            // Get camera sensor orientation
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.first() // back camera
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

            // Camera controller callback
            val cameraController = CameraController(context, textureView) { byteArray, width, height ->
                latestFrame = byteArray
                latestFrameWidth = width
                latestFrameHeight = height
            }

            // Sensor controller callback
            val sensorController = SensorFusionController(context) { currentYaw ->
                val delta = abs(currentYaw - lastAddedYaw)
                if (delta >= YAW_THRESHOLD && latestFrame != null) {
                    val bytes = latestFrame!!
                    val width = latestFrameWidth
                    val height = latestFrameHeight

                    // Launch bitmap processing in background
                    scope.launch(Dispatchers.Default) {
                        // Convert YUV → Bitmap
                        val bitmap = YuvConverter.toBitmap(bytes, width, height)

                        // Rotate to match sensor
                        val rotatedBitmap = rotateBitmap(bitmap, sensorOrientation.toFloat())

                        // Cylindrical projection
                        val projected = CylindricalProjector.project(rotatedBitmap, 700f)

                        // Add to panorama
                        panoramaEngine.addFrame(projected)

                        // Update last added yaw
                        lastAddedYaw = currentYaw
                    }

                    // Clear latestFrame to avoid duplicates
                    latestFrame = null
                }
            }

            cameraController.startCamera()
            sensorController.start()
        }

        // Finish button
        Button(
            onClick = {
                scope.launch {
                    val pano = panoramaEngine.buildPanorama()
                    val uri = repository.saveAndReturnUri(pano)
                    navToViewer(uri)
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text("Finish & Save")
        }
    }
}

/** Rotate bitmap by degrees */
fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}