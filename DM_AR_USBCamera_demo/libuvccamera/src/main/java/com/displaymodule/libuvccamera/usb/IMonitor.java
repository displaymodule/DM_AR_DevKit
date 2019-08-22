package com.displaymodule.libuvccamera.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;

public interface IMonitor {
    void registerReceiver();

    void unregisterReceiver();

    void checkDevice();

    void requestPermission(UsbDevice usbDevice);

    void connectDevice(UsbDevice usbDevice);

    void closeDevice();

    UsbController getUsbController();

    UsbDeviceConnection getConnection();
}
