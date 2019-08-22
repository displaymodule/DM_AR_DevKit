package com.displaymodule.glasssensor.sensors;

import java.io.Serializable;

public class USBSensorData implements Serializable {
    private class UsbMessageHead{
        byte flag_s;
        byte message_type;
        short length;
    }

    /* Command Message */
    private class UsbCommandMessage{
        UsbMessageHead message_head;
        int uTick;
        byte value;
        byte reserve;
        byte checksum;
        byte flag_e;
    }

    /* Data Message */
    private class UsbDataMessage{
        float[] acc_data = new float[3];
        float[] gyro_data = new float[3];
        float[] mag_data = new float[3];
        int uTick;
    }

    private UsbMessageHead message_head;
    private UsbDataMessage data;
    private byte[] reserve = new byte[2];
    private byte checksum;
    private byte flag_e;
}
