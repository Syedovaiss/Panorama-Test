package com.ovais.panoramacv

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import com.ovais.nativecore.NativeLib
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
class PanoramaCVActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            PanoramaCaptureScreen()
        }
    }

    @Composable
    fun PanoramaCaptureScreen() {
        val context = LocalContext.current
        var isRecording by remember { mutableStateOf(false) }
        val frames = remember { mutableStateListOf<Bitmap>() }
        var stitchedBitmap by remember { mutableStateOf<Bitmap?>(null) }
        val previewView = remember { PreviewView(context) }

        // CameraX setup
        LaunchedEffect(Unit) {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                context as ComponentActivity,
                cameraSelector,
                preview,
                imageCapture
            )

            val sensor = RotationSensor(context) {
                if (isRecording) {
                    imageCapture.takePicture(ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                image.image?.let { mediaImage ->
                                    val bitmap = YuvConverter.toBitmap(mediaImage)
                                    frames.add(bitmap)
                                    image.close()
                                } ?: run {
                                    image.close()
                                }
                            }
                        })
                }
            }
            sensor.start()
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            content = { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    AndroidView({ previewView }, modifier = Modifier.weight(1f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = { isRecording = true }) { Text("Start") }
                        Button(onClick = { isRecording = false }) { Text("Stop") }
                        Button(onClick = {
                            if (frames.isNotEmpty()) {
                                stitchedBitmap = NativeLib.stitchBitmaps(frames)
                                Toast.makeText(context, "Stitching Done!", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("Stitch") }
                    }

                    stitchedBitmap?.let {
                        Image(bitmap = it.asImageBitmap(), contentDescription = "Stitched Panorama",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}