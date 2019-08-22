package com.displaymodule.usbcamera.utils.camera;

import android.content.Context;
import android.graphics.Point;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import com.displaymodule.libuvccamera.Size;
import com.displaymodule.libuvccamera.usb.ConnectCallback;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class USBCameraHelper implements PreviewCallback{
    private static final String TAG = "USBCameraHelper";
    public static final int DEFAULT_PREVIEW_WIDTH = 1280;
    public static final int DEFAULT_PREVIEW_HEIGHT = 720;
    private Context mContext;
    private USBCamera mCamera;
    private int mCameraId;
    private Point previewViewSize;
    private View previewDisplayView;
    private Size previewSize;
    private Point specificPreviewSize;
    private int displayOrientation = 0;
    private int rotation;
    private int additionalRotation;
    private boolean isMirror = false;

    private Integer specificCameraId = null;
    private USBCameraListener cameraListener;


    private USBCameraHelper(USBCameraHelper.Builder builder) {
        mContext = (Context)builder.context.get();
        previewDisplayView = builder.previewDisplayView;
        specificCameraId = builder.specificCameraId;
        cameraListener = builder.cameraListener;
        rotation = builder.rotation;
        additionalRotation = builder.additionalRotation;
        previewViewSize = builder.previewViewSize;
        specificPreviewSize = builder.previewSize;
        if (builder.previewDisplayView instanceof TextureView) {
            isMirror = builder.isMirror;
        } else if (isMirror) {
            throw new RuntimeException("mirror is effective only when the preview is on a textureView");
        }
    }

    public void init() {

        if (isMirror) {
            previewDisplayView.setScaleX(-1);
        }
    }

    ConnectCallback connectCallback = new ConnectCallback() {
        @Override
        public void onAttached(UsbDevice usbDevice) {
            Log.i(TAG, "onAttached");
            mCamera.requestPermission(usbDevice);
        }

        @Override
        public void onGranted(UsbDevice usbDevice, boolean granted) {
            if (granted) {
                mCamera.connectDevice(usbDevice);
            } else {
                Log.e(TAG, "usb device permission denied");
            }
        }

        @Override
        public void onConnected(UsbDevice usbDevice) {
            mCamera.openCamera();
            Log.i(TAG, "openCamera");
        }

        @Override
        public void onCameraOpened() {
            mCamera.setPreviewSize(DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT);
            //mCamera.startPreview();
            try {
                previewSize = mCamera.getPreviewSize();
                List<Size> supportedPreviewSizes = mCamera.getSupportedPreviewSizes();
                if (supportedPreviewSizes != null && supportedPreviewSizes.size() > 0) {
                    previewSize = getBestSupportedSize(supportedPreviewSizes, previewViewSize);
                }
                mCamera.setPreviewCallback(USBCameraHelper.this);
                mCamera.startPreview();
                if (cameraListener != null) {
                    cameraListener.onCameraOpened(mCamera, mCameraId, 0/*displayOrientation*/, isMirror);
                }
            } catch (Exception e) {
                if (cameraListener != null) {
                    cameraListener.onCameraError(e);
                }
            }
        }

        @Override
        public void onDetached(UsbDevice usbDevice) {
            mCamera.closeCamera();
        }
    };

    public void start() {
        synchronized (this) {
            if (mCamera != null) {
                return;
            }
            if (mCamera == null) {
                mCamera = new USBCamera(mContext);
                mCamera.getConfig().setProductId(0)
                        .setVendorId(0);//no filter here
                if (previewDisplayView instanceof TextureView) {
                    mCamera.setPreviewTexture(((TextureView) previewDisplayView));
                } else {
                    mCamera.setPreviewSurface(((SurfaceView) previewDisplayView));
                }
                mCamera.setConnectCallback(connectCallback);
            }
            displayOrientation = getCameraOri(rotation);// here we just let the rotation == 0, since the AR Glass is landscape
            //mCamera.setPreviewRotation(displayOrientation);

        }
    }

    private int getCameraOri(int rotation) {
        int degrees = rotation * 90;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
                break;
        }
        additionalRotation /= 90;
        additionalRotation *= 90;
        degrees += additionalRotation;
        int result = 0;
//        Camera.CameraInfo info = new Camera.CameraInfo();
//        Camera.getCameraInfo(mCameraId, info);
//        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            result = (info.orientation + degrees) % 360;
//            result = (360 - result) % 360;
//        } else {
//            result = (info.orientation - degrees + 360) % 360;
//        }
        return result;
    }

    public void stop() {
        synchronized (this) {
            if (mCamera == null) {
                return;
            }
            //mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.closeCamera();
//            mCamera.release();
            mCamera = null;
            if (cameraListener != null) {
                cameraListener.onCameraClosed();
            }
        }
    }

    public boolean isStopped() {
        synchronized (this) {
            return mCamera == null;
        }
    }

    public void release() {
        synchronized (this) {
            stop();
            previewDisplayView = null;
            specificCameraId = null;
            cameraListener = null;
            previewViewSize = null;
            specificPreviewSize = null;
            previewSize = null;
        }
    }

    private Size getBestSupportedSize(List<Size> sizes, Point previewViewSize) {
        if (sizes == null || sizes.size() == 0) {
            return mCamera.getPreviewSize();
        }
        Size[] tempSizes = sizes.toArray(new Size[0]);
        Arrays.sort(tempSizes, new Comparator<Size>() {
            @Override
            public int compare(Size o1, Size o2) {
                if (o1.width > o2.width) {
                    return -1;
                } else if (o1.width == o2.width) {
                    return o1.height > o2.height ? -1 : 1;
                } else {
                    return 1;
                }
            }
        });
        sizes = Arrays.asList(tempSizes);

        Size bestSize = sizes.get(0);
        float previewViewRatio;
        if (previewViewSize != null) {
            previewViewRatio = (float) previewViewSize.x / (float) previewViewSize.y;
        } else {
            previewViewRatio = (float) bestSize.width / (float) bestSize.height;
        }

        if (previewViewRatio > 1) {
            previewViewRatio = 1 / previewViewRatio;
        }
        boolean isNormalRotate = (additionalRotation % 180 == 0);

        for (Size s : sizes) {
            if (specificPreviewSize != null && specificPreviewSize.x == s.width && specificPreviewSize.y == s.height) {
                return s;
            }
            if (isNormalRotate) {
                if (Math.abs((s.height / (float) s.width) - previewViewRatio) < Math.abs(bestSize.height / (float) bestSize.width - previewViewRatio)) {
                    bestSize = s;
                }
            } else {
                if (Math.abs((s.width / (float) s.height) - previewViewRatio) < Math.abs(bestSize.width / (float) bestSize.height - previewViewRatio)) {
                    bestSize = s;
                }
            }
        }
        return bestSize;
    }

    public List<Size> getSupportedPreviewSizes() {
        if (mCamera == null) {
            return null;
        }
        return mCamera.getSupportedPreviewSizes();
    }


    @Override
    public void onPreviewFrame(byte[] nv21) {
        if (cameraListener != null) {
            cameraListener.onPreview(nv21);
        }
    }
    /*
        private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
    //            start();
                if (mCamera != null) {
                    try {
                        mCamera.setPreviewTexture(surfaceTexture);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                Log.i(TAG, "onSurfaceTextureSizeChanged: " + width + "  " + height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                stop();
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        };
        private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
    //            start();
                if (mCamera != null) {
    //                try {
                        mCamera.setPreviewDisplay(holder.getSurface());
    //                } catch (IOException e) {
    //                    e.printStackTrace();
    //                }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                stop();
            }
        };
    */
    public void changeDisplayOrientation(int rotation) {
        if (mCamera != null) {
            this.rotation = rotation;
            displayOrientation = getCameraOri(rotation);
            mCamera.setPreviewRotation(displayOrientation);
            if (cameraListener != null) {
                cameraListener.onCameraConfigurationChanged(mCameraId, displayOrientation);
            }
        }
    }

    public static final class Builder {

//        private Context context;
        private WeakReference<Context> context;
        /**
         * preview view，only support surfaceView and textureView right now
         */
        private View previewDisplayView;

        /**
         * mirror preview，only support textureView
         */
        private boolean isMirror;
        /**
         * not used right now
         */
        private Integer specificCameraId;
        /**
         * lister
         */
        private USBCameraListener cameraListener;
        /**
         * previewView size, only used when choose best size
         */
        private Point previewViewSize;
        /**
         * getWindowManager().getDefaultDisplay().getRotation()
         */
        private int rotation;
        /**
         * specify dedicated preview size
         */
        private Point previewSize;

        /**
         * rotation for some special devices
         */
        private int additionalRotation;

        public Builder() {
        }


        public USBCameraHelper.Builder Context(Context val) {
            context = new WeakReference(val);
            return this;
        }

        public USBCameraHelper.Builder previewOn(View val) {
            if (val instanceof SurfaceView || val instanceof TextureView) {
                previewDisplayView = val;
                return this;
            } else {
                throw new RuntimeException("you must preview on a textureView or a surfaceView");
            }
        }


        public USBCameraHelper.Builder isMirror(boolean val) {
            isMirror = val;
            return this;
        }

        public USBCameraHelper.Builder previewSize(Point val) {
            previewSize = val;
            return this;
        }

        public USBCameraHelper.Builder previewViewSize(Point val) {
            previewViewSize = val;
            return this;
        }

        public USBCameraHelper.Builder rotation(int val) {
            rotation = val;
            return this;
        }

        public USBCameraHelper.Builder additionalRotation(int val) {
            additionalRotation = val;
            return this;
        }

        public USBCameraHelper.Builder specificCameraId(Integer val) {
            specificCameraId = val;
            return this;
        }

        public USBCameraHelper.Builder cameraListener(USBCameraListener val) {
            cameraListener = val;
            return this;
        }

        public USBCameraHelper build() {
            if (previewViewSize == null) {
                Log.e(TAG, "previewViewSize is null, now use default previewSize");
            }
            if (cameraListener == null) {
                Log.e(TAG, "cameraListener is null, callback will not be called");
            }
            if (previewDisplayView == null) {
                throw new RuntimeException("you must preview on a textureView or a surfaceView");
            }
            if (context == null) {
                throw new RuntimeException("context is null");
            }
            return new USBCameraHelper(this);
        }
    }
}

