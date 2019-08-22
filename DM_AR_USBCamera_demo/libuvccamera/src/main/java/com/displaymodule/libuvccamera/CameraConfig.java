package com.displaymodule.libuvccamera;

public class CameraConfig {
    private int mVendorId = 0; // filter vid, if 0, no filter
    private int mProductId = 0; // filter pid, if 0, no filter

    public int getVendorId() {
        return mVendorId;
    }

    public CameraConfig setVendorId(int mVendorId) {
        this.mVendorId = mVendorId;
        return this;
    }

    public int getProductId() {
        return mProductId;
    }

    public CameraConfig setProductId(int mProductId) {
        this.mProductId = mProductId;
        return this;
    }
}
