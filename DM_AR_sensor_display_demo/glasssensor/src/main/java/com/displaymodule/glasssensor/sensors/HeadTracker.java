/*
 * Copyright 2014 Google Inc. All Rights Reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.displaymodule.glasssensor.sensors;

import android.content.Context;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.opengl.Matrix;
import android.os.Looper;
import android.util.Log;

import com.displaymodule.glasssensor.sensors.internal.OrientationEKF;


/**
 * Provides head tracking information from the device IMU. 
 */
public class HeadTracker {
	private final String TAG = getClass().getName();
	private static final double NS2S = 1.E-09D;
	private static final int[] INPUT_SENSORS = { 1, 4 };
	private final Context mContext;
	private final float[] mEkfToHeadTracker = new float[16];

	private final float[] mTmpHeadView = new float[16];

	private final float[] mTmpRotatedEvent = new float[3];
	private Looper mSensorLooper;
	private SensorEventListener mSensorEventListener;
	private volatile boolean mTracking;
	private final OrientationEKF mTracker = new OrientationEKF();
	private long mLastGyroEventTimeNanos;

//	private USBSensor mUSBSensor = new USBSensor();
	private USBSensorCallback mUSBSensorCallback = new USBSensorCallback();

	private float yaw, roll, pitch;

	public HeadTracker(Context context) {
		mContext = context;
		Matrix.setRotateEulerM(mEkfToHeadTracker, 0, -90.0F, 0.0F, 0.0F);

		// Sensor from Glass
//		mUSBSensor.register(context, (accSensorData, gyroSensorData, magSensorData) -> {
		mUSBSensorCallback.register(context, (accSensorData, gyroSensorData, magSensorData) -> {
			yaw = magSensorData[0];
			pitch = magSensorData[1];
			roll = magSensorData[2];
			long timeNanos = System.nanoTime();
			mLastGyroEventTimeNanos = timeNanos;
			synchronized (mTracker) {
//				mTracker.processAcc(accSensorData, mUSBSensor.getTimeNanos());
//				mTracker.processGyro(gyroSensorData, mUSBSensor.getTimeNanos());
                mTracker.processAcc(accSensorData, mUSBSensorCallback.getTimeNanos());
                mTracker.processGyro(gyroSensorData, mUSBSensorCallback.getTimeNanos());
			}
			//Log.d(TAG, " Acc from Glass" + accSensorData[0] + ", " + accSensorData[1] + ", " + accSensorData[2]);
			//Log.d(TAG, " Gyro from Glass" + gyroSensorData[0] + ", " + gyroSensorData[1] + ", " + gyroSensorData[2]);
		});
	}

	public void destroy() {
//		mUSBSensor.unRegister();
        mUSBSensorCallback.unRegister();
	}
//
//	public void startTracking(){
//		if (mTracking) {
//			return;
//		}
//
//		mTracker.reset();
//
//		// Sensor from Mobile
//		mSensorEventListener = new SensorEventListener(){
//			public void onSensorChanged(SensorEvent event) {
//				HeadTracker.this.processSensorEvent(event);
//				/*if(event.sensor.getType() == 1){
//					Log.d(TAG, "Acc from Mobile" + (-event.values[1]) + ", " + event.values[0] + ", " + event.values[2]);
//				}else if (event.sensor.getType() == 4) {
//					Log.d(TAG, "Gyro from Mobile" + (-event.values[1]) + ", " + event.values[0] + ", " + event.values[2]);
//				}*/
//			}
//
//			public void onAccuracyChanged(Sensor sensor, int accuracy)
//			{
//			}
//		};
//		Thread sensorThread = new Thread(new Runnable() {
//			@Override
//			public void run() {
//				Looper.prepare();
//
//				mSensorLooper = Looper.myLooper();
//				Handler handler = new Handler();
//
//				SensorManager sensorManager = (SensorManager) mContext.getSystemService("sensor");
//
//				for (int sensorType : HeadTracker.INPUT_SENSORS) {
//					Sensor sensor = sensorManager.getDefaultSensor(sensorType);
//					sensorManager.registerListener(mSensorEventListener, sensor, 0, handler);
//				}
//
//				Looper.loop();
//			}
//		});
//		sensorThread.start();
//		mTracking = true;
//	}
//
//	public void stopTracking(){
//		if (!mTracking) {
//			return;
//		}
//
//		SensorManager sensorManager = (SensorManager)mContext.getSystemService("sensor");
//
//		sensorManager.unregisterListener(mSensorEventListener);
//		mSensorEventListener = null;
//
//		mSensorLooper.quit();
//		mSensorLooper = null;
//		mTracking = false;
//	}

	public void startTracking(){
		if (mTracking) {
			return;
		}

		mTracker.reset();

//		mUSBSensor.start();
		mUSBSensorCallback.start();
		mTracking = true;

		Log.d(TAG, "startTracking");
	}

	public void stopTracking() {
		if (!mTracking) {
			return;
		}
//		mUSBSensor.stop();
		mUSBSensorCallback.stop();

		mTracking = false;
		Log.d(TAG, "stopTracking");
	}

	public void getLastHeadView(float[] headView, int offset) {
		if (offset + 16 > headView.length) {
			throw new IllegalArgumentException("Not enough space to write the result");
		}

		synchronized (mTracker) {
			double secondsSinceLastGyroEvent = (System.nanoTime() - mLastGyroEventTimeNanos) * 1.E-09D;

			double secondsToPredictForward = secondsSinceLastGyroEvent + 0.03333333333333333D;
			double[] mat = mTracker.getPredictedGLMatrix(secondsToPredictForward);
			for (int i = 0; i < headView.length; i++) {
				mTmpHeadView[i] = ((float)mat[i]);
			}
		}

		Matrix.multiplyMM(headView, offset, mTmpHeadView, 0, mEkfToHeadTracker, 0);
	}

	private void processSensorEvent(SensorEvent event) {
		long timeNanos = System.nanoTime();

		mTmpRotatedEvent[0] = (-event.values[1]);
		mTmpRotatedEvent[1] = event.values[0];
		mTmpRotatedEvent[2] = event.values[2];
		synchronized (mTracker) {
			if (event.sensor.getType() == 1) {
				mTracker.processAcc(mTmpRotatedEvent, event.timestamp);

			} else if (event.sensor.getType() == 4) {
				mLastGyroEventTimeNanos = timeNanos;
				mTracker.processGyro(mTmpRotatedEvent, event.timestamp);
			}
		}
	}

	public float getYaw(){
		return yaw;
	}

	public float getRoll(){
		return roll;
	}

	public float getPitch(){
		return pitch;
	}
}