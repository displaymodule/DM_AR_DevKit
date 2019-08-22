package com.displaymodule.usbcamera.utils.camera;

public interface USBCameraListener {
    /**
     * on camera opened
     * @param cameraId camera ID (not used right now)
     * @param displayOrientation preview rotation
     * @param isMirror display mirror or not
     */
    void onCameraOpened(USBCamera camera, int cameraId, int displayOrientation, boolean isMirror);

    /**
     * preview data callback
     * @param data preview data
     */
    void onPreview(byte[] data);

    /**
     * on camera closed
     */
    void onCameraClosed();

    /**
     * on camera error
     * @param e camera error exception
     */
    void onCameraError(Exception e);

    /**
     * configure changed
     * @param cameraID  camera id (not used right now)
     * @param displayOrientation    camera rotation
     */
    void onCameraConfigurationChanged(int cameraID, int displayOrientation);
}

