package com.displaymodule.glassdemo;


import android.content.Context;
import android.util.Log;

import com.displaymodule.glasssensor.sensors.USBSensorCallback;

public class AccelerometerCompassProvider extends OrientationProvider {

    /**
     * Compass values
     */
    final private float[] magnitudeValues = new float[3];

    /**
     * Accelerometer values
     */
    final private float[] accelerometerValues = new float[3];

    /**
     * Inclination values
     */
    final float[] inclinationValues = new float[16];

    /**
     * Initialises a new OrientationProvider
     *
     * @param context
     * @param glassSensor
     */
    public AccelerometerCompassProvider(Context context, USBSensorCallback glassSensor) {
        super(context, glassSensor);
    }



    @Override
    public void onSensorChanged(float[] accSensorData, float[] gyroSensorData, float[] magSensorData) {
//                    Log.e("Usb Alpha", "accSensorData = [" + accSensorData[0] + " , " + accSensorData[1] + " , " + accSensorData[2] + " ], "
//                    + " gyroSensorData = [" + gyroSensorData[0] + " , " + gyroSensorData[1] + " , " + gyroSensorData[2] + " ], "
//                    + " magSensorData = [" + magSensorData[0] + " , " + magSensorData[1] + " , " + magSensorData[2] + " ], ");
        System.arraycopy(accSensorData, 0, accelerometerValues, 0, accelerometerValues.length);
        System.arraycopy(magSensorData, 0, magnitudeValues, 0, magnitudeValues.length);

        if (magnitudeValues != null && accelerometerValues != null) {
            // Fuse accelerometer with compass
            getRotationMatrix(currentOrientationRotationMatrix.matrix, inclinationValues, accelerometerValues,
                    magnitudeValues);
            // Transform rotation matrix to quaternion
            currentOrientationQuaternion.setRowMajor(currentOrientationRotationMatrix.matrix);
        }
    }
}
