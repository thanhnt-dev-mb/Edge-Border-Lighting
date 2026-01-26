package com.merryblue.baseapplication.helpers.ripple

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class RippleSimulation(val gridW: Int, val gridH: Int, var damping: Float = 0.86f) {
    private val w = gridW + 1
    private val h = gridH + 1

    private var prev = Array(w) { FloatArray(h) }
    private var curr = Array(w) { FloatArray(h) }
    private var interp = Array(w) { FloatArray(h) }

    private val fixedStep = 0.033f
    private var acc = 0f

    private var clampAbs = 60f

    fun update(dt: Float) {
        acc += dt
        while (acc > fixedStep) {
            stepOnce()
            val tmp = prev
            prev = curr
            curr = tmp
            acc -= fixedStep
        }

        val t = (acc / fixedStep).coerceIn(0f, 1f)
        for (y in 0 until h) {
            for (x in 0 until w) {
                interp[x][y] = prev[x][y] * (1f - t) + curr[x][y] * t
            }
        }
    }

    private fun stepOnce() {
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (x in 1 until w - 1 && y in 1 until h - 1) {
                    val v = ((prev[x - 1][y] + prev[x + 1][y] + prev[x][y - 1] + prev[x][y + 1]) / 2f) - curr[x][y]
                    curr[x][y] = min(10f, v)
                }
                curr[x][y] *= damping
            }
        }
    }

    fun addDrop(gx: Float, gy: Float, radius: Int = 3, strength: Float = -60f) {
        val cx = gx.toInt()
        val cy = gy.toInt()

        val r = max(1, radius)
        val k = r.toFloat()

        val w1 = gridW + 1
        val h1 = gridH + 1

        for (y in max(0, cy - r) until min(h1, cy + r + 1)) {
            for (x in max(0, cx - r) until min(w1, cx + r + 1)) {
                val dx = (x - gx)
                val dy = (y - gy)
                val dist = sqrt(dx * dx + dy * dy)
                val t = (dist * 1.57f) / k
                val c = max(0f, cos(t))
                var v = curr[x][y] + strength * c
                v = v.coerceIn(-clampAbs, clampAbs)
                curr[x][y] = v
            }
        }
    }

    fun heightAt(x: Int, y: Int): Float = interp[x][y]
}