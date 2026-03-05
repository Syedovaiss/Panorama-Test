package com.ovais.panoramacv

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

class RotationSensor(
    context: Context,
    private val onFrameTrigger: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private var lastYaw = 0f

    fun start() = sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
    fun stop() = sensorManager.unregisterListener(this)
    fun resetYaw() { lastYaw = 0f }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)
        val yaw = Math.toDegrees(orientation[0].toDouble()).toFloat()

        if (abs(yaw - lastYaw) > 3.5f) { // capture every ~3.5°
            lastYaw = yaw
            onFrameTrigger()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}