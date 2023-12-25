package com.dsd.kosjenka.presentation.home.camera

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import com.dsd.kosjenka.presentation.home.camera.VisageWrapper.ScreenSpaceGazeData
import com.dsd.kosjenka.utils.GLTriangle
import java.util.Random
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


@SuppressLint("ViewConstructor")
class GazeCalibrationView(context: Context, wrapper: VisageWrapper) : GLSurfaceView(context) {

    private val renderer: MyGLRenderer
    private var rand: Random

    private val context: Context
    private val visageWrapper: VisageWrapper

    private var addPointCounter: Int = 0
    private val MAX_CALIBRATION_POINTS = 20

    private val TAG = "CalibrateSurface"
    init {

        // Create an OpenGL ES 1.0 context
        setEGLContextClientVersion(2)

        renderer = MyGLRenderer(context, this)
        rand = Random()
        this.context = context
        visageWrapper = wrapper

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer)

        keepScreenOn = true
        preserveEGLContextOnPause = true
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (addPointCounter <= MAX_CALIBRATION_POINTS){
            // MotionEvent reports input details from the touch screen
            // and other input controls. In this case, you are only
            // interested in events where the touch position changed.
            val androidX: Float = e.x/ width
            val androidY: Float = e.y/ height

            val ratio: Float = width.toFloat() / height.toFloat()
            // Convert touch coordinates into normalized device
            // coordinates, keeping in mind that Android's Y
            // coordinates are inverted.
            val glX: Float = (androidX * 2 - 1)
            val glY: Float = -(androidY * 2 - 1)

            when (e.action) {
                MotionEvent.ACTION_UP -> {
                    Log.d(TAG, "$androidX,$androidY")
                    queueEvent{
                        if (renderer.isShapeTapped(glX, glY)) {
                            visageWrapper.AddGazeCalibrationPoint(androidX, androidY)
                            addPointCounter++

                            if (addPointCounter >= MAX_CALIBRATION_POINTS) {
                                renderer.calibrateMode = false
                                visageWrapper.FinalizeOnlineGazeCalibration()
                                Handler(context.mainLooper).post{ showCalibrateCompeteDialog() }
                            }

                            Log.d(TAG, "calibration point added")
                            renderer.translateBy = getRandomPosition()
                            requestRender()
                        }
                    }
                }
            }

            return true
        } else {
            return false
        }

    }

    private fun getRandomPosition(): List<Float> {
        val ratio: Float = width.toFloat() / height.toFloat()
        val x: Float = ratio * (rand.nextFloat() * 2 - 1)
        val y: Float = rand.nextFloat() * 2 - 1
        val z = 0.0f
        return floatArrayOf(x, y, z).toList()
    }

    private fun showCalibrateCompeteDialog(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder
            .setMessage("Calibration is completed and Gaze Tracking configured.")
            .setTitle("Calibration Finished")
            .setPositiveButton("Continue") { dialog, which ->
                // Do something.
            }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    inner class MyGLRenderer(context: Context, view: GazeCalibrationView) : Renderer {

        private val context: Context
        private val surfaceView: GLSurfaceView

        // vPMatrix is an abbreviation for "Model View Projection Matrix"
        private val vPMatrix = FloatArray(16)
        private val projectionMatrix = FloatArray(16)
        private val viewMatrix = FloatArray(16)

        private lateinit var mTriangle : GLTriangle

        private val TAG = "CalibrateRenderer"

        private var screenRatio = width/ height.toFloat()

        @Volatile
        var translateBy: List<Float> = listOf(0f,0f,0f)
        @Volatile
        var calibrateMode: Boolean = true

        init {
            this.context = context
            this.surfaceView = view
        }
        override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
            Log.d(TAG, "onSurfaceCreated")
            // Set the background frame color
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

            mTriangle = GLTriangle()
        }

        private val translateMatrix = FloatArray(16)
        override fun onDrawFrame(gl: GL10) {
            // Redraw background color
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

            Matrix.setIdentityM(translateMatrix, 0)
            if (calibrateMode){
                Matrix.translateM(translateMatrix, 0, translateBy[0],translateBy[1],translateBy[2])
            } else {
                val gazeData: ScreenSpaceGazeData? = visageWrapper.GetScreenSpaceGazeData()
                gazeData?.let {
                    val glx = (it.x * 2 - 1) * screenRatio
                    val gly = -(it.y * 2 - 1)
//                    Log.d(TAG, "${it.index},${glx},${gly},${gazeData.inState},${gazeData.quality}")
                    Matrix.translateM(translateMatrix, 0, glx, gly, 0f)
                }
            }

            // Set the camera position (View matrix)
            Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
            // Calculate the projection and view transformation
            val scratch = FloatArray(16)
            Matrix.multiplyMM(scratch, 0, projectionMatrix, 0, viewMatrix, 0)

            Matrix.multiplyMM(vPMatrix, 0, scratch, 0, translateMatrix,0)

            mTriangle.draw(vPMatrix)
        }

        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)

            screenRatio = width.toFloat() / height.toFloat()
            // this projection matrix is applied to object coordinates
            // in the onDrawFrame() method
            Matrix.frustumM(projectionMatrix, 0, -screenRatio, screenRatio, -1f, 1f, 3f, 7f)
        }

        fun isShapeTapped(normalizedX: Float, normalizedY: Float):Boolean {
            return mTriangle.isPointInTriangle(
                normalizedX,
                normalizedY,
                vPMatrix
            )
        }
    }
}

