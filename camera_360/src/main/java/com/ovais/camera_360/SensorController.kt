package com.ovais.camera_360

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

class SensorController(
    context: Context,
    private val onAngleChanged: (Float) -> Unit
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var lastYaw = 0f

    fun start() {
        sensorManager.registerListener(
            this,
            rotationSensor,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    fun stop() = sensorManager.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        val orientations = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientations)

        val yaw = Math.toDegrees(orientations[0].toDouble()).toFloat()

        if (abs(yaw - lastYaw) > 15) {
            lastYaw = yaw
            onAngleChanged(yaw)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}