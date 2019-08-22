package com.displaymodule.glassdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.displaymodule.glasssensor.sensors.USBSensorCallback;

public class MainActivity extends AppCompatActivity {
    private final String TAG = getClass().getName();
    USBSensorCallback mDisplay;
//    private USBDisplay mDisplay;
    static int mRotation = 0;
    static int mOnOff = 1;
    SeekBar luminanceBar;
    TextView seekBarText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDisplay = new USBSensorCallback();
        mDisplay.register(getApplicationContext(), (accSensorData, gyroSensorData, magSensorData) -> {});
        mDisplay.start();
//        mDisplay = new USBDisplay(this);
        seekBarText = findViewById(R.id.seekBarText2);
        luminanceBar = findViewById(R.id.lum);
        luminanceBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Log.d(TAG, "onProgressChanged: " + i);
                int brightness = i;
                seekBarText.setText("lu:" + i);

                if (brightness <= 0) {
                    brightness = 1;
                }

                if (brightness > 255) {
                    brightness = 255;
                }

                mDisplay.setInstantLuminance((byte) brightness);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDisplay.stop();
    }

    public void onTurnOnOffButtonClick(View view) {
         if (mOnOff == 1) {
             mDisplay.turnOffGlassScreen();
             mOnOff = 0;
         } else {
             mDisplay.turnOnGlassScreen();
             mOnOff = 1;
         }
    }

    public void onRotationButtonClick(View view) {
        mRotation = (mRotation+1)%4;
        mDisplay.setRotation((byte) mRotation);
    }

    public void onLumButtonClick(View view) {
        byte level = (byte) 0;

        switch (view.getId()) {
            case R.id.lum120:
                level = (byte) 1;
                break;
            case R.id.lum300:
                level = (byte) 2;
                break;
            case R.id.lum500:
                level = (byte) 0;
                break;
            case R.id.lum1500:
                level = (byte) 3;
                break;
            case R.id.lum3000:
                level = (byte) 4;
                break;
        }
        mDisplay.setPresetBrightness(level);
    }

    public void onCubeDemoButtonClick(View view) {
        startActivity(new Intent(this, CubeActivity.class));
    }
}
