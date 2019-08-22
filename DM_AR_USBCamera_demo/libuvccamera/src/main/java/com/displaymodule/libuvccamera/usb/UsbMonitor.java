package com.displaymodule.libuvccamera.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.displaymodule.libuvccamera.CameraConfig;

import java.util.HashMap;

public class UsbMonitor implements IMonitor {
    private static final String TAG = "UsbMonitor";
    private static final String ACTION_USB_DEVICE_PERMISSION = "com.displaymodule.USB_PERMISSION";
    private Context mContext;
    private UsbManager mUsbManager;
    private USBReceiver mUsbReceiver;
    private UsbController mUsbController;
    private ConnectCallback mConnectCallback;
    private CameraConfig mConfig;

    public UsbMonitor(Context context, CameraConfig config) {
        this.mContext = context;
        this.mConfig = config;
        this.mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    /**
     * register receiver of usb device attach / deattach
     */
    @Override
    public void registerReceiver() {
        Log.i(TAG, "registerReceiver");
        if (mUsbReceiver == null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            filter.addAction(ACTION_USB_DEVICE_PERMISSION);
            mUsbReceiver = new USBReceiver();
            mContext.registerReceiver(mUsbReceiver, filter);
        }
    }

    /**
     * unregister receiver of usb device attach / deattach
     */
    @Override
    public void unregisterReceiver() {
        Log.i(TAG, "unregisterReceiver");
        if (mUsbReceiver != null) {
            mContext.unregisterReceiver(mUsbReceiver);
            mUsbReceiver = null;
        }
    }

    @Override
    public void checkDevice() {
        Log.i(TAG, "checkDevice");
        UsbDevice usbDevice = getUsbCameraDevice();
        if (isTargetDevice(usbDevice) && mConnectCallback != null) {
            mConnectCallback.onAttached(usbDevice);
        }
    }

    @Override
    public void requestPermission(UsbDevice usbDevice) {
        Log.i(TAG,"requestPermission-->" + usbDevice);
        if (mUsbManager.hasPermission(usbDevice)) {
            if (mConnectCallback != null) {
                mConnectCallback.onGranted(usbDevice, true);
            }
        } else {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_DEVICE_PERMISSION), 0);
            mUsbManager.requestPermission(usbDevice, pendingIntent);
        }
    }

    @Override
    public void connectDevice(UsbDevice usbDevice) {
        Log.i(TAG,"connectDevice-->" + usbDevice);
        mUsbController = new UsbController(mUsbManager, usbDevice);
        if (mUsbController.open() != null && mConnectCallback != null) {
            mConnectCallback.onConnected(usbDevice);
        }
    }

    @Override
    public void closeDevice() {
        Log.i(TAG,"closeDevice");
        if (mUsbController != null) {
            mUsbController.close();
            mUsbController = null;
        }
    }

    @Override
    public UsbController getUsbController() {
        return mUsbController;
    }

    @Override
    public UsbDeviceConnection getConnection() {
        if (mUsbController != null) {
            return mUsbController.getConnection();
        }
        return null;
    }

    public void setConnectCallback(ConnectCallback callback) {
        this.mConnectCallback = callback;
    }

    /**
     * check if usb camera exist
     *
     * @return
     */
    public boolean hasUsbCamera() {
        return getUsbCameraDevice() != null;
    }

    /**
     * get the usb camera device
     *
     * @return
     */
    public UsbDevice getUsbCameraDevice() {
        HashMap<String, UsbDevice> deviceMap = mUsbManager.getDeviceList();
        if (deviceMap != null) {
            for (UsbDevice usbDevice : deviceMap.values()) {
                if (isUsbCamera(usbDevice)) {
                    return usbDevice;
                }
            }
        }
        return null;
    }

    /**
     * check if the device is a USB camera device，usb camera class is 239-2
     *
     * @param usbDevice
     * @return
     */
    public boolean isUsbCamera(UsbDevice usbDevice) {
        return usbDevice != null && 239 == usbDevice.getDeviceClass() && 2 == usbDevice.getDeviceSubclass();
    }

    /**
     * if the target device's pid and vid is same as configure
     *
     * @param usbDevice
     * @return
     */
    public boolean isTargetDevice(UsbDevice usbDevice) {
        if (!isUsbCamera(usbDevice)
                || mConfig == null
                || (mConfig.getProductId() != 0 && mConfig.getProductId() != usbDevice.getProductId())
                || (mConfig.getVendorId() != 0 && mConfig.getVendorId() != usbDevice.getVendorId())) {
            Log.i(TAG,"No target camera device");
            return false;
        }
        Log.i(TAG,"Find target camera device");
        return true;
    }

    /**
     * usb attach / deattach broadcastreceiver
     */
    private class USBReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            Log.i(TAG,"usbDevice-->" + usbDevice);
            if (!isTargetDevice(usbDevice) || mConnectCallback == null) {
                return;
            }

            switch (intent.getAction()) {
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    Log.i(TAG,"onAttached");
                    mConnectCallback.onAttached(usbDevice);
                    break;

                case ACTION_USB_DEVICE_PERMISSION:
                    boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                    mConnectCallback.onGranted(usbDevice, granted);
                    Log.i(TAG,"onGranted-->" + granted);
                    break;

                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    Log.i(TAG,"onDetached");
                    mConnectCallback.onDetached(usbDevice);
                    break;

                default:
                    break;
            }
        }
    }
}

