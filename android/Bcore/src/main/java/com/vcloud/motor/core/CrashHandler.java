package com.vcloud.motor.core;

import com.vcloud.motor.VcloudCore;


public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    public static void create() {
        new CrashHandler();
    }

    public CrashHandler() {
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (VcloudCore.get().getExceptionHandler() != null) {
            VcloudCore.get().getExceptionHandler().uncaughtException(t, e);
        }
        mDefaultHandler.uncaughtException(t, e);
    }
}
