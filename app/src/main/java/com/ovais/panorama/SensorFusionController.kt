package com.ovais.panorama

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

class SensorFusionController(
    context: Context,
    private val onStableRotation: (Float) -> Unit
) : SensorEventListener {

    private val manager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationSensor =
        manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var lastYaw = 0f
    private var smoothedYaw = 0f

    fun start() {
        manager.registerListener(
            this,
            rotationSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    override fun onSensorChanged(event: SensorEvent) {

        val matrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(matrix, event.values)

        val orientation = FloatArray(3)
        SensorManager.getOrientation(matrix, orientation)

        val yaw = Math.toDegrees(orientation[0].toDouble()).toFloat()

        // Low pass filter
        smoothedYaw = 0.9f * smoothedYaw + 0.1f * yaw

        if (abs(smoothedYaw - lastYaw) > 12f) {
            lastYaw = smoothedYaw
            onStableRotation(smoothedYaw)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}