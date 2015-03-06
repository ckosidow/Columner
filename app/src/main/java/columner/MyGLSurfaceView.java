package columner;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

/**
 * Created by CKosidowski11 on 2/16/2015.
 */
class MyGLSurfaceView extends GLSurfaceView {
    private MyGLRenderer mRenderer;
    private Context context;
    private DisplayMetrics displayMetrics;
    private int width;
    private float x;

    public MyGLSurfaceView(Context c) {
        super(c);

        context = c;

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        mRenderer = new MyGLRenderer(context);

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if ((e.getEventTime() - e.getDownTime()) < 1) {
            displayMetrics = context.getResources().getDisplayMetrics();
            width = displayMetrics.widthPixels;

            x = e.getX();

            if (!mRenderer.falling) {
                if (x <= (width / 3))
                    mRenderer.nextCol = 0;
                else if (x > width / 3 && x < (width * 2 / 3))
                    mRenderer.nextCol = 1;
                else if (x > (width * 2 / 3))
                    mRenderer.nextCol = 2;
            }

            if (!mRenderer.started)
                mRenderer.started = true;

            if (!mRenderer.jump && !mRenderer.falling)
                mRenderer.jump = true;

            requestRender();
        }

        return true;
    }
}