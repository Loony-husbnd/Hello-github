package com.foldersec.guard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;

import java.io.File;

/** 사이렌 + 연속 촬영. 이벤트가 있을 때만 잠깐 실행되고 끝나면 스스로 종료한다. */
public class IntruderService extends Service {
    private HandlerThread thread;
    private Handler handler;
    private Siren siren;
    private PowerManager.WakeLock wakeLock;
    private boolean bursting;
    private volatile CameraBurst burst;

    static void trigger(Context c, boolean test) {
        Intent i = new Intent(c, IntruderService.class);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }

    static File photoDir(Context c) {
        return new File(c.getFilesDir(), "intruders");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, buildNotification());

        if (thread == null) {
            thread = new HandlerThread("guard");
            thread.start();
            handler = new Handler(thread.getLooper());
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "foldersec:guard");
            wakeLock.acquire(Prefs.SIREN_MS + 15_000L);   // 타임아웃 있는 웨이크락
        }
        if (siren == null) {
            siren = new Siren(this);
            siren.start();
            handler.postDelayed(new Runnable() {
                @Override public void run() { stopSelf(); }
            }, Prefs.SIREN_MS);
        }
        if (!bursting) {
            bursting = true;
            handler.post(new Runnable() {
                @Override public void run() {
                    burst = new CameraBurst(photoDir(IntruderService.this), handler, new Runnable() {
                        @Override public void run() { bursting = false; }
                    });
                    burst.start();
                }
            });
        }
        return START_NOT_STICKY;
    }

    private Notification buildNotification() {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(
                    "guard", "보안 경보", NotificationManager.IMPORTANCE_LOW));
            b = new Notification.Builder(this, "guard");
        } else {
            b = new Notification.Builder(this);
        }
        return b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("폴더 가드 작동 중")
                .setContentText("잠금 해제 실패 감지")
                .build();
    }

    @Override
    public void onDestroy() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            final CameraBurst b = burst;
            if (b != null) handler.post(new Runnable() {
                @Override public void run() { b.finish(); }   // 카메라 반드시 해제
            });
        }
        if (siren != null) siren.stop();
        if (thread != null) thread.quitSafely();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
