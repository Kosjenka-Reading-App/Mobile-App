package com.dsd.kosjenka.presentation.home.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentCameraBinding
import com.dsd.kosjenka.presentation.MainActivity
import com.dsd.kosjenka.utils.Common
import com.google.android.gms.common.util.concurrent.HandlerExecutor
import java.nio.ByteBuffer
import java.util.Collections
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class Camera2Fragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback, LifecycleObserver {

    private lateinit var binding: FragmentCameraBinding

    private var visageWrapper: VisageWrapper? = null

    /**
     * A reference to the opened [CameraDevice].
     */
    private var mCameraDevice: CameraDevice? = null
    private var frontCameraActive = true

    /**
     * A reference to the current [CameraCaptureSession] for
     * preview.
     */
    private var mPreviewSession: CameraCaptureSession? = null

    /**
     * The [Size] of camera preview.
     */
    private var mPreviewSize: Size? = null

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private lateinit var cameraBackgroundThread: HandlerThread

    /**
     * A [Handler] for running tasks in the background.
     */
    private lateinit var cameraBackgroundHandler: Handler

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val mCameraOpenCloseLock = Semaphore(1)

    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its status.
     */
    private val mStateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            mCameraDevice = cameraDevice
            startPreview()
            mCameraOpenCloseLock.release()
            VisageWrapper.SetParameters(
                mPreviewSize!!.width,
                mPreviewSize!!.height,
                mOrientation!!,
                if (frontCameraActive) 1 else 0
            )
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
            this@Camera2Fragment.activity?.finish()
        }
    }

    private var mOrientation: Int? = null
    private var mPreviewBuilder: CaptureRequest.Builder? = null
    private var mImageReader: ImageReader? = null
    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        var image: Image? = null
        try {
            val tsA = System.currentTimeMillis()
            image = reader.acquireLatestImage()
            if (image == null) return@OnImageAvailableListener
            val planes: Array<Image.Plane> = image.planes
            val byteBuffer0: ByteBuffer = planes[0].buffer
            val byteBuffer1: ByteBuffer = planes[1].buffer
            val byteBuffer2: ByteBuffer = planes[2].buffer
            if (byteBuffer0 == null || byteBuffer1 == null || byteBuffer2 == null) return@OnImageAvailableListener
            VisageWrapper.WriteFrameStream(
                byteBuffer0,
                byteBuffer1,
                byteBuffer2,
                tsA,
                frameID++.toLong(),
                planes[1].pixelStride
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            image?.close()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //inflater.inflate(R.layout.fragment_camera, container, false)
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_camera, container, false)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        requireActivity().lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                super.onCreate(owner)
                owner.lifecycle.removeObserver(this)

                visageWrapper = VisageWrapper.get(context)
                visageWrapper!!.switchToCamera()
                visageWrapper!!.onCreate()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        visageWrapper!!.onResume()
        startCameraBackgroundThread()
        openCamera(640, 480, frontCameraActive)
    }

    override fun onPause() {
        super.onPause()
        visageWrapper!!.onPause()
        closeCamera()
        stopCameraBackgroundThread()
    }

    override fun onDetach() {
        super.onDetach()
        //        visageWrapper.onStop();
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val trackerView : View = visageWrapper!!.trackerView

        if (trackerView.parent != null){
            (trackerView.parent as ViewGroup).removeView(trackerView)
        }
        binding.cameraRoot.addView(trackerView, 0)
    }

    override fun onConfigurationChanged(newConfiguration: Configuration) {
        super.onConfigurationChanged(newConfiguration)
        visageWrapper!!.PauseTracker()
        val manager: CameraManager =
            requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val characteristics: CameraCharacteristics =
                manager.getCameraCharacteristics(mCameraDevice!!.id)
            mOrientation = getCorrectCameraOrientation(characteristics)
            VisageWrapper.SetParameters(
                mPreviewSize!!.width,
                mPreviewSize!!.height,
                mOrientation!!,
                if (frontCameraActive) 1 else 0
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Starts a background thread and its [Handler].
     */
    private fun startCameraBackgroundThread() {
        cameraBackgroundThread = HandlerThread("CameraBackground").apply { start() }
        cameraBackgroundHandler = Handler(cameraBackgroundThread.looper)
    }

    /**
     * Stops the background thread and its [Handler].
     */
    private fun stopCameraBackgroundThread() {
        cameraBackgroundThread.quitSafely()
        try {
            cameraBackgroundThread.join()
//            cameraBackgroundThread = null
//            cameraBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    /**
     * Gets whether you should show UI with rationale for requesting permissions.
     *
     * @param permissions The permissions your app wants to request.
     * @return Whether you can show permission rationale UI.
     */
    private fun shouldShowRequestPermissionRationale(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity() as MainActivity, permission)) {
                return true
            }
        }
        return false
    }

    /**
     * Requests permissions needed for recording video.
     */
    private fun requestVideoPermissions() {
        if (shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS)) {
            ConfirmationDialog().show(childFragmentManager, FRAGMENT_DIALOG)
        } else {
            ActivityCompat.requestPermissions(
                requireActivity() as MainActivity,
                VIDEO_PERMISSIONS,
                REQUEST_VIDEO_PERMISSIONS
            )
            requestPermissionLauncher.launch(VIDEO_PERMISSIONS)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in VIDEO_PERMISSIONS && !it.value)
                permissionGranted = false
        }
        if (!permissionGranted){
            ErrorDialog.newInstance("Permission request")
                .show(childFragmentManager, FRAGMENT_DIALOG)
        }
