package com.ovais.panorama

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class CaptureViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val images = mutableListOf<Bitmap>()
    private val repository = ImageRepository(application)

    fun addFrame(bitmap: Bitmap) {
        images.add(bitmap)
    }

   /* fun savePanorama() {
        viewModelScope.launch {
            val stitched = StitchEngine.stitch(images)
            repository.save(stitched)
        }
    }*/
}