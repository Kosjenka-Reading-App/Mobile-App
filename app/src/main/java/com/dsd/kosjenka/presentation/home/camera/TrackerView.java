package com.dsd.kosjenka.presentation.home.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

@SuppressLint("ViewConstructor")
public class TrackerView extends GLSurfaceView {

    private TrackerRenderer trackerRenderer;
    private VisageWrapper visageWrapper;

    public TrackerView(Context context, VisageWrapper wrapper) {
        super(context);
        trackerRenderer = new TrackerRenderer(context, this);
        visageWrapper = wrapper;


        setEGLConfigChooser(8,8,8,8,16,0);
        getHolder().setFormat(PixelFormat.TRANSPARENT);
        setRenderer(trackerRenderer);
//        setDebugFlags(DEBUG_LOG_GL_CALLS);

        setKeepScreenOn(true);
        setPreserveEGLContextOnPause(true);
    }

    public boolean onTouchEvent(final MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP) {
            visageWrapper.SendCoordinates(event.getX(), getHeight() - event.getY());
        }

        return true;
    }

    public class TrackerRenderer implements Renderer {
        float mRed;
        float mGreen;
        float mBlue;

        public int width;
        public int height;
        TrackerView trackerView;
        Context context;

        private static final String TAG = "TrackRenderer";

        TrackerRenderer(Context ctx, TrackerView trView){
            context = ctx;
            trackerView = trView;
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config)
        {
            Log.d(TAG, "onSurfaceCreated");
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            gl.glViewport(0, 0, width, height);
            this.width = width;
            this.height = height;
            Log.d(TAG, "onSurfaceChanged");

            visageWrapper.ResetTextures();
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            gl.glEnable(GL10.GL_BLEND);
//            gl.glClearColor(mRed, mGreen, mBlue, 1.0f);
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

            visageWrapper.DisplayTrackingStatus(width, height);
        }

        void setColor(float v, float v1, float v2) {
            mRed = v;
            mGreen = v1;
            mBlue = v2;
        }
    }
}
