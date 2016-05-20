package squareUp

import android.app.Activity
import android.os.Bundle

class MainMenuOGLESActivity:Activity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)

        this.setContentView(MyGLSurfaceView(this))
    }
}