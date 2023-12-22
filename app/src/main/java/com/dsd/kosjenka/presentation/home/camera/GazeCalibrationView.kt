package com.dsd.kosjenka.presentation.home.camera

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.dsd.kosjenka.utils.GLTriangle
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@SuppressLint("ViewConstructor")
class GazeCalibrationView(context: Context, visageWrapper: VisageWrapper) : GLSurfaceView(context) {

    private val renderer: MyGLRenderer

    init {

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        renderer = MyGLRenderer(context, this)

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer)

        keepScreenOn = true
    }

    class MyGLRenderer(context: Context, view: GazeCalibrationView) : Renderer {

        private lateinit var mTriangle : GLTriangle
        private val vPMatrix = FloatArray(16)
        private val projectionMatrix = FloatArray(16)
        private val viewMatrix = FloatArray(16)
        override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
            // Set the background frame color
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

            mTriangle = GLTriangle()
        }

        override fun onDrawFrame(unused: GL10) {
            // Redraw background color
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

            // Set the camera position (View matrix)
            Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

            // Calculate the projection and view transformation
            Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)


            mTriangle.draw(vPMatrix)
        }

        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)

            val ratio: Float = width.toFloat() / height.toFloat()

            // this projection matrix is applied to object coordinates
            // in the onDrawFrame() method
            Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
        }
    }
}

