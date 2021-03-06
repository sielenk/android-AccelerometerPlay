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
 * A particle system is just a collection of particles
 */
class ParticleSystem<Data>(count: Int, initializer: (Int) -> Data) {
    private val mBalls = Array(count) { i -> Particle(initializer(i)) }
    private var mLastT: Long? = null

    /*
     * Update the position of each particle in the system using the Verlet integrator.
     */
    private fun updatePositions(sx: Float, sy: Float, timestamp: Long) {
        val lastT = mLastT
        mLastT = timestamp

        if (lastT != null) {
            val dT = (timestamp - lastT).toFloat() / 1000f /* (1.0f / 1000000000.0f)*/
            for (ball in mBalls) {
                ball.computePhysics(sx, sy, dT)
            }
        }
    }

    /*
     * Performs one iteration of the simulation. First updating the
     * position of all the particles and resolving the constraints and
     * collisions.
     */
    fun update(sx: Float, sy: Float, now: Long) {
        // update the system's positions
        updatePositions(sx, sy, now)

        // We do no more than a limited number of iterations
        val maxIterations = 10

        /*
         * Resolve collisions, each particle is tested against every
         * other particle for collision. If a collision is detected the
         * particle is moved away using a virtual spring of infinite
         * stiffness.
         */
        var more = true
        val count = mBalls.size
        var k = 0
        while (k < maxIterations && more) {
            more = false
            for (i in 0 until count) {
                val curr = mBalls[i]
                for (j in i + 1 until count) {
                    val ball = mBalls[j]
                    more = more or ball.collisionCheck(curr)
                }
            }
            k++
        }
    }

    fun updateParticles(transform: (PointF, Data) -> Unit) {
        for (ball in mBalls) {
            transform(ball.getPos(), ball.data)
        }
    }
}
