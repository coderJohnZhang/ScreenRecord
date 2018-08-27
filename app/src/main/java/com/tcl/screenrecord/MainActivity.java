package com.tcl.screenrecord;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.view.KeyEvent;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView mTextView;

    private static final String RECORD_STATUS = "record_status";
    private static final int REQUEST_CODE = 1000;

    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = PERMISSION_REQ_ID_RECORD_AUDIO + 1;
    private boolean isPermissible = false;

    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;

    /**
     * 是否已经开启视频录制
     */
    private boolean isStarted = ScreenRecordApp.isStarted;
    /**
     * 是否为标清视频
     */
    private boolean isVideoSd = ScreenRecordApp.isVideoSd;
    /**
     * 是否开启音频录制
     */
    private boolean isAudio = ScreenRecordApp.isAudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO) && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE)) {
            isPermissible = true;
        }
        Log.d(TAG, "onCreate: isPermissible = " + isPermissible);
        if (savedInstanceState != null) {
            isStarted = savedInstanceState.getBoolean(RECORD_STATUS);
        }
        getView();
        getScreenBaseInfo();
    }

    public boolean checkSelfPermission(String permission, int requestCode) {
        Log.d(TAG, "checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult " + grantResults[0] + " " + requestCode);
        switch (requestCode) {
            case PERMISSION_REQ_ID_RECORD_AUDIO:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
                } else {
                    isPermissible = false;
                    showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO);
                    finish();
                }
                break;
            case PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: isPermissible = " + isPermissible);
                    isPermissible = true;
                } else {
                    isPermissible = false;
                    showLongToast("No permission for " + Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    finish();
                }
                break;
            default:
                break;
        }
    }

    public final void showLongToast(final String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    private void getView() {
        mTextView = (TextView) findViewById(R.id.button_control);
        if (isStarted) {
            statusIsStarted();
        } else {
            statusIsStopped();
        }
        mTextView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isStarted) {
                    stopScreenRecording();
                    statusIsStopped();
                    Log.i(TAG, "Stopped screen recording");
                } else if (isPermissible) {
                    startScreenRecording();
                }
            }
        });

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radio_group);
        radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.sd_button:
                        isVideoSd = true;
                        break;
                    case R.id.hd_button:
                        isVideoSd = false;
                        break;

                    default:
                        break;
                }
            }
        });

        CheckBox audioBox = (CheckBox) findViewById(R.id.audio_check_box);
        audioBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isAudio = isChecked;
            }
        });
    }

    /**
     * 开启屏幕录制时的UI状态
     */
    private void statusIsStarted() {
        mTextView.setText(R.string.stop_recording);
    }

    /**
     * 结束屏幕录制后的UI状态
     */
    private void statusIsStopped() {
        mTextView.setText(R.string.start_recording);
    }

    /**
     * 获取屏幕相关数据
     */
    private void getScreenBaseInfo() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
        mScreenDensity = metrics.densityDpi;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(RECORD_STATUS, isStarted);
    }

    /**
     * 获取屏幕录制的权限
     */
    private void startScreenRecording() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
        startActivityForResult(permissionIntent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // 获得权限，启动Service开始录制
                Intent service = new Intent(this, ScreenRecordService.class);
                service.putExtra("code", resultCode);
                service.putExtra("data", data);
                service.putExtra("audio", isAudio);
                service.putExtra("width", mScreenWidth);
                service.putExtra("height", mScreenHeight);
                service.putExtra("density", mScreenDensity);
                service.putExtra("quality", isVideoSd);
                startService(service);
                // 已经开始屏幕录制，修改UI状态
                isStarted = !isStarted;
                statusIsStarted();
                Log.i(TAG, "Started screen recording");
            } else {
                Toast.makeText(this, R.string.user_cancelled, Toast.LENGTH_LONG).show();
                Log.i(TAG, "User cancelled");
            }
        }
    }

    /**
     * 关闭屏幕录制，即停止录制Service
     */
    private void stopScreenRecording() {
        Intent service = new Intent(this, ScreenRecordService.class);
        stopService(service);
        isStarted = !isStarted;
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: ");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
            moveTaskToBack(true);//true对任何Activity都适用
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
