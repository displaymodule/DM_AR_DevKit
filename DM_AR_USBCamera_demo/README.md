
# DM_AR_USBCamera_demo
A simple sample to show how to control DM AR glasses's USB Camera and preview on display


## API Notes
AR Glass development kit camera is a pure UVC (USB video class) camera.
The USB device vendor id is 0x0EDC, product id is 0x3080

Developer can ref to the open source project UVCCamera from
(github: https://github.com/saki4510t/UVCCamera)

In our camera demo, we just show a simple camera preview on Glass.
We do not use USB device filter because the kit camera is a standard UVC Camera


## The process to get camera preview work on glass as follows:
1.	new USBCameraListener, with onCameraOpened, onPreview, onCameraClosed, onCameraError, onCameraConfigurationChanged.

```java
USBCameraListener usbcameraListener = new USBCameraListener() {
            @Override
            public void onCameraOpened(USBCamera camera, int cameraId, int displayOrientation, boolean isMirror) {
                Log.i(TAG, "onCameraOpened");
            }

            @Override
            public void onPreview(byte[] data) {
                Log.i(TAG, "onPreview");
                //todo something, like facial recoginition algorithm
            }

            @Override
            public void onCameraClosed() {
                Log.i(TAG, "onCameraClosed");
                //todo something
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i(TAG, "onCameraError: " + e.getMessage());
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                Log.i(TAG, "onCameraConfigurationChanged");
                //todo something
            }
        };
```
2.new USBCameraHelper, then call init(), start()
```java
    usbcameraHelper = new USBCameraHelper.Builder()
               .Context(this.getApplication())
               .previewViewSize(new Point(mPreviewView.getMeasuredWidth(), mPreviewView.getMeasuredHeight()))
               .rotation(getWindowManager().getDefaultDisplay().getRotation())
               .isMirror(false)
               .previewOn(mPreviewView)
               .cameraListener(usbcameraListener)
               .build();
       usbcameraHelper.init();
       usbcameraHelper.start();
```
## Use com.displaymodule.libuvccamera.usb.UsbMonitor and com.displaymodule.libuvccamera.UVCCamera for application development.
1.implement ConnectCallback for UsbMonitor
```java
public interface ConnectCallback {
    /**
     * attach the device
     * @param usbDevice
     */
    void onAttached(UsbDevice usbDevice);
    /**
     * USB device grant permission
     * @param usbDevice
     * @param granted   permission granted or not
     */
    void onGranted(UsbDevice usbDevice, boolean granted);

    /**
     * on device connected successfully
     * @param usbDevice
     */
    void onConnected(UsbDevice usbDevice);

    /**
     * camera open successfully
     */
    void onCameraOpened();

    /**
     * device detached.
     * @param usbDevice
     */
    void onDetached(UsbDevice usbDevice);
}
```
2.use UsbMonitor.setConnectCallback() to set implemented callback.

3.control UVCCamera, like startPreview/stopPreview, setSharpness/getSharpness, getGamma/setGamma, getZoom/setZoom, startCapture/stopCapture etc.
