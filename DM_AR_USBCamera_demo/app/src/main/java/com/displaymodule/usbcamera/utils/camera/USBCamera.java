package com.displaymodule.usbcamera.utils.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import com.displaymodule.libuvccamera.CameraConfig;
import com.displaymodule.libuvccamera.IFrameCallback;
import com.displaymodule.libuvccamera.Size;
import com.displaymodule.libuvccamera.UVCCamera;
import com.displaymodule.libuvccamera.usb.ConnectCallback;
import com.displaymodule.libuvccamera.usb.UsbMonitor;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.displaymodule.libuvccamera.UVCCamera.FRAME_FORMAT_MJPEG;

public class USBCamera implements IUVCCamera {
    private static final String TAG = "USBCamera";
    private static int PICTURE_WIDTH = 640;
    private static int PICTURE_HEIGHT = 480;
    private Context mContext;
    private UsbMonitor mUsbMonitor;
    protected UVCCamera mUVCCamera;
    private View mPreviewView;
    private Surface mSurface;
    private PreviewCallback mPreviewCallback;
    private PictureCallback mPictureCallback;
    private ConnectCallback mConnectCallback;
    private CameraConfig mConfig;
    protected float mPreviewRotation;
    protected boolean isTakePhoto;
    private String mPictureName;

    public USBCamera(Context context) {
        mContext = context;
        mConfig = new CameraConfig();
        mUsbMonitor = new UsbMonitor(mContext, mConfig);
    }
    /**
     * register usb attach broadcast
     */

    @Override
    public void registerReceiver() {
        mUsbMonitor.registerReceiver();
    }

    /**
     * unregister usb broadcast
     */
    @Override
    public void unregisterReceiver() {
        mUsbMonitor.unregisterReceiver();
    }

    /**
     * check if usb camera is connected
     */
    @Override
    public void checkDevice() {
        mUsbMonitor.checkDevice();
    }

    /**
     * request usb permission
     *
     * @param usbDevice
     */
    @Override
    public void requestPermission(UsbDevice usbDevice) {
        mUsbMonitor.requestPermission(usbDevice);
    }

    /**
     * connect usb devices
     *
     * @param usbDevice
     */
    @Override
    public void connectDevice(UsbDevice usbDevice) {
        mUsbMonitor.connectDevice(usbDevice);
    }

    /**
     * close usb devices
     */
    @Override
    public void closeDevice() {
        mUsbMonitor.closeDevice();
    }

    /**
     * open camera
     */
    @Override
    public void openCamera() {
        try {
            mUVCCamera = new UVCCamera();
            mUVCCamera.open(mUsbMonitor.getUsbController());
            Log.i(TAG, "openCamera");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mUVCCamera != null && mConnectCallback != null) {
            mConnectCallback.onCameraOpened();
        }
    }

