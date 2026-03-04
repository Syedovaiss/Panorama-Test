package com.ovais.nativecore

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

object NativeLib {

    /**
     * A native method that is implemented by the 'nativecore' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    init {
        System.loadLibrary("nativecore")
    }

    external fun testOpenCV(): String
    external fun stitchImages(imageByteArrays: Array<ByteArray>): ByteArray?
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }

    fun stitchBitmaps(bitmaps: List<Bitmap>): Bitmap? {
        val arrays = bitmaps.map { bitmapToByteArray(it) }.toTypedArray()
        val stitchedBytes = stitchImages(arrays) ?: return null
        return BitmapFactory.decodeByteArray(stitchedBytes, 0, stitchedBytes.size)
    }
}