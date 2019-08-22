/**
 * 
 */
package com.displaymodule.glassdemo;

import android.content.Context;

import com.displaymodule.glasssensor.sensors.USBSensorCallback;
import com.displaymodule.glasssensor.sensors.USBSensorEventListener;
import com.displaymodule.glasssensor.sensors.representation.MatrixF4x4;
import com.displaymodule.glasssensor.sensors.representation.Quaternion;


/**
 * Classes implementing this interface provide an orientation of the device
 * either by directly accessing hardware, using Android sensor fusion or fusing
 * sensors itself.
 * 
 * The orientation can be provided as rotation matrix or quaternion.
 *
 * 
 */
public abstract class OrientationProvider implements USBSensorEventListener {
    /**
     * Sync-token for syncing read/write to sensor-data from sensor manager and
     * fusion algorithm
     */
    protected final Object synchronizationToken = new Object();

    /**
     * The matrix that holds the current rotation
     */
    protected final MatrixF4x4 currentOrientationRotationMatrix;

    /**
     * The quaternion that holds the current rotation
     */
    protected final Quaternion currentOrientationQuaternion;

    protected USBSensorCallback mGlassSensor;

    protected Context mContext;

    /**
     * Initialises a new OrientationProvider
     *
     */
    public OrientationProvider(Context context, USBSensorCallback glassSensor) {

        mGlassSensor = glassSensor;
        mContext = context;

        // Initialise with identity
        currentOrientationRotationMatrix = new MatrixF4x4();

        // Initialise with identity
        currentOrientationQuaternion = new Quaternion();
    }

    /**
     * Starts the sensor fusion (e.g. when resuming the activity)
     */
    public void start() {
        // enable our sensor when the activity is resumed, ask for
        // 10 ms updates.
//        for (Sensor sensor : sensorList) {
//            // enable our sensors when the activity is resumed, ask for
//            // 20 ms updates (Sensor_delay_game)
//            sensorManager.registerListener(this, sensor,
//                    SensorManager.SENSOR_DELAY_GAME);
//        }
        mGlassSensor.register(mContext, this);
        mGlassSensor.start();
    }

    /**
     * Stops the sensor fusion (e.g. when pausing/suspending the activity)
     */
    public void stop() {
        // make sure to turn our sensors off when the activity is paused
//        for (Sensor sensor : sensorList) {
//            sensorManager.unregisterListener(this, sensor);
//        }
        mGlassSensor.stop();
        mGlassSensor.unRegister();
    }

    /**
     * Get the current rotation of the device in the rotation matrix format (4x4 matrix)
     */
    public void getRotationMatrix(MatrixF4x4 matrix) {
        synchronized (synchronizationToken) {
            matrix.set(currentOrientationRotationMatrix);
        }
    }

    /**
     * Get the current rotation of the device in the quaternion format (vector4f)
     */
    public void getQuaternion(Quaternion quaternion) {
        synchronized (synchronizationToken) {
            quaternion.set(currentOrientationQuaternion);
        }
    }

    /**
     * Get the current rotation of the device in the Euler angles
     */
    public void getEulerAngles(float angles[]) {
        synchronized (synchronizationToken) {
            getOrientation(currentOrientationRotationMatrix.matrix, angles);
        }
    }

    private static float[] getOrientation(float[] R, float[] values) {
        /*
         * 4x4 (length=16) case:
         *   /  R[ 0]   R[ 1]   R[ 2]   0  \
         *   |  R[ 4]   R[ 5]   R[ 6]   0  |
         *   |  R[ 8]   R[ 9]   R[10]   0  |
         *   \      0       0       0   1  /
         *
         * 3x3 (length=9) case:
         *   /  R[ 0]   R[ 1]   R[ 2]  \
         *   |  R[ 3]   R[ 4]   R[ 5]  |
         *   \  R[ 6]   R[ 7]   R[ 8]  /
         *
         */
        if (R.length == 9) {
            values[0] = (float) Math.atan2(R[1], R[4]);
            values[1] = (float) Math.asin(-R[7]);
            values[2] = (float) Math.atan2(-R[6], R[8]);
        } else {
            values[0] = (float) Math.atan2(R[1], R[5]);
            values[1] = (float) Math.asin(-R[9]);
            values[2] = (float) Math.atan2(-R[8], R[10]);
        }

        return values;
    }