    /**
     * close camera
     */
    @Override
    public void closeCamera() {
        try {
            if (mUVCCamera != null) {
                mUVCCamera.destroy();
                mUVCCamera = null;
            }
            mUsbMonitor.closeDevice();
            Log.i(TAG,"closeCamera");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setPreviewSurface(SurfaceView surfaceView) {
        this.mPreviewView = surfaceView;
        if (surfaceView != null && surfaceView.getHolder() != null) {
            surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    Log.i(TAG,"surfaceCreated");
                    mSurface = holder.getSurface();
                    checkDevice();
                    registerReceiver();
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    Log.i(TAG,"surfaceChanged");
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    Log.i(TAG,"surfaceDestroyed");
                    mSurface = null;
                    unregisterReceiver();
                    closeCamera();
                }
            });
        }
    }

    @Override
    public void setPreviewTexture(TextureView textureView) {
        this.mPreviewView = textureView;
        if (textureView != null) {
            if (mPreviewRotation != 0) {
                textureView.setRotation(mPreviewRotation);
            }
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    Log.i(TAG,"onSurfaceTextureAvailable");
                    mSurface = new Surface(surface);
                    checkDevice();
                    registerReceiver();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                    Log.i(TAG,"onSurfaceTextureSizeChanged");
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    Log.i(TAG,"onSurfaceTextureDestroyed");
                    mSurface = null;
                    unregisterReceiver();
                    closeCamera();
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                }
            });
        }
    }

    /**
     * set camera preview rotation, only support TextureView right now
     *
     * @param rotation
     */
    @Override
    public void setPreviewRotation(float rotation) {
        if (mPreviewView != null && mPreviewView instanceof TextureView) {
            this.mPreviewRotation = rotation;
            mPreviewView.setRotation(rotation);
        }
    }

    /**
     * set camera preview Surface
     *
     * @param surface
     */
    @Override
    public void setPreviewDisplay(Surface surface) {
        mSurface = surface;
        try {
            if (mUVCCamera != null && mSurface != null) {
                mUVCCamera.setPreviewDisplay(mSurface);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * set preview size
     *
     * @param width
     * @param height
     */
    @Override
    public void setPreviewSize(int width, int height) {
        try {
            if (mUVCCamera != null) {
                this.PICTURE_WIDTH = width;
                this.PICTURE_HEIGHT = height;
                mUVCCamera.setPreviewSize(width, height, FRAME_FORMAT_MJPEG);
                Log.i(TAG,"setPreviewSize-->" + width + " * " + height);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * get camera preview size
     *
     * @return
     */
    @Override
    public Size getPreviewSize() {
        try {
            if (mUVCCamera != null) {
                return mUVCCamera.getPreviewSize();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * get device supported preview size
     *
     * @return
     */
    @Override
    public List<Size> getSupportedPreviewSizes() {
        try {
            if (mUVCCamera != null) {
                return mUVCCamera.getSupportedSizeList();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @Override
    public void startPreview() {

        try {
            if (mUVCCamera != null) {
                Log.i(TAG,"startPreview");
                mUVCCamera.setFrameCallback(new IFrameCallback() {
                    @Override
                    public void onFrame(ByteBuffer frame) {
                        int lenght = frame.capacity();
                        byte[] yuv = new byte[lenght];
                        frame.get(yuv);
                        if (mPreviewCallback != null) {
                            mPreviewCallback.onPreviewFrame(yuv);
                        }
                        if (isTakePhoto) {
                            Log.i(TAG, "take picture");
                            isTakePhoto = false;
                            savePicture(yuv, PICTURE_WIDTH, PICTURE_HEIGHT, mPreviewRotation);
                        }

                    }
                }, UVCCamera.PIXEL_FORMAT_YUV420SP);

                if (mSurface != null) {
                    mUVCCamera.setPreviewDisplay(mSurface);
                }
                mUVCCamera.updateCameraParams();
                mUVCCamera.startPreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopPreview() {
        try {
            if (mUVCCamera != null) {
                Log.i(TAG,"stopPreview");
                mUVCCamera.setButtonCallback(null);
                mUVCCamera.setFrameCallback(null, 0);
                mUVCCamera.stopPreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void takePicture() {
        isTakePhoto = true;
        mPictureName = UUID.randomUUID().toString() + ".jpg";
    }

    @Override
    public void takePicture(String pictureName) {
        isTakePhoto = true;
        mPictureName = pictureName;
    }

    /**
     * save picture
     *
     * @param yuv
     * @param width
     * @param height
     * @param rotation
     */
    public void savePicture(final byte[] yuv, final int width, final int height, final float rotation) {
        if (mPictureCallback == null) {
            return;
        }
        Log.i(TAG, "savePicture");
        //TODO:
    }

    @Override
    public void setConnectCallback(ConnectCallback callback) {
        this.mConnectCallback = callback;
        this.mUsbMonitor.setConnectCallback(callback);
    }

    @Override
    public void setPreviewCallback(PreviewCallback callback) {
        this.mPreviewCallback = callback;
    }

    @Override
    public void setPictureTakenCallback(PictureCallback callback) {
        this.mPictureCallback = callback;
    }

    @Override
    public UVCCamera getUVCCamera() {
        return mUVCCamera;
    }

    @Override
    public boolean isCameraOpen() {
        return mUVCCamera != null;
    }

    /**
     * config info
     *
     * @return
     */
    @Override
    public CameraConfig getConfig() {
        return mConfig;
    }
}

