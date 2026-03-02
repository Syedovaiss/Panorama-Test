package com.ovais.camera_360

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ovais.camera_360.ui.theme.PanoramaTheme
import org.opencv.android.OpenCVLoader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Camera360Activity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var panoramaEngine: PanoramaEngine
    private lateinit var sensorController: SensorController

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OpenCVLoader.initDebug()

        cameraExecutor = Executors.newSingleThreadExecutor()
        panoramaEngine = PanoramaEngine(this)
        
        // Fix: Properly initialize SensorController with the required callback
        sensorController = SensorController(this) { yaw ->
            Log.d("Camera360", "Angle changed: $yaw")
            // Optionally trigger a frame capture here via panoramaEngine
        }

        setContent {
            PanoramaTheme {
                var isCapturing by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = {
                            previewView = PreviewView(it).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    Button(
                        onClick = {
                            isCapturing = !isCapturing
                            if (isCapturing) {
                                panoramaEngine.start()
                                sensorController.start()
                            } else {
                                panoramaEngine.stop()
                                sensorController.stop()
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        Text(if (isCapturing) "Stop" else "Start")
                    }
                }

                LaunchedEffect(Unit) {
                    startCamera()
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

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(cameraExecutor, panoramaEngine::processFrame) }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("Camera360", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onResume() {
        super.onResume()
        if (::sensorController.isInitialized) {
            sensorController.start()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::sensorController.isInitialized) {
            sensorController.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
