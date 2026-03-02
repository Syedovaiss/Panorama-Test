package com.ovais.panorama

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
class PanoramaEngine {

    private val frameQueue = ArrayDeque<Bitmap>()
    private val mutex = Mutex()

    suspend fun addFrame(bitmap: Bitmap) {
        mutex.withLock {
            if (frameQueue.size >= 3) {
                frameQueue.removeFirst() // removes old frames
            }
            frameQueue.addLast(bitmap)
        }
    }
    suspend fun buildPanorama(): Bitmap {
        return withContext(Dispatchers.Default) {
            mutex.withLock {
                // Reduce frame queue safely
                val iterator = frameQueue.iterator()
                var acc = if (iterator.hasNext()) iterator.next() else throw IllegalStateException("No frames")
                while (iterator.hasNext()) {
                    val bmp = iterator.next()
                    acc = SeamBlender.blend(acc, bmp, overlap = 200)
                }

                // After final panorama is built, recycle old frames
                frameQueue.forEach { it.recycle() }
                frameQueue.clear()

                acc
            }
        }
    }
}