package com.dsd.kosjenka.presentation.home.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.View;

import java.nio.ByteBuffer;

public class VisageWrapper {

    static final int DISPLAY_FEATURE_POINTS = 1;
    static final int DISPLAY_SPLINES = 2;
    static final int DISPLAY_GAZE = 4;
    static final int DISPLAY_IRIS = 8;
    static final int DISPLAY_AXES = 16;
    static final int DISPLAY_FRAME = 32;
    static final int DISPLAY_WIRE_FRAME = 64;
    static final int DISPLAY_TRACKING_QUALITY = 128;

    static final int DISPLAY_POINT_QUALITY = 256;

    static final int DISPLAY_AGE = 512;
    static final int DISPLAY_GENDER = 1024;
    static final int DISPLAY_EMOTIONS = 2048;
    static final int DISPLAY_DEFAULT = DISPLAY_FEATURE_POINTS + DISPLAY_SPLINES + DISPLAY_GAZE + DISPLAY_IRIS + DISPLAY_AXES + DISPLAY_TRACKING_QUALITY + DISPLAY_POINT_QUALITY;
    public static final int NUM_EMOTIONS = 6; //should be 6 because neutral emotion won't be displayed

    public enum TrackMode {
        CAMERA,
        CALIBRATION
    };

    private static volatile VisageWrapper instance = null;
    private Context ctx;
    private static final String TAG = "VisageWrapper";

    private VisageWorkerThread visageWorkerThread = null;
    private Handler visageWorkerHandler = null;

    private String path;
    TrackMode trackMode;
    private boolean trackerStarted = false;

    private TrackerView trackerView = null;
    private GazeCalibrationView gazeCalibrationView = null;

//    private Drawable logo;

    public static VisageWrapper get(Context context) {
        if (instance == null) {
            synchronized (VisageWrapper.class) {
                if (instance == null) {
                    instance = new VisageWrapper(context);
                }
            }
        }
        return instance;
    }

    private VisageWrapper(Context context) {
        ctx = context;
        path = ctx.getFilesDir().getAbsolutePath();

        trackMode = TrackMode.CAMERA;
        trackerView = new TrackerView(context, this);
        gazeCalibrationView = new GazeCalibrationView(context, this);
        SetDisplayOptions(DISPLAY_DEFAULT);


//        logo = ResourcesCompat.getDrawable(context.getResources(), R.drawable.logo, null);
    }


    void onCreate() {
        if (visageWorkerThread == null) {
            startVisageWorker();
        }

        visageWorkerHandler.sendMessage(visageWorkerHandler.obtainMessage(VisageWorkerThread.MSG_INIT));
    }

    void onResume() {

        if (trackMode == TrackMode.CAMERA)
            trackerView.onResume();
        else
            gazeCalibrationView.onResume();

        Log.d(TAG, "backgroundHandler");
        visageWorkerHandler.sendMessage(visageWorkerHandler.obtainMessage(VisageWorkerThread.MSG_START_TRACKER));
    }

    void onPause() {
        if (trackMode == TrackMode.CAMERA)
            trackerView.onPause();
        else
            gazeCalibrationView.onPause();

        PauseTracker();
    }

    void onDestroy() {
        TrackerStop();
        if (visageWorkerThread == null)
            return;

        stopVisageWorker();

    }

    private void startVisageWorker() {
        visageWorkerThread = new VisageWorkerThread();
        visageWorkerThread.start();

        visageWorkerHandler = new Handler(visageWorkerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case VisageWorkerThread.MSG_START_TRACKER:
                        visageWorkerThread.startTracker();
                        break;
                    case VisageWorkerThread.MSG_INIT:
                        visageWorkerThread.initialize();
                        break;
                    case VisageWorkerThread.MSG_RELEASE:
                        visageWorkerThread.release();
                        break;
                    default:
                        throw new RuntimeException("Not known msg.what");
                }
            }
        };
    }

    private void stopVisageWorker() {
        visageWorkerThread.quitSafely();
        try {
            visageWorkerThread.join();
            visageWorkerThread = null;
            visageWorkerHandler = null;
        } catch (InterruptedException e) {
            Log.w(TAG, "Error stopping background thread", e);
        }
    }

    void switchToCamera() {
        PauseTracker();
        trackMode = TrackMode.CAMERA;
    }

    void switchToCalibration() {
        PauseTracker();
        trackMode = TrackMode.CALIBRATION;
    }

    void toggleEars() {
        ToggleEars(path);
    }

    void toggleIris() {
        ToggleIris();
    }
    void toggleRefineLandmarks(boolean enableOrDisable) {
        ToggleRefineLandmarks(enableOrDisable);
    }
    void initAnalyser() { InitAnalyser();};

    boolean isTrackingFromCamera() {
        return trackMode == TrackMode.CAMERA;
    }

    View getTrackerView() {
        return trackerView;
    }

    View getGazeCalibrationView() { return gazeCalibrationView; }

    private class VisageWorkerThread extends HandlerThread {

        private static final int MSG_START_TRACKER = 1;
        private static final int MSG_INIT = 2;
        private static final int MSG_RELEASE = 3;
        private Bitmap bitmapLogo;

        private VisageWorkerThread() {
            super("VisageWorkerThread", Process.THREAD_PRIORITY_DISPLAY);

//            bitmapLogo = ((BitmapDrawable) logo).getBitmap();
        }

        void initialize() {
            TrackerInit(path, "Facial Features Tracker.cfg");

            //WriteLogoImage(MediaLoader.ImageLoader.ConvertToByte(bitmapLogo, true), bitmapLogo.getWidth(), bitmapLogo.getHeight());
        }

        void startTracker() {
            if (trackerStarted)
                return;

            trackerStarted = true;

            TrackLoop();

            trackerStarted = false;
        }

        public void release() {
            TrackerStop();
            trackerStarted = false;
        }
    }

    public native void TrackerInit(String path, String configFilename);

    public static native void SetParameters(int width, int height, int orientation, int flip);

    public static native void SetParameters(int width, int height);

    public native void TrackerStop();

    public native void PauseTracker();

    public native void ResumeTracker();

    public native void SetDisplayOptions(int displayO);

    public native void ToggleEars(String path);

    public native void ToggleIris();

    public native void ToggleRefineLandmarks(boolean enableOrDisable);

    public native void TrackLoop();

    public static native void WriteFrameStream(ByteBuffer frameChannel0, ByteBuffer frameChannel1, ByteBuffer frameChannel2, long timestampA, long frameID, int pixelStride);

    public static native void WriteFrameImage(byte[] frame, int width, int height);

    public native boolean DisplayTrackingStatus(int width, int height);

    public static native float GetAge();

    public static native int GetGender();

    public static native float[] GetEmotions();

    public native void DeallocateResources();

    public native void AllocateResources();

    public native void ResetTextures();

    public native void SendCoordinates(float x, float y);

    public static native boolean ShowInstruction();

    public static native void WriteLogoImage(byte[] logo, int width, int height);

    public native void InitAnalyser();
}