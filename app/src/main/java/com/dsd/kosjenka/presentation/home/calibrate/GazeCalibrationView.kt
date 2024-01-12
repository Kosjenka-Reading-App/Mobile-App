package com.dsd.kosjenka.presentation.home.calibrate

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import com.dsd.kosjenka.presentation.home.VisageWrapper
import com.dsd.kosjenka.presentation.home.VisageWrapper.ScreenSpaceGazeData
import com.dsd.kosjenka.utils.GLTriangle
import java.util.Random
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.properties.Delegates


@SuppressLint("ViewConstructor")
class GazeCalibrationView(context: Context, wrapper: VisageWrapper) : GLSurfaceView(context) {

    private val renderer: MyGLRenderer
    private var rand: Random

    private val context: Context
    private val visageWrapper: VisageWrapper

    private var calibrationPointCount: Int = 0
    private val MAX_CALIBRATION_POINTS = 20

    enum class GazeTrackerMode {
        Calibration, Estimation
    }

    var pointCords: FloatArray? = null
    var calibrationFinished = false
    var calibPointClickListener : (()->Unit)? = null

    private val TAG = "CalibrateSurface"
    init {
        setEGLContextClientVersion(2)
        renderer = MyGLRenderer(context, this)
        rand = Random()
        this.context = context
        visageWrapper = wrapper

        setZOrderOnTop(true)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.RGBA_8888)

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer)

        keepScreenOn = true
        preserveEGLContextOnPause = true
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!calibrationFinished){
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
                    Log.d(TAG, "$glX,$glY")

                    if (renderer.isShapeTapped(glX, glY)) {
                        Log.d(TAG, "calibration point added")
                        visageWrapper.AddGazeCalibrationPoint(androidX, androidY)
                        calibrationPointCount++

                        if (pointCords != null) {
                            setCalibrationPoint()
                            calibPointClickListener?.invoke()
                        }
//                        if (calibrationFinished) {
//                            visageWrapper.FinalizeOnlineGazeCalibration()
//
//                            Handler(context.mainLooper).post{
//                                showCalibrateCompeteDialog()
//                            }
//
//                            setGazeEstimatingMode()
//                        } else {
//                            val normalizedPoint = normalizePoint(pointCords!!)
//                            queueEvent{
//                                renderer.translateBy =
//                                    normalizedPoint.toList() //getRandomPosition()
//                                requestRender()
//                            }
//                            calibPointClickListener?.invoke()
//                        }
                    }
                }
            }
        }
        return true
    }

    private fun normalizePoint(point: FloatArray): FloatArray {
        point[0] *= width.toFloat() / height.toFloat()
        return point
    }

    private fun getRandomPosition(): List<Float> {
        val ratio: Float = width.toFloat() / height.toFloat()
        val x: Float = ratio * (rand.nextFloat() * 2 - 1)
        val y: Float = rand.nextFloat() * 2 - 1
        val z = 0.0f
        return floatArrayOf(x, y, z).toList()
    }

    val setGazeCalibratingMode = {
        queueEvent {
            renderer.currentGazeMode = GazeTrackerMode.Calibration
        }
    }


    val setGazeEstimatingMode = {
        queueEvent {
//            visageWrapper.onResume()
            renderer.currentGazeMode = GazeTrackerMode.Estimation
        }
    }

    fun setCalibrationPoint(){
        val normalizedPoint = normalizePoint(pointCords!!)
        Log.d(TAG, normalizedPoint.toString())
        queueEvent{
            renderer.translateBy =
                normalizedPoint.toList() //getRandomPosition()
            requestRender()
        }
    }

    fun isEstimationMode() : Boolean {
        return renderer.currentGazeMode == GazeTrackerMode.Estimation
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

        private var screenRatio by Delegates.notNull<Float>() //= width/ height.toFloat()

        @Volatile
        var translateBy: List<Float> = listOf(0f,0f,0f)
        @Volatile
        var currentGazeMode: GazeTrackerMode = GazeTrackerMode.Calibration

        init {
            this.context = context
            this.surfaceView = view
        }
        override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
            Log.d(TAG, "onSurfaceCreated")
            // Set the background frame color
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

            screenRatio = width.toFloat() / height.toFloat()
            mTriangle = GLTriangle()

            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = wm.defaultDisplay
            val size = Point()
            display.getSize(size)
        }

        private val translateMatrix = FloatArray(16)
        override fun onDrawFrame(gl: GL10) {
            // Redraw background color
            GLES20.glClearColor(0f, 0f, 0f, 0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

            Matrix.setIdentityM(translateMatrix, 0)

            val gazeData: ScreenSpaceGazeData? = visageWrapper.GetScreenSpaceGazeData()
            gazeData?.let {
                Log.d(TAG, "Tracking state: ${gazeData.inState}, Quality: ${gazeData.quality}")
            }
            if (currentGazeMode == GazeTrackerMode.Calibration){
                Matrix.translateM(translateMatrix, 0, translateBy[0],translateBy[1],translateBy[2])
            } else if (currentGazeMode == GazeTrackerMode.Estimation) {
                gazeData?.let {
                    val glx = (it.x * 2 - 1) * screenRatio
                    val gly = -(it.y * 2 - 1)
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
            visageWrapper.ResetTextures()
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

