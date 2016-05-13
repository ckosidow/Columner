package columner;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

public class MyGLSurfaceView extends GLSurfaceView {
    private MyGLRenderer mRenderer;
    private Context context;
    private DisplayMetrics displayMetrics;
    private int width;
    private float x;

    public MyGLSurfaceView(final Context context) {
        super(context);

        this.context = context;

        // Create an OpenGL ES 2.0 context
        this.setEGLContextClientVersion(2);

        mRenderer = new MyGLRenderer(this.context);

        // Set the Renderer for drawing on the GLSurfaceView
        this.setRenderer(mRenderer);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (event.getEventTime() - event.getDownTime() < 1) {
            displayMetrics = context.getResources().getDisplayMetrics();
            width = displayMetrics.widthPixels;

            x = event.getX();

            if (!mRenderer.falling) {
                if (x <= width / 3) {
                    mRenderer.nextCol = 0;
                } else if (x > width / 3 && x < (width << 1) / 3) {
                    mRenderer.nextCol = 1;
                } else if (x > (width << 1) / 3) {
                    mRenderer.nextCol = 2;
                }
            }

            if (!mRenderer.started) {
                mRenderer.started = true;
            }

            if (!mRenderer.jump && !mRenderer.falling) {
                mRenderer.jump = true;
            }

            this.requestRender();
        }

        return true;
    }
}