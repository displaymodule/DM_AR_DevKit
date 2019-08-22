package com.displaymodule.glasssensor.usb;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;


public abstract class AbstractService extends Service {
    protected IBinder mBinder;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (mBinder == null) {
            mBinder = initBinder();
        }
        return mBinder;
    }

    protected abstract IBinder initBinder();
}
