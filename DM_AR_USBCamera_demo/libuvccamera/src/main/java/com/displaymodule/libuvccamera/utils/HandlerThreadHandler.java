package com.displaymodule.libuvccamera.utils;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HandlerThreadHandler extends Handler {
    private static final String TAG = "HandlerThreadHandler";

    public static final HandlerThreadHandler createHandler() {
        return createHandler(TAG);
    }

    public static final HandlerThreadHandler createHandler(final String name) {
        final HandlerThread thread = new HandlerThread(name);
        thread.start();
        return new HandlerThreadHandler(thread.getLooper());
    }

    public static final HandlerThreadHandler createHandler(@Nullable final Callback callback) {
        return createHandler(TAG, callback);
    }

    public static final HandlerThreadHandler createHandler(final String name, @Nullable final Callback callback) {
        final HandlerThread thread = new HandlerThread(name);
        thread.start();
        return new HandlerThreadHandler(thread.getLooper(), callback);
    }

    private final long mId;
    private HandlerThreadHandler(@NonNull final Looper looper) {
        super(looper);
        final Thread thread = looper.getThread();
        mId = thread != null ? thread.getId() : 0;
    }

    private HandlerThreadHandler(@NonNull final Looper looper, @Nullable final Callback callback) {
        super(looper, callback);
        final Thread thread = looper.getThread();
        mId = thread != null ? thread.getId() : 0;
    }

    public long getId() {
        return mId;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void quitSafely() throws IllegalStateException {
        final Looper looper = getLooper();
        if (looper != null) {
            looper.quitSafely();
        } else {
            throw new IllegalStateException("has no looper");
        }
    }

    public void quit() throws IllegalStateException {
        final Looper looper = getLooper();
        if (looper != null) {
            looper.quit();
        } else {
            throw new IllegalStateException("has no looper");
        }
    }

    public boolean isCurrentThread() throws IllegalStateException {
        final Looper looper = getLooper();
        if (looper != null) {
            return mId == Thread.currentThread().getId();
        } else {
            throw new IllegalStateException("has no looper");
        }
    }
}


