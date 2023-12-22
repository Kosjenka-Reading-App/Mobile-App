package com.dsd.kosjenka.presentation.home.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.dsd.kosjenka.utils.GLCircleSprite;
import com.dsd.kosjenka.utils.GLTriangle;

import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

@SuppressLint("ViewConstructor")
public class TrackerView extends GLSurfaceView {

    private TrackerRenderer trackerRenderer;
    private final VisageWrapper visageWrapper;
    private final Random rand = new Random();

    public TrackerView(Context context, VisageWrapper wrapper) {
        super(context);

        // Create an OpenGL ES 2.0 context
//        setEGLContextClientVersion(2);

        trackerRenderer = new TrackerRenderer(context, this);
        visageWrapper = wrapper;


        setEGLConfigChooser(8,8,8,8,16,0);
        getHolder().setFormat(PixelFormat.TRANSPARENT);
        setRenderer(trackerRenderer);
        setDebugFlags(DEBUG_LOG_GL_CALLS);
        setDebugFlags(DEBUG_CHECK_GL_ERROR);

        setKeepScreenOn(true);
        setPreserveEGLContextOnPause(true);
    }

    public boolean onTouchEvent(final MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_UP) {
            visageWrapper.SendCoordinates(event.getX(), getHeight() - event.getY());
        }

//        int MAX_COUNT = 6;
//        if (CALIBRATION_MODE && CALIBRATION_COUNT <= MAX_COUNT) {

//            if (event.getX() > (trackerRenderer.mTriangle.getVertexBuffer().get()[3]) &&
//                    event.getX() < (trackerRenderer.mTriangle.getTriangleCoords()[6]) &&
//                    (getHeight() - event.getY()) > (trackerRenderer.mTriangle.getTriangleCoords()[4]) &&
//                    (getHeight() - event.getY()) < (trackerRenderer.mTriangle.getTriangleCoords()[2])){
//
//                // the target was tapped, move it by new offset
//                if (CALIBRATION_COUNT < MAX_COUNT){
//                    trackerRenderer.setCordOffset(generateNewOffsetCords());
//                    requestRender();
//                } else if (CALIBRATION_COUNT == MAX_COUNT) {
//                    CALIBRATION_MODE = false;
//                }
//                CALIBRATION_COUNT++;
//
//            }
//        }

        return true;
    }

    private float[] generateNewOffsetCords() {
        float x = rand.nextFloat() * getWidth();
        float y = rand.nextFloat() * getHeight();
        float z = 0.0f;

        return new float[] {x, y, z};
    }

    public class TrackerRenderer implements Renderer {
        float mRed;
        float mGreen;
        float mBlue;

        public int width;
        public int height;
        TrackerView trackerView;
        Context context;

        private GLCircleSprite mCircle;

        private final float[] vPMatrix = new float[16];
        private final float[] projectionMatrix = new float[16];
        private final float[] viewMatrix = new float[16];

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

//            mCircle = new GLCircleSprite(new float[]{ 0.63671875f, 0.76953125f, 0.22265625f, 1.0f});
//            mCircle.setRadius(10.0f);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            gl.glViewport(0, 0, width, height);
//            GLES20.glViewport(0, 0, width, height);
            this.width = width;
            this.height = height;
            Log.d(TAG, "onSurfaceChanged");

            visageWrapper.ResetTextures();

            //setting projection matrix for the calibration mode
//            float ratio = (float) width / height;
//            Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            gl.glEnable(GL10.GL_BLEND);
//            gl.glClearColor(mRed, mGreen, mBlue, 1.0f);
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
//            GLES20.glEnable(GLES20.GL_BLEND);
//            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            visageWrapper.DisplayTrackingStatus(width, height);

            // Calibration MODE
//            if (trackerView.CALIBRATION_MODE){
                // Set the camera position (View matrix)
//                Matrix.setLookAtM(viewMatrix, 0, 0, 0, 3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
//                // Calculate the projection and view transformation
//                Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
//                Matrix.translateM(vPMatrix, 0, triangleCordsOffset[0],triangleCordsOffset[1],triangleCordsOffset[2]);

//                mCircle.draw();
//            }
        }

        void setColor(float v, float v1, float v2) {
            mRed = v;
            mGreen = v1;
            mBlue = v2;
        }
    }
}
