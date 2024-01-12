package com.dsd.kosjenka.presentation.home.calibrate

import android.Manifest
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
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentCalibrateBinding
import com.dsd.kosjenka.presentation.MainActivity
import com.dsd.kosjenka.presentation.home.VisageWrapper
import com.dsd.kosjenka.presentation.home.camera.Camera2Fragment
import com.dsd.kosjenka.utils.Common
import com.dsd.kosjenka.utils.SharedPreferences
import com.google.android.gms.common.util.concurrent.HandlerExecutor
import dagger.hilt.android.AndroidEntryPoint
import java.nio.ByteBuffer
import java.util.Collections
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

@AndroidEntryPoint
class CalibrateFragment: Fragment() {

    private lateinit var binding: FragmentCalibrateBinding
    private val viewModel by viewModels<CalibrateViewModel>()
    private lateinit var surfaceView: GazeCalibrationView
    private lateinit var visageWrapper: VisageWrapper

    @Inject
    lateinit var preferences: SharedPreferences

    /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */
    private val cameraManager: CameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /**
     * A reference to the opened [CameraDevice].
     */
    private var mCameraDevice: CameraDevice? = null
    /**
     * A reference to the current [CameraCaptureSession] for
     * preview.
     */
    private var mPreviewSession: CameraCaptureSession? = null
    /**
     * The [Size] of camera preview.
     */
    private lateinit var mPreviewSize: Size
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

    private var mOrientation by Delegates.notNull<Int>()

    private lateinit var mPreviewBuilder: CaptureRequest.Builder
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
//            Log.d(TAG, "On image available")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            image?.close()
        }
    }

    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its status.
     */
    private val mStateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            mCameraDevice = cameraDevice
            startPreview()
            mCameraOpenCloseLock.release()
            VisageWrapper.SetParameters(
                mPreviewSize.width,
                mPreviewSize.height,
                mOrientation,
                1
            )
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            Log.d(TAG, "Camera disconnected")
//            mCameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            ErrorDialog.newInstance("Camera error")
                .show(childFragmentManager, FRAGMENT_DIALOG)
//            this@CalibrateFragment.activity?.finish()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_calibrate, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        visageWrapper = VisageWrapper.get(context)
        visageWrapper.switchToCalibrateScreen()
        visageWrapper.onCreate()
//        Common.showToast(requireContext(), "ready for calibration")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        visageWrapper.InitOnlineGazeCalibration()
        surfaceView = visageWrapper.calibrateView as GazeCalibrationView

        observeViewModel()

        if (surfaceView.parent != null){
            (surfaceView.parent as ViewGroup).removeView(surfaceView)
        }
        binding.calibrateFrame.addView(surfaceView)

        surfaceView.pointCords = viewModel.calibScreenPointList.value?.removeFirst()
        setGazeCalibrationMode()

        showAlertDialog(
            "Please look at the object and tap on it as it spawns on the screen.",
            "Gaze Tracking Calibration"
        ) {
            surfaceView.setCalibrationPoint()
        }

