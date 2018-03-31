/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.accelerometerplay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.DisplayMetrics
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout

class SimulationView(context: Context) : FrameLayout(context), SensorEventListener {
    companion object {
        val NUM_PARTICLES = 5
        val ballDiameter = 0.006f
        var xBound: Float = 0f
        var yBound: Float = 0f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val mDisplay = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay

    private val mAccelerometer: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val mMetersToPixelsX: Float
    private val mMetersToPixelsY: Float

    private var mXOrigin: Float = 0f
    private var mYOrigin: Float = 0f
    private var mSensorX: Float = 0f
    private var mSensorY: Float = 0f

    private val mParticleSystem: ParticleSystem<View>


    /*
             * It is not necessary to get accelerometer events at a very high
             * rate, by using a slower rate (SENSOR_DELAY_UI), we get an
             * automatic low-pass filter, which "extracts" the gravity component
             * of the acceleration. As an added benefit, we use less power and
             * CPU resources.
             */
    fun startSimulation() =
            sensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME)


    fun stopSimulation() =
            sensorManager.unregisterListener(this)

    init {
        val metrics = DisplayMetrics()

        mDisplay.getMetrics(metrics)

        mMetersToPixelsX = metrics.xdpi / 0.0254f
        mMetersToPixelsY = metrics.ydpi / 0.0254f

        // rescale the mBalls so it's about 0.5 cm on screen
        val mDstWidth = (ballDiameter * mMetersToPixelsX + 0.5f).toInt()
        val mDstHeight = (ballDiameter * mMetersToPixelsY + 0.5f).toInt()

        val w = metrics.widthPixels
        val h = metrics.heightPixels

        mXOrigin = (w - mDstWidth) * 0.5f
        mYOrigin = (h - mDstHeight) * 0.5f

        xBound = (w / mMetersToPixelsX - ballDiameter) * 0.5f
        yBound = (h / mMetersToPixelsY - ballDiameter) * 0.5f

        mParticleSystem = ParticleSystem(NUM_PARTICLES) {
            val ballView = View(context)

            ballView.setBackgroundResource(R.drawable.ball)
            ballView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            addView(ballView, ViewGroup.LayoutParams(mDstWidth, mDstHeight))

            ballView
        }

        val opts = BitmapFactory.Options()

        opts.inPreferredConfig = Bitmap.Config.RGB_565
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) {
            return
        }

        /*
         * record the accelerometer data, the event's timestamp as well as
         * the current time. The latter is needed so we can calculate the
         * "present" time during rendering. In this application, we need to
         * take into account how the screen is rotated with respect to the
         * sensors (which always return data in a coordinate space aligned
         * to with the screen in its native orientation).
         */

        when (mDisplay.rotation) {
            Surface.ROTATION_0 -> {
                mSensorX = event.values[0]
                mSensorY = event.values[1]
            }
            Surface.ROTATION_90 -> {
                mSensorX = -event.values[1]
                mSensorY = event.values[0]
            }
            Surface.ROTATION_180 -> {
                mSensorX = -event.values[0]
                mSensorY = -event.values[1]
            }
            Surface.ROTATION_270 -> {
                mSensorX = event.values[1]
                mSensorY = -event.values[0]
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        /*
         * Compute the new position of our object, based on accelerometer
         * data and present time.
         */
        val particleSystem = mParticleSystem
        val now = System.currentTimeMillis()
        val sx = mSensorX
        val sy = mSensorY

        particleSystem.update(sx, sy, now)

        val xc = mXOrigin
        val yc = mYOrigin
        val xs = mMetersToPixelsX
        val ys = mMetersToPixelsY

        particleSystem.updateParticles { pos, ballView ->
            /*
             * We transform the canvas so that the coordinate system matches
             * the sensors coordinate system with the origin in the center
             * of the screen and the unit is the meter.
             */
            val x = xc + pos.x * xs
            val y = yc - pos.y * ys

            ballView.translationX = x
            ballView.translationY = y
        }

        // and make sure to redraw asap
        invalidate()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }
}
