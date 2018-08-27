package com.tcl.screenrecord;

import android.app.Application;
import android.util.Log;

/**
 * Created by ZhangJun on 2018/8/27.
 */
public class ScreenRecordApp extends Application {

    private static final String TAG = ScreenRecordApp.class.getSimpleName();
    /**
     * 是否已经开启视频录制
     */
    public static boolean isStarted = false;
    /**
     * 是否为标清视频
     */
    public static boolean isVideoSd = true;
    /**
     * 是否开启音频录制
     */
    public static boolean isAudio = true;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
    }
}
