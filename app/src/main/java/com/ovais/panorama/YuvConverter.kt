package com.ovais.panorama

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import java.io.ByteArrayOutputStream

object YuvConverter {

    /**
     * Converts an Image (YUV_420_888) to a NV21 byte array.
     * Safe for async processing.
     */
    fun toByteArray(image: Image): ByteArray {
        val width = image.width
        val height = image.height

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copy Y
        yBuffer.get(nv21, 0, ySize)

        // Copy VU (interleaved)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }
    fun toNV21(image: Image): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 2
        val nv21 = ByteArray(ySize + uvSize)

        val yBuffer = image.planes[0].buffer
        val yRowStride = image.planes[0].rowStride
        val yPixelStride = image.planes[0].pixelStride
        var offset = 0
        for (row in 0 until height) {
            for (col in 0 until width) {
                nv21[offset++] = yBuffer.get(row * yRowStride + col * yPixelStride)
            }
        }

        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val uRowStride = image.planes[1].rowStride
        val vRowStride = image.planes[2].rowStride
        val uPixelStride = image.planes[1].pixelStride
        val vPixelStride = image.planes[2].pixelStride

        var uvOffset = ySize
        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val uIndex = row * uRowStride + col * uPixelStride
                val vIndex = row * vRowStride + col * vPixelStride
                nv21[uvOffset++] = vBuffer.get(vIndex) // V first for NV21
                nv21[uvOffset++] = uBuffer.get(uIndex) // U next
            }
        }

        return nv21
    }

    /** Convert NV21 ByteArray -> Bitmap */
    fun toBitmap(data: ByteArray, width: Int, height: Int): Bitmap {
        val yuvImage = YuvImage(data, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val bytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /** Convenience: Image -> Bitmap */
    fun toBitmap(image: Image): Bitmap {
        val nv21 = toNV21(image)
        return toBitmap(nv21, image.width, image.height)
    }
}