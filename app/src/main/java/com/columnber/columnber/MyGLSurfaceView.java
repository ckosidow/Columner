package com.columnber.columnber;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

/**
 * Created by CKosidowski11 on 2/16/2015.
 */
class MyGLSurfaceView extends GLSurfaceView {
    private final MyGLRenderer mRenderer;
    private Context context;
    private final float TOUCH_SCALE_FACTOR = 180.0f / 1800;

    public MyGLSurfaceView(Context c) {
        super(c);

        context = c;

        // Render the view only when there is a change in the drawing data.
        // To allow the triangle to rotate automatically, this line is commented out:
        //setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        mRenderer = new MyGLRenderer();

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;

        float x = e.getX();

        if(x <= (width / 3))
            mRenderer.jumpCol = 0;
        else if (x > width / 3 && x < (width * 2 / 3))
            mRenderer.jumpCol = 1;
        else if (x > (width * 2 / 3))
            mRenderer.jumpCol = 2;

        if (!mRenderer.started)
            mRenderer.started = true;

        if (!mRenderer.jump && !mRenderer.inAir)
            mRenderer.jump = true;

        requestRender();

        return true;
    }
}