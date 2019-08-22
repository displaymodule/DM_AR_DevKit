# DM_AR_sensor_display_demo
A simple sample to show how to control DM AR glasses and access the Sensor


## Version Notes
Drive board version 1.0

## API Notes
AR Glass development kit get sensor data and control display via USB communication with STM32.

V1.0 version's USB device vendor id is 0x0533,

Product id is 0x3333

## Sensor Demo
we show an example how to get Gyroscope data and acordingly render the cube rotation (see CalibratedGyroscopeProvider.java).

In the same time, we also provide accelerometer and compass sensor fusion and render the cube (see AccelerometerCompassProvider.java)

***NOTE:***
Change currentOrientationProvider from CalibratedGyroscopeProvider to AccelerometerCompassProvider at CubeActivity.java, you can using accelerometer and compass sensor fusion.

The default is using CalibratedGyroscopeProvider.

```java
currentOrientationProvider = new AccelerometerCompassProvider(getApplicationContext(), mGlassSensor);
currentOrientationProvider = new CalibratedGyroscopeProvider(getApplicationContext(), mGlassSensor);
```
## How to get sensor data?

1.	The only thing you need care is the Class USBSensorCallback.

```java
USBSensorCallback usbSensor = new USBSensorCallback();
```

2.	Register USBSensorEventListener to USBSensorCallback.
```java
usbSensor.register(getApplicationContext(), (accSensorData, gyroSensorData, magSensorData) -> {
    //TODO some sensor data processing here
});
```

3.	Call USBSensorCallback start.
```java
usbSensor.start();
```
4.	If you want to stop glass report sensor data call USBSensorCallback stop.
```java
usbSensor.stop();
```
NOTE: make sure you call usbSensor.stop(); on activity destroy.


## Display Demo
In display control demo, we show how to turn on / off the glass display, change brightness, change orientation.

## How to control glass display
1.	Same as sensor data fetch, the only thing you need care is the class
```java
USBSensorCallback
USBSensorCallback mDisplay = new USBSensorCallback();
```
2.	Register a nothing todo USBSensorEventListener to USBSensorCallback.
```java
mDisplay.register(getApplicationContext(), (accSensorData, gyroSensorData, magSensorData) -> {});
```
3.	Call USBSensorCallback start.
```java
mDisplay.start();
```
4.	Call display control function. example turn off display:
```java
mDisplay.turnOffGlassScreen();//There is issue of V1.0 version
```
5.	if you exit an control activity, make sure call USBSensorCallback stop.
```java
mDisplay.stop();
```

## Available display control API
1.	Turn on Glass display, the glass turn on the display by default.
```java
public void turnOnGlassScreen();
```
2.	Turn off Glass display.
```java
public void turnOffGlassScreen();
```
3.	Change display orientation. left/right/top/bottom mirror The rotation only accept 0-3.
```java
public void setRotation(byte rotation);
```

|rotation value|mirror orientation|
|-|--
|0|left/right|
|1|left/right|
|2|top/bottom|
|3|top/bottom|

4.	Change preset brightness. The level value only accept 0-4
```java
public void setPresetBrightness(byte level);
```
|Level value | Luminance|
|--| --
|0|500|
1|120
2|300
3|1500
4|3000

5.	Change instant brightness. The level value accept 0-255.
```java
public void setInstantLuminance(byte value);
```
