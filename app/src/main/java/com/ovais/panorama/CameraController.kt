package com.ovais.panorama

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresPermission
class CameraController(
    private val context: Context,
    private val previewView: TextureView,
    private val onFrameAvailable: (ByteArray, Int, Int) -> Unit
) {

    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader

    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)

    @RequiresPermission(Manifest.permission.CAMERA)
    fun startCamera() {
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            facing == CameraCharacteristics.LENS_FACING_BACK
        } ?: throw RuntimeException("No back camera found")
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val sensorArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val width = sensorArray?.width() ?: 1920
        val height = sensorArray?.height() ?: 1080

        // 🔹 Max images 5 for safety
        imageReader = ImageReader.newInstance(
            width,
            height,
            ImageFormat.YUV_420_888,
            3
        )

        // 🔹 Production-safe listener
        imageReader.setOnImageAvailableListener({ reader ->

            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

            try {
                val data = YuvConverter.toByteArray(image)
                onFrameAvailable(data, image.width, image.height)
            } finally {
                image.close()
            }

        }, cameraHandler)

        cameraManager.openCamera(cameraId,
            object : CameraDevice.StateCallback() {

                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    cameraDevice.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    cameraDevice.close()
                }

            }, cameraHandler
        )
    }

    private fun createSession() {

        val texture = previewView.surfaceTexture ?: return
        texture.setDefaultBufferSize(1920, 1080)

        val previewSurface = Surface(texture)
        val captureSurface = imageReader.surface

        cameraDevice.createCaptureSession(
            listOf(previewSurface, captureSurface),
            object : CameraCaptureSession.StateCallback() {

                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session

                    val request = cameraDevice.createCaptureRequest(
                        CameraDevice.TEMPLATE_PREVIEW
                    ).apply {
                        addTarget(previewSurface)
                        addTarget(captureSurface)
                        set(
                            CaptureRequest.CONTROL_MODE,
                            CameraMetadata.CONTROL_MODE_AUTO
                        )
                        set(
                            CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                        )
                    }

                    captureSession.setRepeatingRequest(
                        request.build(),
                        null,
                        cameraHandler
                    )
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            },
            cameraHandler
        )
    }

    fun stop() {
        captureSession.close()
        cameraDevice.close()
        imageReader.close()
        cameraThread.quitSafely()
    }
}