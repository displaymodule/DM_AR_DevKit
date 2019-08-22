// IRemoteCallbackApi.aidl
package com.displaymodule.glasssensor.usbservice;

// Declare any non-default types here with import statements
import com.displaymodule.glasssensor.usbservice.IUsbCallback;

interface IUsbCallbackApi {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void sendCommand(String command);
    void registerListener(IUsbCallback callBack);
    void unRegisterListener(IUsbCallback callBack);
}
