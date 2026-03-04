package com.ovais.camera_360

import android.graphics.Bitmap
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ovais.camera_360.databinding.ActivityPanoramaViewBinding
import com.panoramagl.PLImage
import com.panoramagl.PLManager
import com.panoramagl.PLSphericalPanorama

class PanoramaViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPanoramaViewBinding
    private var plManager: PLManager? = null
    private var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate binding and set root
        binding = ActivityPanoramaViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load bitmap from your holder
        bitmap = PanoramaViewHolder.bitmap?.copy(Bitmap.Config.ARGB_8888, true)

        if (bitmap == null) {
            Toast.makeText(this, "No panorama bitmap provided!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize PLManager
        plManager = PLManager(this).apply {
            // contentView must be a FrameLayout or ViewGroup in XML
            setContentView(binding.contentView)
            onCreate()
            isAccelerometerEnabled = false // use false first for debugging touch scroll
            isInertiaEnabled = true
            isZoomEnabled = true
            isScrollingEnabled = true
        }

        // Setup panorama
        val panorama = PLSphericalPanorama().apply {
            setImage(PLImage(bitmap!!, false))
        }

        plManager?.panorama = panorama

        // Optional: start autorotation if desired
        // plManager?.startSensorialRotation()
    }

    override fun onResume() {
        super.onResume()
        plManager?.onResume()
    }

    override fun onPause() {
        plManager?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        plManager?.onDestroy()
        bitmap?.recycle()
        super.onDestroy()
    }

    // Touch forwarding
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return plManager?.onTouchEvent(event) ?: super.onTouchEvent(event)
    }
}