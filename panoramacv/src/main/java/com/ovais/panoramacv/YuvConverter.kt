package com.ovais.panoramacv

import android.graphics.*
import android.media.Image
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

object YuvConverter {
    fun toBitmap(image: Image): Bitmap {
        if (image.format == ImageFormat.JPEG) {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        val yPlane = image.planes[0]
        val uPlane = if (image.planes.size > 1) image.planes[1] else null
        val vPlane = if (image.planes.size > 2) image.planes[2] else null

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane?.buffer
        val vBuffer = vPlane?.buffer

        val width = image.width
        val height = image.height

        // If we don't have YUV planes, we can't manually convert
        if (uBuffer == null || vBuffer == null) {
            val bytes = ByteArray(yBuffer.remaining())
            yBuffer.get(bytes)
            // Return grayscale if only Y plane exists
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            for (i in pixels.indices) {
                val lum = bytes[i].toInt() and 0xFF
                pixels[i] = Color.rgb(lum, lum, lum)
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
        }

        val numPixels = (width * height * 1.5).toInt()
        val nv21 = ByteArray(numPixels)

        val yRowStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        var idy = 0
        for (row in 0 until height) {
            yBuffer.position(row * yRowStride)
            val rowWidth = Math.min(width, yBuffer.remaining())
            yBuffer.get(nv21, idy, rowWidth)
            idy += width
        }

        var iduv = width * height
        val uvWidth = width / 2
        val uvHeight = height / 2

        for (row in 0 until uvHeight) {
            for (col in 0 until uvWidth) {
                val pos = row * uvRowStride + col * uvPixelStride
                if (pos < vBuffer.remaining() && pos < uBuffer.remaining()) {
                    nv21[iduv++] = vBuffer.get(pos)
                    nv21[iduv++] = uBuffer.get(pos)
                }
            }
        }

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val imageBytes = out.toByteArray()
        
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}