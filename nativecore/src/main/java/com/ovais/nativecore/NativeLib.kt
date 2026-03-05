package com.ovais.nativecore

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

object NativeLib {

    interface StitchProgressListener {
        fun onProgressUpdate(message: String)
    }

    init {
        System.loadLibrary("nativecore")
    }

    external fun stringFromJNI(): String
    external fun testOpenCV(): String
    
    // Updated to accept an optional listener
    external fun stitchImages(imageByteArrays: Array<ByteArray>, listener: StitchProgressListener?): ByteArray?

    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }

    fun stitchBitmaps(bitmaps: List<Bitmap>, listener: StitchProgressListener? = null): Bitmap? {
        val arrays = bitmaps.map { bitmapToByteArray(it) }.toTypedArray()
        val stitchedBytes = stitchImages(arrays, listener) ?: return null
        return BitmapFactory.decodeByteArray(stitchedBytes, 0, stitchedBytes.size)
    }
}