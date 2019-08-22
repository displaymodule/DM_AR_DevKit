package com.displaymodule.libuvccamera.usb;

import android.hardware.usb.UsbDevice;

public interface ConnectCallback {
    /**
     * attach the device
     *
     * @param usbDevice
     */
    void onAttached(UsbDevice usbDevice);

    /**
     * USB device grant permission
     *
     * @param usbDevice
     * @param granted   permission granted or not
     */
    void onGranted(UsbDevice usbDevice, boolean granted);

    /**
     * on device connected successfully
     *
     * @param usbDevice
     */
    void onConnected(UsbDevice usbDevice);

    /**
     * camera open successfully
     */
    void onCameraOpened();

    /**
     * device detached.
     *
     * @param usbDevice
     */
    void onDetached(UsbDevice usbDevice);
}

