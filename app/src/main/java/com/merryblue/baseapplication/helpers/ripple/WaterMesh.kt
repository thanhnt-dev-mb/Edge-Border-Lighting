package com.merryblue.baseapplication.helpers.ripple

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.max
import kotlin.times

class WaterMesh(gridW: Int, gridH: Int) {

    private val w = gridW + 1
    private val h = gridH + 1

    private val floatsPerVertex = 7
    private val vertexCount = w * h
    private val indexCount = gridW * gridH * 6

    private val vertices: FloatArray = FloatArray(vertexCount * floatsPerVertex)
    private val vbuf: FloatBuffer = ByteBuffer
        .allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    private val indices: ShortArray = ShortArray(indexCount)
    private val ibuf: ShortBuffer = ByteBuffer
        .allocateDirect(indices.size * 2)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()

    init {
        buildIndices(gridW, gridH)
        ibuf.put(indices).position(0)
    }

    /**
     * Update vertices in PIXEL space, so it can fill full screen with crop/fit.
     * scale + offset are computed in renderer.
     */
    fun updateFrom(
        sim: RippleSimulation,
        xOffset: Float,
        xOffsetStep: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float
    ) {
        val invW = 1f / max(1, (w - 1)).toFloat()
        val invH = 1f / max(1, (h - 1)).toFloat()

        val parallax = if (xOffsetStep > 0f) (0.5f - xOffset) else 0f

        var idx = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val fx = x.toFloat()
                val fy = y.toFloat()

                val (dx, dy) = if (x in 1 until w - 1 && y in 1 until h - 1) {
                    val left = sim.heightAt(x - 1, y)
                    val right = sim.heightAt(x + 1, y)
                    val up = sim.heightAt(x, y - 1)
                    val down = sim.heightAt(x, y + 1)
                    (left - right) to (up - down)
                } else {
                    0f to 0f
                }

                //  Position in pixel space
                val px = offsetX + fx * scale
                val py = offsetY + fy * scale

                vertices[idx++] = px
                vertices[idx++] = py
                vertices[idx++] = 0f

                // UV with perturb
                val u = ((fx + dx) * invW) + parallax * 0.15f
                val v = ((fy + dy) * invH)

                vertices[idx++] = u
                vertices[idx++] = v

                // uv1 for lighting
                vertices[idx++] = dx * invW
                vertices[idx++] = dy * invH
            }
        }

        vbuf.position(0)
        vbuf.put(vertices)
        vbuf.position(0)
    }

    fun bind(aPos: Int, aUv0: Int, aUv1: Int) {
        vbuf.position(0)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, floatsPerVertex * 4, vbuf)

        vbuf.position(3)
        GLES20.glEnableVertexAttribArray(aUv0)
        GLES20.glVertexAttribPointer(aUv0, 2, GLES20.GL_FLOAT, false, floatsPerVertex * 4, vbuf)

        vbuf.position(5)
        GLES20.glEnableVertexAttribArray(aUv1)
        GLES20.glVertexAttribPointer(aUv1, 2, GLES20.GL_FLOAT, false, floatsPerVertex * 4, vbuf)
    }

    fun draw() {
        ibuf.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, ibuf)
    }

    fun unbind(aPos: Int, aUv0: Int, aUv1: Int) {
        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aUv0)
        GLES20.glDisableVertexAttribArray(aUv1)
    }

    fun release() {}

    private fun buildIndices(gridW: Int, gridH: Int) {
        var i = 0
        for (y in 0 until gridH) {
            var s = (w * y).toShort()
            for (x in 0 until gridW) {
                val s1 = (s + 1).toShort()
                val below = (s + w).toShort()
                val below1 = (below + 1).toShort()

                indices[i++] = s
                indices[i++] = s1
                indices[i++] = below

                indices[i++] = s1
                indices[i++] = below1
                indices[i++] = below

                s = s1
            }
        }
    }
}