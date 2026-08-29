package com.viennnaa.utilities.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Subscribes to sensors for as long as the screen is composed and the app is in
 * the foreground.
 *
 * Registration follows the lifecycle rather than composition alone: a sensor
 * left registered while the app is in the background keeps waking the device to
 * deliver readings nobody is looking at.
 *
 * @param types sensor types to listen to, e.g. [Sensor.TYPE_ACCELEROMETER].
 * @param onEvent called on the sensor thread, not the main thread. Write to
 *   Compose state, which is safe to set from any thread, rather than doing real
 *   work here.
 */
@Composable
fun SensorEffect(
    vararg types: Int,
    samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_GAME,
    onEvent: (SensorEvent) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEvent by rememberUpdatedState(onEvent)
    val typeList = types.toList()

    DisposableEffect(lifecycleOwner, typeList, samplingPeriodUs) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) = currentOnEvent(event)
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        fun register() {
            manager ?: return
            for (type in typeList) {
                manager.getDefaultSensor(type)?.let { sensor ->
                    manager.registerListener(listener, sensor, samplingPeriodUs)
                }
            }
        }

        fun unregister() {
            manager?.unregisterListener(listener)
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> register()
                Lifecycle.Event.ON_STOP -> unregister()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            register()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unregister()
        }
    }
}

/** Whether the device actually has every one of [types]. */
@Composable
fun rememberHasSensors(vararg types: Int): Boolean {
    val context = LocalContext.current
    val typeList = types.toList()
    return remember(typeList) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        manager != null && typeList.all { manager.getDefaultSensor(it) != null }
    }
}
