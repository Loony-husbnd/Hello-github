package com.foldersec.guard;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.File;

/** 검은 화면으로 잠금화면 위에 잠깐 떠서 사진 5장을 찍고 닫힌다 (보이는 앱이어야 카메라 사용 가능). */
public class AlarmActivity extends Activity {
    private static volatile AlarmActivity current;
    private HandlerThread ht;
    private Handler h;
    private volatile CameraBurst burst;

    static File photoDir(Context c) { return new File(c.getFilesDir(), "intruders"); }

    static void finishIfOpen() {
        final AlarmActivity a = current;
        if (a != null) a.runOnUiThread(new Runnable() { @Override public void run() { a.finish(); } });
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        current = this;
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout bg = new FrameLayout(this);
        bg.setBackgroundColor(Color.BLACK);
        setContentView(bg);

        startForegroundService(new Intent(this, KeepAliveService.class));

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            finish();
            return;
        }
        ht = new HandlerThread("burst");
        ht.start();
        h = new Handler(ht.getLooper());
        h.post(new Runnable() {
            @Override public void run() {
                burst = new CameraBurst(photoDir(AlarmActivity.this), h, new Runnable() {
                    @Override public void run() {
                        runOnUiThread(new Runnable() {
                            @Override public void run() { finish(); }
                        });
                    }
                });
                burst.start();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (current == this) current = null;
        if (h != null) {
            final CameraBurst bu = burst;
            if (bu != null) h.post(new Runnable() { @Override public void run() { bu.finish(); } });
            ht.quitSafely();
        }
        super.onDestroy();
    }
}
