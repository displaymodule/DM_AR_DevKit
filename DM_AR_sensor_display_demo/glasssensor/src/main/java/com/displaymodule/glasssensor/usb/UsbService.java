package com.displaymodule.glasssensor.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;


import com.displaymodule.glasssensor.usbservice.IUsbCallback;
import com.displaymodule.glasssensor.usbservice.IUsbCallbackApi;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class UsbService extends AbstractService {
    private UsbManager mUsbManager = null;
    private PendingIntent mPermissionIntent;
    private static UsbEndpoint mUsbEndpointIn;
    private static UsbEndpoint mUsbEndpointOut;
    private static UsbInterface mUsbInterface;
    private static UsbDeviceConnection mUsbDeviceConnection = null;
    private static Thread mReadingThread = null;
    private static boolean isReading = false;

    private static String TAG = "UsbService";
    private static final String ACTION_DEVICE_PERMISSION = "com.linc.USB_PERMISSION";

    private static RemoteCallbackList<IUsbCallback> mRemoteCallbackList = new RemoteCallbackList<>();

    //Get device permission broadcast
    private final BroadcastReceiver mUsbPermissionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent);
            String action = intent.getAction();
            if (ACTION_DEVICE_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            initDevice(device);
                        }
                    }
                }
            }
        }
    };


    //Get USB attached or detached broadcast
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.d(TAG, "onReceive: " + action);

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {   // 插入
                searchUsb();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {  // 拔出
                closeUsbService();
            }
        }
    };

    // Search USB device
    private void searchUsb() {
        int MCU_PID = 0x3333;
        int MCU_VID = 0x533;
        int MCU_VID_V2=0x1bbb;
        Log.e(TAG, "start search");
        if (isReading) {
            return;
        }
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        assert mUsbManager != null;
        HashMap<String, UsbDevice> devices = mUsbManager.getDeviceList();
        for (UsbDevice device : devices.values()) {
            int vid = device.getVendorId();
            int pid = device.getProductId();
            Log.e(TAG, "initDevice: pid = " + pid + ", vid = " + vid);
            if (MCU_PID == pid && (MCU_VID == vid||MCU_VID_V2==vid)) {
                Toast.makeText(this, "AR device detected", Toast.LENGTH_LONG).show();
                if (mUsbManager.hasPermission(device)) {
                    initDevice(device);
                    return;
                } else {
                    mUsbManager.requestPermission(device, mPermissionIntent);
                }
            }
        }

//        Toast.makeText(this, "No AR device found", Toast.LENGTH_LONG).show();
    }

    //Initialize device
    private void initDevice(UsbDevice device) {
        UsbInterface usbInterface = device.getInterface(0);

        for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
            UsbEndpoint ep = usbInterface.getEndpoint(i);
            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                    mUsbEndpointIn = ep;
                } else {
                    mUsbEndpointOut = ep;
                }
            }
        }

        //TODO send message to control MCU sensor
        //0x66 0x01 for enable sensor data
        //0x66 0x02 for disable
        //0x66 0x08 for brightness

        //start reading thread
        if ((null == mUsbEndpointIn)) {
            mUsbInterface = null;
        } else {
            mUsbInterface = usbInterface;
            mUsbDeviceConnection = mUsbManager.openDevice(device);
            if (!isReading) {
                startReading();
            }
        }
    }

    public static float byte2float(byte[] b, int index) {
        return Float.intBitsToFloat(byte2int(b, index));
    }

    public static int byte2int(byte[] b, int index) {
        int l;
        l = b[index];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);
        return l;
    }

    private static boolean sendUsbCommand(int command, int value) {
        Log.d(TAG, "sendUsbCommand: " + command);
        if (mUsbDeviceConnection == null || mUsbEndpointOut == null) {
            return false;
        }
        byte[] bytes = new byte[mUsbEndpointOut.getMaxPacketSize()];
        bytes[0] = 0x66;
        bytes[1] = (byte) command;
        bytes[2] = 0;
        bytes[3] = 12;
        bytes[4] = 0;
        bytes[5] = 0;
        bytes[6] = 0;
        bytes[7] = 0;
        bytes[8] = (byte) value;
        bytes[9] = 0;
        bytes[10] = (byte) (0x77 + command + 12 + value);
        bytes[11] = 0x77;


        return mUsbDeviceConnection.bulkTransfer(mUsbEndpointOut, bytes, mUsbEndpointOut.getMaxPacketSize(), 1000) > 0;
    }

    //Open a thread to read data
    private synchronized static void startReading() {
        mUsbDeviceConnection.claimInterface(mUsbInterface, true);

        isReading = true;


        mReadingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //send command to notify mcu to send data
                if (!sendUsbCommand(1, 0)) {
                    return;
                }

                while (isReading) {
                    synchronized (this) {
                        int ret;
                        int count = mRemoteCallbackList.beginBroadcast();
                        if (count == 0) {
//                                isReading = false;

                            mRemoteCallbackList.finishBroadcast();
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            continue;
                        }

                        byte[] bytes = new byte[mUsbEndpointIn.getMaxPacketSize()];
                        ret = mUsbDeviceConnection.bulkTransfer(mUsbEndpointIn, bytes, bytes.length, 100);
//                            Log.d(TAG, "run: ret = " + ret + " " + mUsbEndpointIn.getMaxPacketSize());
                        if (ret > 0) {
                            String sensorData = null;
                            try {
                                sensorData = new String(bytes, "ISO-8859-1");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }

                            try {
                                for (int i = 0; i < count; i++) {
                                    if (ret == 12) {
                                        Log.e(TAG, "run: commandResp");
                                        mRemoteCallbackList.getBroadcastItem(i).commandResp(sensorData);
                                    } else {
                                        mRemoteCallbackList.getBroadcastItem(i).sensorEvent(sensorData);
//                                            Log.d(TAG, "run: send sensordata to " + i + "data = " + byte2float(bytes, 16) +
//                                                    " " + byte2float(bytes, 20) +
//                                                    " " + byte2float(bytes, 24));
//                                            Log.d(TAG, "run: send offset to " + i + "data = " + byte2float(bytes, 28) +
//                                                    " " + byte2float(bytes, 32) +
//                                                    " " + byte2float(bytes, 36));
                                    }
                                }
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            } finally {
                                mRemoteCallbackList.finishBroadcast();
                            }
                        } else {
                            mRemoteCallbackList.finishBroadcast();
                            Log.d(TAG, "run: " + sendUsbCommand(1, 0));
                        }
                    }
                }

                sendUsbCommand(2, 0);
                //mUsbDeviceConnection.close();
            }
        });

        mReadingThread.start();
    }

    //Close USB service
    private void closeUsbService() {
        if (isReading) {
            isReading = false;
        }

        mUsbEndpointIn = null;
        mUsbEndpointOut = null;
        mUsbDeviceConnection = null;

        Log.d(TAG, "closeUsbService: ");
    }

    public UsbService() {
    }

    @Override
    protected IBinder initBinder() {
        if (mBinder == null) {
            mBinder = new RemoteCallBinder();
        }
        return mBinder;
    }

    private static final class RemoteCallBinder extends IUsbCallbackApi.Stub {
        @Override
        public void sendCommand(String command) {
            if (mUsbDeviceConnection == null || mUsbEndpointOut == null) {
                return;
            }

            byte[] cmd;
            try {
                cmd = command.getBytes("ISO-8859-1");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return;
            }
            Log.d(TAG, "sendCommand: " + cmd[0] + " " + cmd[1]);
            mUsbDeviceConnection.bulkTransfer(mUsbEndpointOut, cmd, cmd.length, 100);
        }

        @Override
        public void registerListener(IUsbCallback callBack) {
            Log.e(TAG, "registerListener: ");
            if (null != mRemoteCallbackList) {
                mRemoteCallbackList.register(callBack);
            }

//            if (mUsbDeviceConnection != null && !isReading) {
//                startReading();
//            }
        }

        @Override
        public void unRegisterListener(IUsbCallback callBack) {
            Log.d(TAG, "unRegisterListener: ");
            if (null != mRemoteCallbackList) {
                mRemoteCallbackList.unregister(callBack);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Resister attached or detached broadcast
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, usbFilter);


        //Resister permission broadcast
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_DEVICE_PERMISSION), 0);
        IntentFilter permissionFilter = new IntentFilter(ACTION_DEVICE_PERMISSION);
        registerReceiver(mUsbPermissionReceiver, permissionFilter);

        searchUsb();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbReceiver);
        unregisterReceiver(mUsbPermissionReceiver);
    }
}
