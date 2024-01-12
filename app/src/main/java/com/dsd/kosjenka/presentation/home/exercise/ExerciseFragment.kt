package com.dsd.kosjenka.presentation.home.exercise

import android.Manifest
import android.app.AlertDialog
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
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.FragmentExerciseBinding
import com.dsd.kosjenka.domain.models.Completion
import com.dsd.kosjenka.domain.models.Exercise
import com.dsd.kosjenka.presentation.MainActivity
import com.dsd.kosjenka.presentation.home.calibrate.GazeCalibrationView
import com.dsd.kosjenka.presentation.home.VisageWrapper
import com.dsd.kosjenka.presentation.home.calibrate.CalibrateFragment
import com.dsd.kosjenka.presentation.home.camera.Camera2Fragment
import com.dsd.kosjenka.utils.Common
import com.dsd.kosjenka.utils.SharedPreferences
import com.dsd.kosjenka.utils.TokenManager
import com.dsd.kosjenka.utils.UiStates
import com.google.android.gms.common.util.concurrent.HandlerExecutor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.Collections
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

@AndroidEntryPoint
class ExerciseFragment : Fragment(), HighlightCallback {

    private lateinit var binding: FragmentExerciseBinding
    private val viewModel by viewModels<ExerciseViewModel>()

    private var delayMillis = 1000L
    private var isPlaying = false
    private var hasReachedEnd = false
    private lateinit var handler: Handler

    private val timerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var elapsedSeconds = 0
    private var isTimerRunning = false
    private var timerJob: Job? = null

    private lateinit var thisExercise: Exercise

    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var tokenManager: TokenManager

    private lateinit var gazeSurfaceView: GazeCalibrationView
    private lateinit var visageWrapper: VisageWrapper

    private val cameraManager: CameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    private var mCameraDevice: CameraDevice? = null
    private var mPreviewSession: CameraCaptureSession? = null
    private lateinit var cameraBackgroundThread: HandlerThread
    private lateinit var cameraBackgroundHandler: Handler
    private val mCameraOpenCloseLock = Semaphore(1)
    private var mOrientation by Delegates.notNull<Int>()
    private lateinit var mPreviewBuilder: CaptureRequest.Builder
    private var mImageReader: ImageReader? = null
    private lateinit var mPreviewSize: Size

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        visageWrapper = VisageWrapper.get(context)
        visageWrapper.switchToExerciseScreen()
        visageWrapper.onCreate()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding =
            DataBindingUtil.inflate(layoutInflater, R.layout.fragment_exercise, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity
        activity.setSupportActionBar(binding.exerciseToolbar)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val args = ExerciseFragmentArgs.fromBundle(requireArguments())

        binding.exerciseToolbar.title = args.exerciseTitle
        handler = Handler(Looper.getMainLooper())
        binding.exerciseText.setHighlightCallback(this)

        observeViewModel()
        viewModel.getExercise(args.exerciseId, preferences.userId)
        setupSpeedButtons()
        setupPlayPause()
        setupFontSlider()
        binding.progressBarTimer.max = 100

        binding.enableDisableGaze.setOnClickListener {
            if (preferences.isGazeReadingMode) {
                pauseGaze()
                preferences.isGazeReadingMode = false
                gazeSurfaceView.visibility = View.INVISIBLE
                binding.enableDisableGaze.setImageResource(R.drawable.center_focus_weak)
                Common.showToast(binding.root.context, "Gaze tracking OFF")
            } else {
                if (!preferences.isCalibrated) {
                    startCalibrate()
                } else {
                    preferences.isGazeReadingMode = true
                    resumeGaze()
                    gazeSurfaceView.visibility = View.VISIBLE
                    binding.enableDisableGaze.setImageResource(R.drawable.center_focus_strong)
                    promptRecalibrate()
                    Common.showToast(binding.root.context, "Gaze tracking ON")
                }
            }
        }

        gazeSurfaceView = visageWrapper.readingModeGazeView as GazeCalibrationView

        if (gazeSurfaceView.parent != null){
            (gazeSurfaceView.parent as ViewGroup).removeView(gazeSurfaceView)
        }
        binding.gazeTrackFrame.addView(gazeSurfaceView)
        gazeSurfaceView.setGazeEstimatingMode()
        gazeSurfaceView.visibility = View.INVISIBLE
        binding.enableDisableGaze.setImageResource(R.drawable.center_focus_weak)

        if (preferences.isGazeReadingMode) {
            if (!preferences.isCalibrated) {
                startCalibrate()
            } else {
                promptRecalibrate()
                gazeSurfaceView.calibrationFinished = true
                gazeSurfaceView.visibility = View.VISIBLE
                binding.enableDisableGaze.setImageResource(R.drawable.center_focus_strong)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeGaze()
    }

    override fun onPause() {
        super.onPause()
        pauseGaze()
    }

    private fun resumeGaze(){
        if (preferences.isGazeReadingMode){
            visageWrapper.onResume()
            startCameraBackgroundThread()
            openCamera(640, 480, true)
        }
    }

    private fun pauseGaze(){
        if (preferences.isGazeReadingMode){
            visageWrapper.onPause()
            closeCamera()
            stopCameraBackgroundThread()
        }
    }

    private fun startCalibrate() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder
            .setMessage("Before using gaze tracker it needs to be calibrated first. Please proceed to calibrate.")
            .setTitle("Gaze Tracker")
            .setPositiveButton("Calibrate") { dialog, which ->
                findNavController().navigate(
                    ExerciseFragmentDirections.actionExerciseFragmentToCalibrateFragment()
                )
            }
            .setNegativeButton("Cancel") {dialog, which ->
                preferences.isGazeReadingMode = false
                preferences.isCalibrated = false
            }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun promptRecalibrate(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder
            .setMessage("Tracker is already calibrated!")
            .setTitle("Gaze Tracker")
            .setPositiveButton("Recalibrate") { dialog, which ->
                findNavController().navigate(
                    ExerciseFragmentDirections.actionExerciseFragmentToCalibrateFragment()
                )
            }
            .setNegativeButton("Continue", null)

        val dialog: AlertDialog = builder.create()
        dialog.show()
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

    private fun startCameraBackgroundThread() {
        cameraBackgroundThread = HandlerThread("CameraBackground").apply { start() }
        cameraBackgroundHandler = Handler(cameraBackgroundThread.looper)
    }
    private fun stopCameraBackgroundThread() {
        cameraBackgroundThread.quitSafely()
        try {
            cameraBackgroundThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun shouldShowRequestPermissionRationale(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity() as MainActivity, permission)) {
                return true
            }
        }
        return false
    }

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
            CalibrateFragment.ErrorDialog.newInstance("Permission request")
                .show(childFragmentManager, FRAGMENT_DIALOG)
        }
    }