//        if (!surfaceView.isEstimationMode()){
//            surfaceView.pointCords = model.calibScreenPointList.value?.removeFirst()
//            setGazeCalibrationMode()
//
//            showAlertDialog(
//                "Please look at the object and tap on it as it spawns on the screen.",
//                "Gaze Tracking Calibration"
//            ) { surfaceView.setCalibrationPoint() }
//        } else {
//            val builder: AlertDialog.Builder = AlertDialog.Builder(context)
//            builder
//                .setMessage("Looks like tracker was already calibrated. Do you want to calibrate again?")
//                .setTitle("Gaze Tracking Calibration")
//                .setPositiveButton("Recalibrate") { dialog, which ->
////                    model.calibScreenPointList.value = model.getScreenGridPoints()
//                    surfaceView.pointCords = model.calibScreenPointList.value?.removeFirst()
//                    setGazeCalibrationMode()
//                    surfaceView.setCalibrationPoint()
//                }
//                .setNegativeButton("Continue") {dialog, which ->
//                    setGazeEstimationMode()
//                }
//
//            val dialog: AlertDialog = builder.create()
//            dialog.show()
//        }
    }

    override fun onResume() {
        super.onResume()
        visageWrapper.onResume()
        startCameraBackgroundThread()
        openCamera(640, 480, true)
    }

    override fun onPause() {
        super.onPause()
        visageWrapper.onPause()
        closeCamera()
        stopCameraBackgroundThread()
    }

    override fun onDestroy() {
        super.onDestroy()

//        visageWrapper.onDestroy()

    }

    fun observeViewModel(){
        viewModel.calibScreenPointList.value = viewModel.getScreenGridPoints()
        viewModel.calibScreenPointList.observe(viewLifecycleOwner) {
            viewModel.calibrationCount.value = it.size
        }

        viewModel.calibrationCount.value = viewModel.calibScreenPointList.value!!.size
        viewModel.calibrationCount.observe(viewLifecycleOwner) { count ->
            val c = count + 1
            binding.calibrationCounter.text = c.toString()
        }

        surfaceView.calibPointClickListener = {
            if (viewModel.calibrationCount.value == 0) {
                visageWrapper.FinalizeOnlineGazeCalibration()
//                visageWrapper.ResumeTracker()
                setGazeEstimationMode()

                val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                builder
                    .setMessage("Calibration is completed and Gaze Tracking configured.")
                    .setTitle("Calibration Finished")
                    .setPositiveButton("Finish") { dialog, which ->
                        preferences.isCalibrated = true
                        preferences.isGazeReadingMode = true
                        findNavController().popBackStack()
                    }

                val dialog: AlertDialog = builder.create()
                dialog.show()
            } else {
                surfaceView.pointCords = viewModel.calibScreenPointList.value?.removeFirstOrNull()
                viewModel.calibrationCount.value = viewModel.calibScreenPointList.value?.size
            }
        }
    }

    private fun setGazeCalibrationMode(){
        surfaceView.calibrationFinished = false
        surfaceView.setGazeCalibratingMode()
        binding.calibrationCounter.visibility = VISIBLE
    }

    private fun setGazeEstimationMode(){
        surfaceView.calibrationFinished = true
        surfaceView.setGazeEstimatingMode()
        binding.calibrationCounter.visibility = INVISIBLE
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        visageWrapper.PauseTracker()
        try {
            val characteristics: CameraCharacteristics =
                cameraManager.getCameraCharacteristics(mCameraDevice!!.id)
            mOrientation = getCorrectCameraOrientation(characteristics)
            VisageWrapper.SetParameters(
                mPreviewSize.width,
                mPreviewSize.height,
                mOrientation,
                1
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
            Camera2Fragment.ConfirmationDialog()
                .show(childFragmentManager, FRAGMENT_DIALOG)
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
    }

    private fun allPermissionsGranted() = VIDEO_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Tries to open a [CameraDevice]. The result is listened by `mStateCallback`.
     */
    private fun openCamera(width: Int, height: Int, frontCamera: Boolean) {
        if (!allPermissionsGranted()) {
            requestVideoPermissions()
            return
        }

        try {
            Log.d(TAG, "tryAcquire")
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            var cameraId = cameraManager.cameraIdList.find {
                cameraManager.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT
            }

            if (cameraId == null) {
                cameraId =
                    cameraManager.cameraIdList[0] //camera facing front not found, use first camera from the list
            }



            // Choose the sizes for camera preview
            val characteristics: CameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId!!)
            val sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(SurfaceTexture::class.java)
            mPreviewSize = chooseOptimalSize(
                sizes, width, height, Size(width, height)
            )
            Log.i(
                TAG,
                "Current preview size is " + mPreviewSize.width + " " + mPreviewSize.height
            )
            mOrientation = getCorrectCameraOrientation(characteristics)

            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestVideoPermissions()
                return
            }
            cameraManager.openCamera(cameraId, mStateCallback, null)
            mImageReader = ImageReader.newInstance(
                mPreviewSize.width,
                mPreviewSize.height,
                ImageFormat.YUV_420_888,
                2
            )
            mImageReader!!.setOnImageAvailableListener(
                onImageAvailableListener,
                cameraBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show()
            activity?.finish()
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance("Camera error")
                .show(parentFragmentManager, FRAGMENT_DIALOG)
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }
    }

    private fun closeCamera() {
        try {
            mCameraOpenCloseLock.acquire()
            closePreviewSession()
            mCameraDevice?.close()
            mImageReader?.close()
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
        try {
            closePreviewSession()
            val surfaces: MutableList<Surface> = ArrayList()
            mPreviewBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            //Set up Surface for ImageReader
            val imageSurface = mImageReader!!.surface
            surfaces.add(imageSurface)
            mPreviewBuilder.addTarget(imageSurface)

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
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder)
            mPreviewSession?.setRepeatingRequest(
                mPreviewBuilder.build(),
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
        mPreviewSession?.close()
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

    companion object{
        private var frameID = 0

        private const val TAG = "CalibrateFragment"
        private const val FRAGMENT_DIALOG = "dialog"
        private const val REQUEST_VIDEO_PERMISSIONS = 1
        private val VIDEO_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

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
                Collections.min(bigEnough) { lhs, rhs ->
                    java.lang.Long.signum(
                        lhs.width.toLong() * lhs.height -
                                rhs.width.toLong() * rhs.height)
                }
            } else {
                Log.e(TAG, "Couldn't find any suitable preview size")
                choices[choices.size / 2]
            }
        }
    }

    private fun showAlertDialog(message: String, title: String, callback: (() -> Unit)?){
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder
            .setMessage(message)
            .setTitle(title)
            .setPositiveButton("Continue") { dialog, which ->
                if (callback != null) {
                    callback()
                }
                dialog.dismiss()
            }

        val dialog: AlertDialog = builder.create()
        dialog.show()
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
}