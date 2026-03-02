package com.ovais.panorama

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.createBitmap

object StitchEngine {

    fun stitch(bitmaps: List<Bitmap>): Bitmap {
        val totalWidth = bitmaps.sumOf { it.width }
        val height = bitmaps.first().height

        val result = createBitmap(totalWidth, height)

        val canvas = Canvas(result)
        var currentX = 0f

        bitmaps.forEach {
            canvas.drawBitmap(it, currentX, 0f, null)
            currentX += it.width
        }

        return result
    }
}