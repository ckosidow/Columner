package columner

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val mRenderer: MyGLRenderer

    init {
        // Create an OpenGL ES 2.0 context
        this.setEGLContextClientVersion(2)
        mRenderer = MyGLRenderer(context)
        // Set the Renderer for drawing on the GLSurfaceView
        this.setRenderer(mRenderer)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.getEventTime() - event.getDownTime() < 1) {
            val width = context.getResources().getDisplayMetrics().widthPixels
            val x = event.getX()

            if (!mRenderer.falling) {
                if (x <= width / 3) {
                    mRenderer.nextCol = 0
                } else if (x > width / 3 && x < (width shl 1) / 3) {
                    mRenderer.nextCol = 1
                } else if (x > (width shl 1) / 3) {
                    mRenderer.nextCol = 2
                }
            }

            if (!mRenderer.started) {
                mRenderer.started = true
            }

            if (!mRenderer.jump && !mRenderer.falling) {
                mRenderer.jump = true
            }

            this.requestRender()
        }

        return true
    }
}