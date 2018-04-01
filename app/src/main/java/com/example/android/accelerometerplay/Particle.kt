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

import android.graphics.PointF


/*
 * Each of our particle holds its previous and current position, its
 * acceleration. for added realism each particle has its own friction
 * coefficient.
 */
class Particle<Data>(val data: Data) {
    private val mVel = PointF(0f, 0f)
    private val mPos = PointF(Math.random().toFloat(), Math.random().toFloat())

    init {
        adjust()
    }

    /*
     * Resolving constraints and collisions with the Verlet integrator
     * can be very simple, we simply need to move a colliding or
     * constrained particle in such way that the constraint is
     * satisfied.
     */
    fun getPos() = PointF(mPos.x, mPos.y)

    private fun adjust() {
        val xMax = SimulationView.xBound//0.031000065f
        val yMax = SimulationView.yBound//0.053403694f

        if (mPos.x >= xMax) {
            mPos.x = xMax
            mVel.x = 0f
        } else if (mPos.x <= -xMax) {
            mPos.x = -xMax
            mVel.x = 0f
        }

        if (mPos.y >= yMax) {
            mPos.y = yMax
            mVel.y = 0f
        } else if (mPos.y <= -yMax) {
            mPos.y = -yMax
            mVel.y = 0f
        }
    }

    fun computePhysics(sx: Float, sy: Float, dT: Float) {
        val ax = -sx / 5
        val ay = -sy / 5

        mPos.offset(
                mVel.x * dT + ax * dT * dT / 2,
                mVel.y * dT + ay * dT * dT / 2)

        mVel.offset(ax * dT, ay * dT)

        adjust()
    }

    fun collisionCheck(other: Particle<Data>): Boolean {
        var dx = this.mPos.x - other.mPos.x
        var dy = this.mPos.y - other.mPos.y
        var d = PointF.length(dx, dy)
        val collisionFlag = (d <= SimulationView.ballDiameter)

        // Check for collisions
        if (collisionFlag) {
            // Add a little bit of entropy, after nothing is perfect in the universe.
            dx += (Math.random().toFloat() - 0.5f) * 0.0001f
            dy += (Math.random().toFloat() - 0.5f) * 0.0001f
            d = PointF.length(dx, dy)

            // simulate the spring
            val c = 0.5f * (SimulationView.ballDiameter - d) / d
            val effectX = dx * c
            val effectY = dy * c

            other.mPos.offset(-effectX, -effectY)
            this.mPos.offset(effectX, effectY)

            adjust()
        }

        return collisionFlag
    }
}