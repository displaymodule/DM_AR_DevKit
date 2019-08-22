package com.displaymodule.glassdemo;

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;

import com.displaymodule.glasssensor.sensors.USBSensorCallback;


public class CubeActivity extends Activity {

    /**
     * The surface that will be drawn upon
     */
    private GLSurfaceView mGLSurfaceView;
    /**
     * The class that renders the cube
     */
    private CubeRenderer mRenderer;
    /**
     * The current orientation provider that delivers device orientation.
     */
    private OrientationProvider currentOrientationProvider;

    USBSensorCallback mGlassSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGLSurfaceView = new GLSurfaceView(this);
        // Create our Preview view and set it as the content of our Activity
        setContentView(mGLSurfaceView);

        mGlassSensor = new USBSensorCallback();

        //Accelerometer and Compass fusion somethings has sudden back issue
        //currentOrientationProvider = new AccelerometerCompassProvider(getApplicationContext(), mGlassSensor);
        currentOrientationProvider = new CalibratedGyroscopeProvider(getApplicationContext(), mGlassSensor);
        mRenderer = new CubeRenderer();
        mRenderer.setOrientationProvider(currentOrientationProvider);

        mGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGLSurfaceView.setRenderer(mRenderer);

        mGLSurfaceView.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                mRenderer.toggleShowCubeInsideOut();
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onResume();
        currentOrientationProvider.start();
        mGLSurfaceView.onResume();
    }

    @Override
    public void onPause() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onPause();
        currentOrientationProvider.stop();
        mGLSurfaceView.onPause();
    }

}
