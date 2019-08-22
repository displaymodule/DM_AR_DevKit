package com.displaymodule.usbcamera.utils.camera;

import android.hardware.usb.UsbDevice;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import com.displaymodule.libuvccamera.CameraConfig;
import com.displaymodule.libuvccamera.Size;
import com.displaymodule.libuvccamera.UVCCamera;
import com.displaymodule.libuvccamera.usb.ConnectCallback;

import java.util.List;

public interface IUVCCamera {
    /**
     * register usb attach receiver
     */
    void registerReceiver();

    /**
     * register usb attach receiver
     */
    void unregisterReceiver();

    /**
     * check if the device is attached
     */
    void checkDevice();

    /**
     * request usb permissiono
     *
     * @param usbDevice
     */
    void requestPermission(UsbDevice usbDevice);

    /**
     * connect usb device
     *
     * @param usbDevice
     */
    void connectDevice(UsbDevice usbDevice);

    /**
     * close usb device
     */
    void closeDevice();

    /**
     * open camera
     */
    void openCamera();

    /**
     * close camera
     */
    void closeCamera();

    /**
     * set preview surface
     *
     * @param surfaceView
     */
    void setPreviewSurface(SurfaceView surfaceView);

    /**
     * set preview texture
     *
     * @param textureView
     */
    void setPreviewTexture(TextureView textureView);

    /**
     * set preview rotation in case the rotation is incorrect
     *
     * @param rotation
     */
    void setPreviewRotation(float rotation);

    /**
     * set preview display
     *
     * @param surface
     */
    void setPreviewDisplay(Surface surface);

    /**
     * set preview size
     *
     * @param width
     * @param height
     */
    void setPreviewSize(int width, int height);

    /**
     * get preview size
     *
     * @return
     */
    Size getPreviewSize();

    /**
     * get device supported preview size list
     *
     * @return
     */
    List<Size> getSupportedPreviewSizes();

    /**
     * start preview
     */
    void startPreview();

    /**
     * stop preview
     */
    void stopPreview();

    /**
     * take picture
     */
    void takePicture();

    /**
     * take picture
     *
     * @param pictureName picture name
     */
    void takePicture(String pictureName);

    /**
     * set usb device connect callback
     *
     * @param callback
     */
    void setConnectCallback(ConnectCallback callback);

    /**
     * set preview callback
     *
     * @param callback
     */
    void setPreviewCallback(PreviewCallback callback);

    /**
     * set takepicture callback
     * @param callback
     */
    void setPictureTakenCallback(PictureCallback callback);
    /**
     * uvc camera instance
     *
     * @return
     */
    UVCCamera getUVCCamera();

    /**
     * check if camera is opened
     *
     * @return
     */
    boolean isCameraOpen();

    /**
     * camera configure info
     *
     * @return
     */
    public CameraConfig getConfig();
}

