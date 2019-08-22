package com.displaymodule.usbcamera.utils.camera;

public interface PreviewCallback {
    /**
     * preview call back
     *
     * @param yuv yuv data
     */
    void onPreviewFrame(byte[] yuv);
}