    private fun allPermissionsGranted() = VIDEO_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

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
            val sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(
                SurfaceTexture::class.java)
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
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
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
                    CalibrateFragment.ErrorDialog.newInstance("Camera error")
                        .show(childFragmentManager, FRAGMENT_DIALOG)
//            this@CalibrateFragment.activity?.finish()
                }
                                                                                     }, null)
            mImageReader = ImageReader.newInstance(
                mPreviewSize.width,
                mPreviewSize.height,
                ImageFormat.YUV_420_888,
                2
            )
            mImageReader!!.setOnImageAvailableListener(
                ImageReader.OnImageAvailableListener { reader ->
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
                },
                cameraBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show()
            activity?.finish()
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            CalibrateFragment.ErrorDialog.newInstance("Camera error")
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

        private const val TAG = "ExerciseFragment"
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

    private fun startTimer() {
        isTimerRunning = true
        timerJob = timerScope.launch {
            while (isActive) {
                delay(1000)
                if (isTimerRunning) {
                    elapsedSeconds++
                    updateUIWithElapsedTime(elapsedSeconds)
                }
            }
        }
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val sec = seconds % 60
        return String.format("%02d:%02d", minutes, sec)
    }

    private fun pauseTimer() {
        isTimerRunning = false
    }

    private fun resumeTimer() {
        if (!isTimerRunning && (timerJob == null || timerJob!!.isCompleted))
            startTimer()
        else
            isTimerRunning = true
    }

    private fun updateUIWithElapsedTime(seconds: Int) {
        val progress = (seconds * 100 / 60) % 100
        binding.progressBarTimer.progress = progress
        binding.timerTextView.text = formatTime(seconds)
    }

    private fun setupFontSlider() {
        binding.exerciseText.textSize = 20f

        binding.fontSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fontSize = 20 + progress
                binding.exerciseText.textSize = fontSize.toFloat()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupPlayPause() {
        binding.exercisePlayPause.setOnClickListener {
            if (hasReachedEnd)
                return@setOnClickListener
            if (isPlaying) {
                //Pause Exercise
                binding.exercisePlayPause.setImageResource(R.drawable.ic_play)
                handler.removeCallbacksAndMessages(null)
                isPlaying = false
                pauseTimer()
                //Track completion
                updateCompletion()

            } else {
                //Resume Exercise
                startReadingMode()
                resumeTimer()
            }
        }
    }

    private fun updateCompletion() {
        val completionObject = Completion(
            completion = binding.exerciseText.getCompletion(),
            position = binding.exerciseText.currentIndex,
            time_spent = elapsedSeconds,
            user_id = preferences.userId.toInt(),
        )
        Timber.d("test123: $completionObject")
        viewModel.updateCompletion(
            exerciseId = thisExercise.id,
            completion = completionObject
        )
    }

    private fun setupSpeedButtons() {
        binding.speedMinus.setOnClickListener {
            if (delayMillis < 3000) {
                delayMillis += 100
                if (isPlaying) resetCallback()
            } else
                Toast.makeText(context, "Minimum speed reached (3 seconds)", Toast.LENGTH_SHORT)
                    .show()

            // Enable speedPlus if it was disabled
            binding.speedPlus.isEnabled = true
            // Disable speedMinus if the lower limit is reached
            if (delayMillis >= 3000) binding.speedMinus.isEnabled = false
        }
        binding.speedPlus.setOnClickListener {
            if (delayMillis > 200) {
                delayMillis -= 100
                if (isPlaying) resetCallback()
            } else
                Toast.makeText(context, "Maximum speed reached (0.2 seconds)", Toast.LENGTH_SHORT)
                    .show()

            // Enable speedMinus if it was disabled
            binding.speedMinus.isEnabled = true
            // Disable speedPlus if the upper limit is reached
            if (delayMillis <= 200) binding.speedPlus.isEnabled = false
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.eventFlow.collectLatest {
                        when (it) {
                            UiStates.LOADING -> toggleProgressBar(true)
                            UiStates.SUCCESS -> toggleProgressBar(false)
                            UiStates.UPDATE -> {
                                //User Exercise completion updated.
                                Timber.d("Exercise ${thisExercise.title} completion updated.")
                            }

                            UiStates.NO_INTERNET_CONNECTION -> {
                                binding.loading.visibility = View.GONE
                                Toast.makeText(
                                    binding.root.context,
                                    getString(R.string.network_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            UiStates.INVALID_TOKEN -> {
                                Common.showToast(binding.root.context, getString(R.string.token_error))
                                tokenManager.deleteToken()
                                preferences.isLoggedIn = false
                                findNavController().navigate(
                                    ExerciseFragmentDirections.actionExerciseFragmentToMainFragment()
                                )
                            }

                            else -> {
                                binding.loading.visibility = View.GONE
                                Toast.makeText(
                                    binding.root.context,
                                    getString(R.string.default_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.exerciseDataFlow.collectLatest {
                        thisExercise = it
                        binding.exerciseText.text = it.text
//                        startReadingMode()
//                        startTimer()
                    }
                }
            }
        }
    }

    private fun startReadingMode() {
        binding.exercisePlayPause.setImageResource(R.drawable.ic_pause)
        isPlaying = true
        handler.post(object : Runnable {
            override fun run() {
                if (!isPlaying) {
                    handler.removeCallbacksAndMessages(null)
                    return
                }
                binding.exerciseText.highlightNextWord()
                handler.postDelayed(this, delayMillis)
            }
        })
    }

    private fun resetCallback() {
        // Remove any existing callbacks in the handler
        handler.removeCallbacksAndMessages(null)
        // Start the highlighting loop with the adjusted speed
        binding.exerciseText.resetCallback()
        startReadingMode()
    }

    private fun toggleProgressBar(isLoading: Boolean) {
        if (isLoading) {
            binding.loading.visibility = View.VISIBLE
            binding.exerciseText.visibility = View.GONE
        } else {
            binding.loading.visibility = View.GONE
            binding.exerciseText.visibility = View.VISIBLE
        }
    }

    override fun onHighlightEnd() {
        hasReachedEnd = true
        binding.exercisePlayPause.setImageResource(R.drawable.ic_play)
        handler.removeCallbacksAndMessages(null)
        isPlaying = false
        //Cancel timer
        timerJob?.cancel()
        Toast.makeText(
            binding.root.context,
            "Exercise finished in $elapsedSeconds seconds",
            Toast.LENGTH_SHORT
        ).show()
        //Track completion
        updateCompletion()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerScope.cancel()
    }
}

