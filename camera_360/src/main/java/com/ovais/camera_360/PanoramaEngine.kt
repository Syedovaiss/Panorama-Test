package com.ovais.camera_360

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PanoramaEngine(private val context: Context) {

    private val frameQueue = ArrayDeque<Bitmap>()
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default)

    fun start() {
        // Initialization if needed
    }

    fun stop() {
        frameQueue.forEach { it.recycle() }
        frameQueue.clear()
    }

    fun processFrame(image: ImageProxy) {
        // Convert ImageProxy to Bitmap and add to queue
        // This is a placeholder for the actual conversion logic. 
        // In a real implementation, you'd convert the image here and call addFrame.
        image.close()
    }

    suspend fun addFrame(bitmap: Bitmap) {
        mutex.withLock {
            if (frameQueue.size >= 3) {
                frameQueue.removeFirst().recycle()
            }
            frameQueue.addLast(bitmap)
        }
    }

    suspend fun buildPanorama(): Bitmap {
        return withContext(Dispatchers.Default) {
            mutex.withLock {
                val iterator = frameQueue.iterator()
                var acc = if (iterator.hasNext()) iterator.next() else throw IllegalStateException("No frames")
                while (iterator.hasNext()) {
                    val bmp = iterator.next()
                    acc = SeamBlender.blend(acc, bmp, overlap = 200)
                }
                frameQueue.clear()
                acc
            }
        }
    }
}
