package com.ovais.panorama

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import androidx.core.graphics.createBitmap
import kotlin.math.tan

object CylindricalProjector {

    fun project(bitmap: Bitmap, focalLength: Float): Bitmap {

        val width = bitmap.width
        val height = bitmap.height

        val result = createBitmap(width, height)
        val canvas = Canvas(result)

        val centerX = width / 2f

        for (x in 0 until width) {
            val theta = (x - centerX) / focalLength
            val sourceX = (focalLength * tan(theta) + centerX).toInt()

            if (sourceX in 0 until width) {
                val srcRect = Rect(sourceX, 0, sourceX + 1, height)
                val dstRect = Rect(x, 0, x + 1, height)
                canvas.drawBitmap(bitmap, srcRect, dstRect, null)
            }
        }

        return result
    }
}