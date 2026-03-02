package com.ovais.panorama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class TestPanoramaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NavGraph()
        }
    }
}