//        else {
//        }
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int, permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        Log.d(TAG, "onRequestPermissionsResult")
//        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
//            if (grantResults.size == VIDEO_PERMISSIONS.size) {
//                for (result in grantResults) {
//                    if (result != PackageManager.PERMISSION_GRANTED) {
//                        ErrorDialog.newInstance("Permission request")
//                            .show(childFragmentManager, FRAGMENT_DIALOG)
//                        break
//                    }
//                }
//            } else {
//                ErrorDialog.newInstance("Permission request")
//                    .show(childFragmentManager, FRAGMENT_DIALOG)
//            }
//        } else {
//            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        }
//    }

//    private fun hasPermissionsGranted(permissions: Array<String>): Boolean {
//        for (permission in permissions) {
//            if (ActivityCompat.checkSelfPermission(requireActivity() as MainActivity, permission)
//                != PackageManager.PERMISSION_GRANTED
//            ) {
//                return false
//            }
//        }
//        return true
//    }

    private fun allPermissionsGranted() = VIDEO_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }


    /**
     * Tries to open a [CameraDevice]. The result is listened by `mStateCallback`.
     */
    @SuppressLint("MissingPermission")
    private fun openCamera(width: Int, height: Int, frontCamera: Boolean) {
        if (!allPermissionsGranted()) {
            requestVideoPermissions()
            return
        }
        val activity: FragmentActivity? = activity
        if (null == activity || activity.isFinishing) {
            return
        }
        val manager: CameraManager =
            activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            Log.d(TAG, "tryAcquire")
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            val cameraList: Array<String> = manager.cameraIdList
            var cameraId: String? = null
            for (cam in cameraList) {
                if (frontCamera) {
                    if (CameraCharacteristics.LENS_FACING_FRONT == manager.getCameraCharacteristics(
                            cam
                        ).get(CameraCharacteristics.LENS_FACING)
                    ) {
                        cameraId = cam
                        break
                    }
                } else {
                    if (CameraCharacteristics.LENS_FACING_FRONT != manager.getCameraCharacteristics(
                            cam
                        ).get(CameraCharacteristics.LENS_FACING)
                    ) {
                        cameraId = cam
                        break
                    }
                }
            }
            if (cameraId == null && cameraList.isNotEmpty()) {
                cameraId =
                    cameraList[0] //camera facing front not found, use first camera from the list
            }

            // Choose the sizes for camera preview
            val characteristics: CameraCharacteristics = manager.getCameraCharacteristics(cameraId!!)
            val map: StreamConfigurationMap? = characteristics
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            mPreviewSize = chooseOptimalSize(
                map!!.getOutputSizes(
                    SurfaceTexture::class.java
                ),
                width, height, Size(width, height)
            )
            Log.i(
                TAG,
                "Current preview size is " + mPreviewSize!!.width + " " + mPreviewSize!!.height
            )
            mOrientation = getCorrectCameraOrientation(characteristics)

            manager.openCamera(cameraId!!, mStateCallback, null)
            mImageReader = ImageReader.newInstance(
                mPreviewSize!!.width,
                mPreviewSize!!.height,
                ImageFormat.YUV_420_888,
                2
            )
            mImageReader!!.setOnImageAvailableListener(
                onImageAvailableListener,
                cameraBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show()
            activity.finish()
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance("Camera error")
                .show(childFragmentManager, FRAGMENT_DIALOG)
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }
    }

    private fun closeCamera() {
        try {
            mCameraOpenCloseLock.acquire()
            closePreviewSession()
            if (null != mCameraDevice) {
                mCameraDevice!!.close()
//                mCameraDevice = null
            }
            if (null != mImageReader) {
                mImageReader!!.close()
                mImageReader = null
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.")
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    /**
     * Start the camera preview.
     */
    private fun startPreview() {
        if (null == mCameraDevice || null == mPreviewSize) {
            return
        }
        try {
            closePreviewSession()
            val surfaces: MutableList<Surface> = ArrayList()
            mPreviewBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            //Set up Surface for ImageReader
            val imageSurface = mImageReader!!.surface
            surfaces.add(imageSurface)
            mPreviewBuilder!!.addTarget(imageSurface)

            val sessionCallback = object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    mPreviewSession = session
                    updatePreview()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    val activity: FragmentActivity? = activity
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                mCameraDevice!!.createCaptureSession(surfaces, sessionCallback, cameraBackgroundHandler)
            } else {
                mCameraDevice!!.createCaptureSession(
                    SessionConfiguration(
                        SessionConfiguration.SESSION_REGULAR,
                        listOf(OutputConfiguration(imageSurface)),
                        HandlerExecutor(cameraBackgroundHandler.looper),
                        sessionCallback
                    )
                )
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Update the camera preview. [.startPreview] needs to be called in advance.
     */
    private fun updatePreview() {
        if (null == mCameraDevice) {
            return
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder!!)
            mPreviewSession!!.setRepeatingRequest(
                mPreviewBuilder!!.build(),
                null,
                cameraBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
    }

    private fun closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession!!.close()
            mPreviewSession = null
        }
    }

    private fun getCorrectCameraOrientation(info: CameraCharacteristics): Int {
        val rotation = (requireActivity() as MainActivity).windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        val result: Int
        val mSensorOrientation: Int? = info.get(CameraCharacteristics.SENSOR_ORIENTATION)
        val lensFacing: Int? = info.get(CameraCharacteristics.LENS_FACING)
        result = if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
            (mSensorOrientation!! + degrees) % 360
        } else {
            (mSensorOrientation!! - degrees + 360) % 360
        }
        return result
    }

    fun switchCamera() {
        visageWrapper!!.PauseTracker()
        frontCameraActive = !frontCameraActive
        closeCamera()
        openCamera(640, 480, frontCameraActive)
    }

    /**
     * Compares two `Size`s based on their areas.
     */
    internal class CompareSizesByArea : Comparator<Size> {
        override fun compare(lhs: Size, rhs: Size): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(
                lhs.width.toLong() * lhs.height -
                        rhs.width.toLong() * rhs.height
            )
        }
    }

    class ErrorDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val activity: FragmentActivity? = activity
            return AlertDialog.Builder(activity)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setPositiveButton("OK"
                ) { _, _ ->
                    activity?.finish()
                }
                .create()
        }

        companion object {
            private const val ARG_MESSAGE = "message"
            fun newInstance(message: String?): ErrorDialog {
                val dialog = ErrorDialog()
                val args = Bundle()
                args.putString(ARG_MESSAGE, message)
                dialog.arguments = args
                return dialog
            }
        }
    }

    class ConfirmationDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return AlertDialog.Builder(context)
                .setMessage("Permission request")
                .setPositiveButton("OK"
                ) { _, _ ->
                    ActivityCompat.requestPermissions(
                        (requireActivity() as MainActivity), VIDEO_PERMISSIONS,
                        REQUEST_VIDEO_PERMISSIONS
                    )
                }
                .setNegativeButton(
                    "Cancel"
                ) { _, _ -> requireActivity().finish() }
                .create()
        }
    }

    companion object {
        private var instance: Camera2Fragment? = null
        private const val TAG = "Camera2Fragment"
        private const val REQUEST_VIDEO_PERMISSIONS = 1
        private const val FRAGMENT_DIALOG = "dialog"
        private val VIDEO_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
//            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

        private var frameID = 0
        fun newInstance(): Camera2Fragment {
            return Camera2Fragment()
        }

        fun get(): Camera2Fragment {
            if (instance == null) {
                synchronized(Camera2Fragment::class.java) {
                    if (instance == null) {
                        instance = newInstance()
                    }
                }
            }
            return instance!!
        }

        /**
         * Given `choices` of `Size`s supported by a camera, chooses the smallest one whose
         * width and height are at least as large as the respective requested values, and whose aspect
         * ratio matches with the specified value.
         *
         * @param choices     The list of sizes that the camera supports for the intended output class
         * @param width       The minimum desired width
         * @param height      The minimum desired height
         * @param aspectRatio The aspect ratio
         * @return The optimal `Size`, or an arbitrary one if none were big enough
         */
        private fun chooseOptimalSize(
            choices: Array<Size>,
            width: Int,
            height: Int,
            aspectRatio: Size
        ): Size {
            // Collect the supported resolutions that are at least as big as the preview Surface
            val bigEnough: MutableList<Size> = ArrayList()
            val w = aspectRatio.width
            val h = aspectRatio.height
            for (option in choices) {
                Log.d(TAG, "size " + option.width + " " + option.height)
                if (option.height == option.width * h / w && option.width >= width && option.height >= height) {
                    bigEnough.add(option)
                }
            }

            // Pick the smallest of those, assuming we found any
            return if (bigEnough.size > 0) {
                Collections.min(bigEnough, CompareSizesByArea())
            } else {
                Log.e(TAG, "Couldn't find any suitable preview size")
                choices[choices.size / 2]
            }
        }
    }
}