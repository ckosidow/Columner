package columner;

import android.app.Activity;
import android.os.Bundle;

public class MainMenuOGLESActivity extends Activity {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(new MyGLSurfaceView(this));
    }
}