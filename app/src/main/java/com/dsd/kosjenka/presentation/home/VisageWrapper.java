package com.dsd.kosjenka.presentation.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.View;

import com.dsd.kosjenka.presentation.home.camera.GazeCalibrationView;
import com.dsd.kosjenka.presentation.home.camera.TrackerView;

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

    public enum TrackScreen {
        CAMERA,
        CALIBRATION,
        EXERCISE
    }

    private static volatile VisageWrapper instance = null;
    private Context ctx;
    private static final String TAG = "VisageWrapper";

    private VisageWorkerThread visageWorkerThread = null;
    private Handler visageWorkerHandler = null;

    private String path;
    TrackScreen trackScreen;
    private boolean trackerStarted = false;

    private TrackerView trackerView = null;
    private GazeCalibrationView calibrateView = null;
    private GazeCalibrationView readingModeGazeView = null;

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

        trackScreen = TrackScreen.CAMERA;
        trackerView = new TrackerView(context, this);

        calibrateView = new GazeCalibrationView(context, this);
        readingModeGazeView = new GazeCalibrationView(context, this);
        SetDisplayOptions(DISPLAY_DEFAULT);

//        logo = ResourcesCompat.getDrawable(context.getResources(), R.drawable.logo, null);
    }


    public void onCreate() {
        if (visageWorkerThread == null) {
            startVisageWorker();
        }

        visageWorkerHandler.sendMessage(visageWorkerHandler.obtainMessage(VisageWorkerThread.MSG_INIT));

    }

    public void onResume() {
        switch (trackScreen){
            case CAMERA -> trackerView.onResume();
            case CALIBRATION -> calibrateView.onResume();
            case EXERCISE -> readingModeGazeView.onResume();
        }

        Log.d(TAG, "backgroundHandler");
        visageWorkerHandler.sendMessage(visageWorkerHandler.obtainMessage(VisageWorkerThread.MSG_START_TRACKER));
    }

    public void onPause() {
        switch (trackScreen){
            case CAMERA -> trackerView.onPause();
            case CALIBRATION -> calibrateView.onPause();
            case EXERCISE -> readingModeGazeView.onPause();
        }
        PauseTracker();
    }

    public void onDestroy() {
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
                    case VisageWorkerThread.MSG_START_TRACKER -> visageWorkerThread.startTracker();
                    case VisageWorkerThread.MSG_INIT -> visageWorkerThread.initialize();
                    case VisageWorkerThread.MSG_RELEASE -> visageWorkerThread.release();
                    case VisageWorkerThread.MSG_INIT_GAZE -> visageWorkerThread.initializeGaze();
                    case VisageWorkerThread.MSG_FINALIZE_CALIBRATION ->
                            visageWorkerThread.finalizeGazeCalibration();
                    default -> throw new RuntimeException("Not known msg.what");
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

    public void switchToCameraScreen() {
        PauseTracker();
        trackScreen = TrackScreen.CAMERA;
    }

    public void switchToCalibrateScreen() {
        PauseTracker();
        trackScreen = TrackScreen.CALIBRATION;
    }

    public void switchToExerciseScreen() {
        PauseTracker();
        trackScreen = TrackScreen.EXERCISE;
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
    void initAnalyser() { InitAnalyser();}

    public void initGaze() { visageWorkerHandler.sendMessage(visageWorkerHandler.obtainMessage(VisageWorkerThread.MSG_INIT_GAZE)); }

    boolean isTrackingCameraScreen() {
        return trackScreen == TrackScreen.CAMERA;
    }

    public View getTrackerView() {
        return trackerView;
    }

    public View getCalibrateView() { return calibrateView; }

    public View getReadingModeGazeView() { return readingModeGazeView; }

    public void finalizeGazeCalibration() { visageWorkerHandler.sendMessage(visageWorkerHandler.obtainMessage(VisageWorkerThread.MSG_FINALIZE_CALIBRATION)); }


    private class VisageWorkerThread extends HandlerThread {

        private static final int MSG_START_TRACKER = 1;
        private static final int MSG_INIT = 2;
        private static final int MSG_RELEASE = 3;
        private static final int MSG_INIT_GAZE = 4;
        private static final int MSG_FINALIZE_CALIBRATION = 5;
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

        void initializeGaze() {
            InitOnlineGazeCalibration();
        }

        void finalizeGazeCalibration(){
            FinalizeOnlineGazeCalibration();
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

    public native void InitOnlineGazeCalibration();

    public native void AddGazeCalibrationPoint(float x, float y);

    public native void FinalizeOnlineGazeCalibration();

    public native ScreenSpaceGazeData GetScreenSpaceGazeData();

    public static class ScreenSpaceGazeData {
        public int index;
        public float x;
        public float y;
        public int inState;
        public float quality;

        public ScreenSpaceGazeData(int index, float x, float y, int inState, float quality) {
            this.index = index;
            this.x = x;
            this.y = y;
            this.inState = inState;
            this.quality = quality;
        }
    }
}
