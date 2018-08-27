package com.tcl.screenrecord;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class ScreenRecordService extends Service {

    private static final String TAG = ScreenRecordService.class.getSimpleName();

    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;
    private int mResultCode;
    private Intent mResultData;
    /**
     * 是否为标清视频
     */
    private boolean isVideoSd;
    /**
     * 是否开启音频录制
     */
    private boolean isAudio;

    private MediaProjection mMediaProjection;
    private MediaRecorder mMediaRecorder;
    private VirtualDisplay mVirtualDisplay;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service onCreate() is called");
        ScreenRecordApp.isStarted = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service onStartCommand() is called");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelId = "001";
            String channelName = "myChannel";
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Notification notification;

                notification = new Notification.
                        Builder(getApplicationContext(), channelId).setOngoing(true).setSmallIcon(R.mipmap.ic_launcher).setCategory(Notification.CATEGORY_SERVICE).build();

                startForeground(101, notification);
            }
        } else {
            startForeground(101, new Notification());
        }
        mResultCode = intent.getIntExtra("code", -1);
        mResultData = intent.getParcelableExtra("data");
        mScreenWidth = intent.getIntExtra("width", 720);
        mScreenHeight = intent.getIntExtra("height", 1280);
        mScreenDensity = intent.getIntExtra("density", 1);
        isVideoSd = intent.getBooleanExtra("quality", true);
        isAudio = intent.getBooleanExtra("audio", true);

        mMediaProjection = createMediaProjection();
        mMediaRecorder = createMediaRecorder();
        // 必须在mediaRecorder.prepare() 之后调用，否则报错"fail to get surface"
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
        ScreenRecordApp.isAudio = isAudio;
        ScreenRecordApp.isVideoSd = isVideoSd;
        return Service.START_STICKY;
    }

    private MediaProjection createMediaProjection() {
        Log.i(TAG, "Create MediaProjection");
        return ((MediaProjectionManager) Objects.requireNonNull(getSystemService(Context.MEDIA_PROJECTION_SERVICE))).
                getMediaProjection(mResultCode, mResultData);
    }

    private MediaRecorder createMediaRecorder() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        Date curDate = new Date(System.currentTimeMillis());
        String curTime = formatter.format(curDate).replace(" ", "");
        String videoQuality = "HD";
        if (isVideoSd) videoQuality = "SD";

        Log.i(TAG, "Create MediaRecorder");
        MediaRecorder mediaRecorder = new MediaRecorder();
        if (isAudio) mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(Environment.
                getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/" + videoQuality + curTime + ".mp4");
        mediaRecorder.setVideoSize(mScreenWidth, mScreenHeight);  //after setVideoSource(), setOutFormat()
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);  //after setOutputFormat()
        if (isAudio)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);  //after setOutputFormat()
        int bitRate;
        if (isVideoSd) {
            mediaRecorder.setVideoEncodingBitRate(mScreenWidth * mScreenHeight);
            mediaRecorder.setVideoFrameRate(30);
            bitRate = mScreenWidth * mScreenHeight / 1000;
        } else {
            mediaRecorder.setVideoEncodingBitRate(5 * mScreenWidth * mScreenHeight);
            mediaRecorder.setVideoFrameRate(60); //after setVideoSource(), setOutFormat()
            bitRate = 5 * mScreenWidth * mScreenHeight / 1000;
        }
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException | IOException e) {
            Log.e(TAG, "createMediaRecorder: e = " + e.toString());
        }
        Log.i(TAG, "Audio: " + isAudio + ", SD video: " + isVideoSd + ", BitRate: " + bitRate + "kbps");

        return mediaRecorder;
    }

    private VirtualDisplay createVirtualDisplay() {
        Log.i(TAG, "Create VirtualDisplay");
        return mMediaProjection.createVirtualDisplay(TAG, mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service onDestroy");
        ScreenRecordApp.isStarted = false;
        stopForeground(true);
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener(null);
            mMediaProjection.stop();
            mMediaRecorder.reset();
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