    public static boolean getRotationMatrix(float[] R, float[] I,
                                            float[] gravity, float[] geomagnetic) {
        // TODO: move this to native code for efficiency
        float Ax = gravity[0];
        float Ay = gravity[1];
        float Az = gravity[2];

        final float normsqA = (Ax * Ax + Ay * Ay + Az * Az);
        final float g = 9.81f;
        final float freeFallGravitySquared = 0.01f * g * g;
        if (normsqA < freeFallGravitySquared) {
            // gravity less than 10% of normal value
            return false;
        }

        final float Ex = geomagnetic[0];
        final float Ey = geomagnetic[1];
        final float Ez = geomagnetic[2];
        float Hx = Ey * Az - Ez * Ay;
        float Hy = Ez * Ax - Ex * Az;
        float Hz = Ex * Ay - Ey * Ax;
        final float normH = (float) Math.sqrt(Hx * Hx + Hy * Hy + Hz * Hz);

        if (normH < 0.1f) {
            // device is close to free fall (or in space?), or close to
            // magnetic north pole. Typical values are  > 100.
            return false;
        }
        final float invH = 1.0f / normH;
        Hx *= invH;
        Hy *= invH;
        Hz *= invH;
        final float invA = 1.0f / (float) Math.sqrt(Ax * Ax + Ay * Ay + Az * Az);
        Ax *= invA;
        Ay *= invA;
        Az *= invA;
        final float Mx = Ay * Hz - Az * Hy;
        final float My = Az * Hx - Ax * Hz;
        final float Mz = Ax * Hy - Ay * Hx;
        if (R != null) {
            if (R.length == 9) {
                R[0] = Hx;     R[1] = Hy;     R[2] = Hz;
                R[3] = Mx;     R[4] = My;     R[5] = Mz;
                R[6] = Ax;     R[7] = Ay;     R[8] = Az;
            } else if (R.length == 16) {
                R[0]  = Hx;    R[1]  = Hy;    R[2]  = Hz;   R[3]  = 0;
                R[4]  = Mx;    R[5]  = My;    R[6]  = Mz;   R[7]  = 0;
                R[8]  = Ax;    R[9]  = Ay;    R[10] = Az;   R[11] = 0;
                R[12] = 0;     R[13] = 0;     R[14] = 0;    R[15] = 1;
            }
        }
        if (I != null) {
            // compute the inclination matrix by projecting the geomagnetic
            // vector onto the Z (gravity) and X (horizontal component
            // of geomagnetic vector) axes.
            final float invE = 1.0f / (float) Math.sqrt(Ex * Ex + Ey * Ey + Ez * Ez);
            final float c = (Ex * Mx + Ey * My + Ez * Mz) * invE;
            final float s = (Ex * Ax + Ey * Ay + Ez * Az) * invE;
            if (I.length == 9) {
                I[0] = 1;     I[1] = 0;     I[2] = 0;
                I[3] = 0;     I[4] = c;     I[5] = s;
                I[6] = 0;     I[7] = -s;     I[8] = c;
            } else if (I.length == 16) {
                I[0] = 1;     I[1] = 0;     I[2] = 0;
                I[4] = 0;     I[5] = c;     I[6] = s;
                I[8] = 0;     I[9] = -s;     I[10] = c;
                I[3] = I[7] = I[11] = I[12] = I[13] = I[14] = 0;
                I[15] = 1;
            }
        }
        return true;
    }

    public long getTimeNanos() {
        return mGlassSensor.getTimeNanos();
    }

}
