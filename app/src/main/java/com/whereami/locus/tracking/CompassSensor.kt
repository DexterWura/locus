package com.whereami.locus.tracking

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Reads device azimuth (compass heading): 0° = North, clockwise.
 * Uses rotation vector when available, otherwise accelerometer + magnetometer.
 */
class CompassSensor(
    context: Context
) : SensorEventListener {
    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private val _headingDegrees = MutableStateFlow<Float?>(null)
    val headingDegrees: StateFlow<Float?> = _headingDegrees.asStateFlow()

    private val _available = MutableStateFlow(isHardwareAvailable())
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private var listening = false
    private var lastPublished: Float? = null

    fun isHardwareAvailable(): Boolean {
        return rotationSensor != null || (accelerometer != null && magnetometer != null)
    }

    fun start() {
        if (listening) return
        _available.value = isHardwareAvailable()
        if (!_available.value) return

        listening = true
        if (rotationSensor != null) {
            sensorManager.registerListener(
                this,
                rotationSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        } else {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            magnetometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    fun stop() {
        if (!listening) return
        listening = false
        sensorManager.unregisterListener(this)
        hasGravity = false
        hasGeomagnetic = false
        lastPublished = null
        _headingDegrees.value = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                publishFromRotationMatrix(rotationMatrix)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                lowPass(event.values, gravity)
                hasGravity = true
                tryComputeFallback()
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                lowPass(event.values, geomagnetic)
                hasGeomagnetic = true
                tryComputeFallback()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun tryComputeFallback() {
        if (!hasGravity || !hasGeomagnetic) return
        val rotationMatrix = FloatArray(9)
        val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
        if (success) {
            publishFromRotationMatrix(rotationMatrix)
        }
    }

    private fun publishFromRotationMatrix(rotationMatrix: FloatArray) {
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)
        val azimuthRad = orientation[0]
        var degrees = Math.toDegrees(azimuthRad.toDouble()).toFloat()
        degrees = (degrees + 360f) % 360f

        val previous = lastPublished
        if (previous != null && shortestAngleDelta(previous, degrees) < 2f) {
            return
        }
        lastPublished = degrees
        _headingDegrees.value = degrees
    }

    private fun shortestAngleDelta(from: Float, to: Float): Float {
        var delta = abs(to - from) % 360f
        if (delta > 180f) delta = 360f - delta
        return delta
    }

    private fun lowPass(input: FloatArray, output: FloatArray) {
        val alpha = 0.15f
        for (i in output.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
    }
}
