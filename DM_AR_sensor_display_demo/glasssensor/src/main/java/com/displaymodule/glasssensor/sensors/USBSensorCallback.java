package com.displaymodule.glasssensor.sensors;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.displaymodule.glasssensor.usbservice.IUsbCallback;
import com.displaymodule.glasssensor.usbservice.IUsbCallbackApi;

import java.io.UnsupportedEncodingException;

public class USBSensorCallback {

    private RemoteCallbackApiConn mCallbackApiConn=new RemoteCallbackApiConn();
    private final static String ACTION_CALLBACK="com.displaymodule.glasssenor.usbservice.callback";
    private IUsbCallbackApi mRemoteCallbackApi;
    private Context mContext = null;

    private USBSensorEventListener mListener;
    private long mTicks = 0;
    private long mStartTicks = 0;
    private long mTimeNanos = 0;

    private static String TAG = "USBSensorCallback";

    public void register(Context context, USBSensorEventListener listener) {
        mContext = context;
        mListener = listener;
    }

    public void unRegister() {

    }

    public long getTimeNanos(){
        //Log.d(TAG, "mTimeNanos = " + mTimeNanos + ", mTicks = " + mTicks * 100000);
        return mTimeNanos + (mTicks - mStartTicks) * 100000;
    }



    static float byte2float(byte[] b, int index) {
        return Float.intBitsToFloat(byte2int(b, index));
    }

    static int byte2int(byte[] b, int index) {
        int l;
        l = b[index];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);
        return l;
    }

    private IUsbCallback mIRemoteCallback=new IUsbCallback.Stub() {
        @Override
        public void sensorEvent(String data) {
            byte[] sensorData;
            try {
                sensorData = data.getBytes("ISO-8859-1");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return;
            }

            if (sensorData.length > 36) {
//                Log.e(TAG, "sensorEvent acc: " + byte2float(sensorData, 4) + " " + byte2float(sensorData, 8) + " " + byte2float(sensorData, 12));
//                Log.e(TAG, "gyro: " + byte2float(sensorData, 16) + " " + byte2float(sensorData, 20) + " " + byte2float(sensorData, 24));
//                Log.e(TAG, "6x: " + byte2float(sensorData, 28) + " " + byte2float(sensorData, 32) + " " + byte2float(sensorData, 36));
                float[] accSensorData = new float[3];
                float[] gyroSensorData = new float[3];
                float[] magSensorData = new float[3];

                // ACC
                accSensorData[0] = byte2float(sensorData, 4);
                accSensorData[1] = byte2float(sensorData, 8);
                accSensorData[2] = byte2float(sensorData, 12);

                // Gyro
                gyroSensorData[0] = byte2float(sensorData, 16) * (float) Math.PI / 180.0f;
                gyroSensorData[1] = byte2float(sensorData, 20) * (float) Math.PI / 180.0f;
                gyroSensorData[2] = byte2float(sensorData, 24) * (float) Math.PI / 180.0f;

                // Mag, not used so far
                magSensorData[0] = (float) (byte2float(sensorData, 28) * Math.PI / 180.0f);
                magSensorData[1] = (float) (byte2float(sensorData, 32) * Math.PI / 180.0f);
                magSensorData[2] = (float) (byte2float(sensorData, 36) * Math.PI / 180.0f);

                mTicks = byte2int(sensorData, 40);

                mListener.onSensorChanged(accSensorData, gyroSensorData, magSensorData);
            }
        }

        @Override
        public void commandResp(String resp){
            byte[] data;
            Log.d(TAG, "commandResp: start");
            try {
                data = resp.getBytes("ISO-8859-1");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return;
            }

            if (data[8] == 1) {
                mTimeNanos = System.nanoTime();
                mStartTicks = byte2int(data, 4);
                Log.d(TAG, "sensorEvent starttick = : " + mStartTicks);
            }
        }
    };


    private final class  RemoteCallbackApiConn implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "onServiceConnected: ");
            mRemoteCallbackApi=IUsbCallbackApi.Stub.asInterface(iBinder);
            try {
                mRemoteCallbackApi.registerListener(mIRemoteCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            try {
                byte[] command = new byte[12];
                command[0] = 0x66;
                command[1] = 0x1;
                mRemoteCallbackApi.sendCommand(new String(command, "ISO-8859-1"));
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected: ");
//            try {
//                mRemoteCallbackApi.unRegisterListener(mIRemoteCallback);
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
        }
    }

    public void start(){
        Log.d(TAG, "start: ");
        AIDLUtil.bindAIDLService(mContext,mCallbackApiConn,ACTION_CALLBACK);
    }

    public void stop(){
        Log.d(TAG, "stop: ");
        try {
            mRemoteCallbackApi.unRegisterListener(mIRemoteCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mContext.unbindService(mCallbackApiConn);
    }

        /*
    SENSOR_DATA_ON = 1,
    SENSOR_DATA_OFF = 2,
    SENSOR_GYRO_CORRECTION = 3,
    CAMERA_ENABLE = 4,
    CAMERA_DISABLE = 5,
    PANEL_LUMINANCE_SET = 8, //0 for panel off
    PANEL_PRESET_SET = 9,
    PANEL_ROTATION_SET = 10,
    PANEL_BRIGHTNESS_SET = 11,
    PANEL_ON = 14,
    PANEL_OFF = 15,
    CAMERA_ON = 20,
    CAMERA_OFF = 21,
    SENSOR_DATA = 101,
    */

    private void sendUsbCommand(byte cmd){
        sendUsbCommand(cmd, (byte)0);
    }

    private void sendUsbCommand(byte cmd, byte value){
        try {
            byte[] command = new byte[12];
            command[0] = 0x66;
            command[1] = cmd;
            command[2] = value;
            mRemoteCallbackApi.sendCommand(new String(command, "ISO-8859-1"));
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void openSensor(){
        sendUsbCommand((byte)0x1);
    }

    public void closeSensor(){
        sendUsbCommand((byte)0x2);
    }

    public void enableSideBySideMode(){
        sendUsbCommand((byte)0x6);
    }

    public void disableSideBySideMode(){
        sendUsbCommand((byte)0x7);
    }

    public void turnOnGlassScreen(){
        sendUsbCommand((byte)0x0e);
    }

    public void turnOffGlassScreen(){
        sendUsbCommand((byte)0x0f);
    }

    public void setPresetBrightness(byte level) {
        sendUsbCommand((byte)0x9, level);
    }

    public void setInstantLuminance(byte value) {
        sendUsbCommand((byte)0x8, value);
    }

    public void setRotation(byte rotation) {
        sendUsbCommand((byte)0xA, rotation);
    }

}
