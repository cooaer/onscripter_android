package jp.ogapee.onscripter;

import android.app.Activity;

/**
 * Created by cooaer on 2017/6/29 22:39.
 */

public class DemoGLSurfaceView extends GLSurfaceView_SDL {
    public DemoGLSurfaceView(Activity context) {
        super(context);
        mRenderer = new DemoRenderer(context);
        setRenderer(mRenderer);
    }
    
    public void exitApp() {
        mRenderer.exitApp();
    };
    
    @Override
    public void onPause() {
        super.onPause();
        surfaceDestroyed(this.getHolder());
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }
    
    @Override
    public void onStop()
    {
        super.onStop();
    }
    
    DemoRenderer mRenderer;
}
