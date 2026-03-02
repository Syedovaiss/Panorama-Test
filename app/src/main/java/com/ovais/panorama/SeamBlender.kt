package com.ovais.panorama

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.createBitmap

object SeamBlender {

    fun blend(left: Bitmap, right: Bitmap, overlap: Int): Bitmap {

        val result = createBitmap(left.width + right.width - overlap, left.height)

        val canvas = Canvas(result)
        canvas.drawBitmap(left, 0f, 0f, null)

        val paint = Paint()

        for (i in 0 until overlap) {
            val alpha = i / overlap.toFloat()
            paint.alpha = (alpha * 255).toInt()
            canvas.drawBitmap(
                right,
                i - overlap + left.width.toFloat(),
                0f,
                paint
            )
        }

        canvas.drawBitmap(
            right,
            (left.width - overlap).toFloat(),
            0f,
            null
        )

        return result
    }
}