package columner

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val mRenderer: MyGLRenderer
    private val surfaceWidth = context.getResources().getDisplayMetrics().widthPixels

    init {
        this.setEGLContextClientVersion(2)
        mRenderer = MyGLRenderer(context)

        this.setRenderer(mRenderer)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.getEventTime() - event.getDownTime() < 1) {
            val x = event.getX() * 3

            if (!mRenderer.falling) {
                if (x <= surfaceWidth) {
                    mRenderer.nextCol = 0
                } else if (x > surfaceWidth && x < surfaceWidth shl 1) {
                    mRenderer.nextCol = 1
                } else if (x > surfaceWidth shl 1) {
                    mRenderer.nextCol = 2
                }
            }

            mRenderer.started = true
            mRenderer.jump = !mRenderer.jump && !mRenderer.falling;

            this.requestRender()
        }

        return true
    }
}