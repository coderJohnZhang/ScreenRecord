package com.tcl.screenrecord;

import android.app.Application;
import android.util.Log;

/**
 * Created by ZhangJun on 2018/8/27.
 */
public class ScreenRecordApp extends Application {

    private static final String TAG = ScreenRecordApp.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
    }
}
