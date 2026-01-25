package com.merryblue.baseapplication.helpers.ripple

import android.opengl.GLSurfaceView

class PreviewGLRenderer(
    private val core: WaterCore
) : GLSurfaceView.Renderer {

    override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        core.initGL()
    }

    override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
        core.setViewport(width, height)
    }

    override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
        core.drawFrame()
    }
}