package com.dsd.kosjenka.presentation.home.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.dsd.kosjenka.R
import com.dsd.kosjenka.databinding.ActivityCameraBinding
import com.dsd.kosjenka.utils.Common
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding

    private val cameraManager: CameraManager by lazy {
        applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /** Internal reference to the ongoing [CameraCaptureSession] configured with our parameters */
    private lateinit var session: CameraCaptureSession

    /** The [CameraDevice] that will be opened in this fragment */
    private lateinit var camera: CameraDevice

    /** [HandlerThread] where all camera operations run */
    private val cameraThread = HandlerThread("CameraThread").apply { start() }

    /** [Handler] corresponding to [cameraThread] */
    val cameraHandler = Handler(cameraThread.looper)

    /** Readers used as buffers for camera still shots */
    private lateinit var imageReader: ImageReader

    /** [HandlerThread] where all buffer reading operations run */
    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }

    /** [Handler] corresponding to [imageReaderThread] */
    private val imageReaderHandler = Handler(imageReaderThread.looper)

    /** [CameraCharacteristics] corresponding to the provided Camera ID */
    private lateinit var characteristics: CameraCharacteristics
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera)

        setSupportActionBar(binding.cameraToolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                if (allPermissionsGranted()) {
                    initializeCamera()
                } else {
                    requestPermissions()
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                val state = GetStatus()
                val time = GetTrackTime()
                binding.statusTextview.text = "state: ($state) time: ($time)"
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        //this.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::camera.isInitialized){
            camera.close()
        }
//        TrackerStop()
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            this, it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in REQUIRED_PERMISSIONS && !it.value)
                permissionGranted = false
        }
        if (!permissionGranted){
            Common.showToast(this, "Permission Denied")
        }else {
            initializeCamera()
        }
    }

    private fun initializeCamera() = lifecycleScope.launch(Dispatchers.Main){
        TrackerInit(applicationContext.filesDir.absolutePath, "Facial Features Tracker.cfg")

        // Open the selected camera
        camera = openCamera(cameraManager, getCameraId(), cameraHandler)

        characteristics = cameraManager.getCameraCharacteristics(camera.id)

        // Initialize an image reader which will be used to capture still photos
        val size = characteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            .getOutputSizes(ImageFormat.YUV_420_888).maxByOrNull { it.height * it.width }!!
        imageReader = ImageReader.newInstance(
            size.width, size.height, ImageFormat.YUV_420_888, IMAGE_BUFFER_SIZE
        )

        val screenOrientation = windowManager.defaultDisplay.rotation
        val orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)

        imageReaderHandler.post {
            setParameters(size.width, size.height, (screenOrientation*90 + orientation!!)%360, 1)
            TrackFromCam()
        }

        val capReq = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(Surface(binding.textureView.surfaceTexture))
        }

        session = createCaptureSession(camera, listOf(Surface(binding.textureView.surfaceTexture), imageReader.surface), cameraHandler)
        session.setRepeatingRequest(capReq.build(), null, cameraHandler)

        imageReader.setOnImageAvailableListener(
            { reader ->
                val buffer = reader.acquireLatestImage().planes[0].buffer
                val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
//                WriteFrameCamera(bytes)
            }, imageReaderHandler)
    }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
        manager: CameraManager,
        cameraId: String,
        handler: Handler? = null
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) = cont.resume(device)

            override fun onDisconnected(device: CameraDevice) {
                Log.w(TAG, "Camera $cameraId has been disconnected")
                finish()
            }

            override fun onError(device: CameraDevice, error: Int) {
                val msg = when (error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
                    ERROR_CAMERA_DISABLED -> "Device policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                Log.e(TAG, exc.message, exc)
                if (cont.isActive) cont.resumeWithException(exc)
            }
        }, handler)
    }

    /**
     * Starts a [CameraCaptureSession] and returns the configured session (as the result of the
     * suspend coroutine
     */
    private suspend fun createCaptureSession(
        device: CameraDevice,
        targets: List<Surface>,
        handler: Handler? = null
    ): CameraCaptureSession = suspendCoroutine { cont ->

        // Create a capture session using the predefined targets; this also involves defining the
        // session state callback to be notified of when the session is ready
        device.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {

            override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)

            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session configuration failed")
                Log.e(TAG, exc.message, exc)
                cont.resumeWithException(exc)
            }
        }, handler)
    }

    private fun getCameraId() : String {
        val cameraId = "-1"
        val cameraIds = cameraManager.cameraIdList.filter {
            val characteristics = cameraManager.getCameraCharacteristics(it)
            val lensFacing = characteristics.get(
                CameraCharacteristics.LENS_FACING)
            lensFacing == CameraMetadata.LENS_FACING_FRONT
        }
        return cameraIds[0] ?: cameraId
    }

    fun AlertDialogFunction(message: String){
        val builder = AlertDialog.Builder(this)

        with(builder)
        {
            setTitle("License warning")
            setMessage(message)
            setPositiveButton("OK", null)
            show()
        }
    }

    companion object {
        private val TAG = CameraActivity::class.java.simpleName

        /** Maximum number of images that will be held in the reader's buffer */
        private const val IMAGE_BUFFER_SIZE: Int = 3

        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    /** Interface to native method called for initializing tracker.
     *
     * @param configFilename absolute path to tracker configuration file.
     */
    external fun TrackerInit(pathToAssets: String?, configFilename: String?)

    external fun TrackerStop()
    external fun TrackFromCam()

    external fun setParameters(
        width: Int,
        height: Int,
        orientation: Int,
        flip: Int
    )

    /** Interface to native method called to obtain frame rate information from tracker.
     *
     * @return frame rate value
     */
    external fun GetFps(): Float

    /** Interface to native method called to obtain rendering frame rate information.
     *
     * @return rendering frame rate value
     */
    external fun GetDisplayFps(): Float

    /** Interface to native method called to obtain status of tracking information from tracker.
     *
     * @return status of tracking information as text.
     */
    external fun GetStatus(): String?

    /** Interface to native method called to obtain pure tracking time.
     *
     * @return status of tracking information as text.
     */
    external fun GetTrackTime(): Int

    /** Interface to native method used for passing raw pixel data to tracker.
     * This method is called to write camera frames into VisageSDK::VisageTracker object through VisageSDK::AndroidCameraCapture
     *
     * @param frame raw pixel data of image used for tracking.
     */
    external fun WriteFrameCamera(frame: ByteArray?)

    init {
        System.loadLibrary("VisageVision")
        System.loadLibrary("VisageWrapper")
    }
}