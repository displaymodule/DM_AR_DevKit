// IRemoteCallback.aidl
package com.displaymodule.glasssensor.usbservice;
// Declare any non-default types here with import statements

interface IUsbCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void sensorEvent(String data);
    void commandResp(String resp);
}
