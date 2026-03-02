package com.ovais.panorama_strip

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import androidx.core.graphics.createBitmap

class PanoramaProcessor(private val stripHeight: Int) {
    
    private var resultBitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var currentX = 0
    private val maxWidth = 20000

    /**
     * @param frame The latest camera frame
     * @param pixelsToAdd Number of pixels to move forward based on rotation
     */
    fun processFrame(frame: Bitmap, pixelsToAdd: Int): Bitmap? {
        if (pixelsToAdd <= 0) return resultBitmap // No movement, no new slice

        if (resultBitmap == null) {
            resultBitmap = createBitmap(maxWidth, stripHeight, Bitmap.Config.ARGB_8888)
            canvas = Canvas(resultBitmap!!)
        }

        if (currentX + pixelsToAdd >= maxWidth) {
            return null
        }

        val frameWidth = frame.width
        val frameHeight = frame.height
        
        // To get a clear image without stretching, we should ideally take a slice 
        // that matches 'pixelsToAdd' in width from the center of the frame.
        // If we take a fixed width slice but move 'pixelsToAdd', it stretches.
        
        val srcRect = Rect(
            frameWidth / 2 - pixelsToAdd / 2, 
            0, 
            frameWidth / 2 + pixelsToAdd / 2, 
            frameHeight
        )
        
        val destRect = Rect(
            currentX, 
            0, 
            currentX + pixelsToAdd, 
            stripHeight
        )

        // Using high-quality filtering
        canvas?.drawBitmap(frame, srcRect, destRect, null)
        currentX += pixelsToAdd
        
        return try {
            Bitmap.createBitmap(resultBitmap!!, 0, 0, currentX, stripHeight)
        } catch (e: Exception) {
            resultBitmap
        }
    }

    fun isFull(): Boolean = currentX >= maxWidth - 50

    fun reset() {
        resultBitmap = null
        canvas = null
        currentX = 0
    }
}
