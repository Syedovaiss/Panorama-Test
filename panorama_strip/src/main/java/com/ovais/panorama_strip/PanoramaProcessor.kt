package com.ovais.panorama_strip

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.graphics.createBitmap
class PanoramaProcessor(
    private val stripHeight: Int
) {

    private var resultBitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var currentX = 0
    private val maxWidth = 20000

    private val fixedSliceWidth = 6   // smaller = sharper, smoother
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG) // high-quality scaling

    fun processFrame(frame: Bitmap, verticalOffset: Int = 0): Bitmap? {

        if (resultBitmap == null) {
            resultBitmap = Bitmap.createBitmap(maxWidth, stripHeight, Bitmap.Config.ARGB_8888)
            canvas = Canvas(resultBitmap!!)
        }

        if (currentX + fixedSliceWidth >= maxWidth) return Bitmap.createBitmap(resultBitmap!!, 0, 0, currentX, stripHeight)

        val frameWidth = frame.width
        val frameHeight = frame.height
        val centerX = frameWidth / 2

        // Apply vertical offset to reduce shaky lines
        val srcRect = Rect(
            centerX - fixedSliceWidth / 2,
            verticalOffset.coerceIn(0, frameHeight - 1),
            centerX + fixedSliceWidth / 2,
            (frameHeight + verticalOffset).coerceIn(1, frameHeight)
        )

        val destRect = Rect(
            currentX,
            0,
            currentX + fixedSliceWidth,
            stripHeight
        )

        canvas?.drawBitmap(frame, srcRect, destRect, paint)
        currentX += fixedSliceWidth

        return Bitmap.createBitmap(resultBitmap!!, 0, 0, currentX, stripHeight)
    }

    fun isFull(): Boolean = currentX >= maxWidth

    fun reset() {
        resultBitmap = null
        canvas = null
        currentX = 0
    }
}