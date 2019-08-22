package com.displaymodule.glasssensor.sensors;

public interface USBSensorEventListener {

    void onSensorChanged(float[] accSensorData, float[] gyroSensorData, float[] magSensorData);
}