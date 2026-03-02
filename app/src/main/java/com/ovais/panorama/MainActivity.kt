package com.ovais.panorama

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ovais.camera_360.Camera360Activity
import com.ovais.panorama.ui.theme.PanoramaTheme
import com.ovais.panorama_strip.PanoramaStripActivity

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PanoramaTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    "Panorama Pro",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                ) { innerPadding ->
                    LandingScreen(
                        modifier = Modifier.padding(innerPadding),
                        onFeatureClick = { feature ->
                            when (feature) {
                                Feature.StripPanorama -> startActivity(Intent(this, PanoramaStripActivity::class.java))
                                Feature.Sensor360 -> startActivity(Intent(this, Camera360Activity::class.java))
                                Feature.LiveStitch -> startActivity(Intent(this, LivePanoramaActivity::class.java))
                                Feature.VideoStitch -> startActivity(Intent(this, CaptureVideoPanoramaActivity::class.java))
                                Feature.TestPanorama -> startActivity(Intent(this, TestPanoramaActivity::class.java))
                            }
                        }
                    )
                }
            }
        }
    }
}

enum class Feature(
    val title: String,
    val description: String,
    @param:DrawableRes val icon: Int
) {
    StripPanorama(
        "Strip Panorama",
        "iPhone style real-time strip capture",
        R.drawable.ic_add_photo
    ),
    Sensor360(
        "360° Sensor Stitch",
        "Sensor-based automatic stitching",
        R.drawable.ic_add_photo
    ),
    LiveStitch(
        "Live Stitching",
        "Real-time OpenCV feature matching",
        R.drawable.ic_add_photo
    ),
    VideoStitch(
        "Video to Panorama",
        "Extract frames from video and stitch",
        R.drawable.ic_add_photo
    ),
    TestPanorama(
        "Capture & View",
        "Basic capture with 360 viewer",
        R.drawable.ic_add_photo
    )
}

@Composable
fun LandingScreen(
    modifier: Modifier = Modifier,
    onFeatureClick: (Feature) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Explore Features",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(Feature.entries) { feature ->
            FeatureCard(feature = feature, onClick = { onFeatureClick(feature) })
        }
    }
}

@Composable
fun FeatureCard(
    feature: Feature,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(feature.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
