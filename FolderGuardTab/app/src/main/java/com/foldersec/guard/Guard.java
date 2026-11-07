package com.foldersec.guard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;

/**
 * 경보 총괄. 사이렌은 수신기에서 즉시 울리고(오디오는 백그라운드에서도 가능),
 * 카메라는 안드로이드 10+ 제한 때문에 잠금화면 위에 AlarmActivity를 띄워 촬영한다.
 */
final class Guard {
    private static final int NOTI_ID = 2;
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static Siren siren;
    private static PowerManager.WakeLock wl;
    private static Context app;

    private static final Runnable timeout = new Runnable() {
        @Override public void run() { if (app != null) stopAll(app); }
    };

    static synchronized void trigger(Context c) {
        app = c.getApplicationContext();
        if (siren == null) {
            siren = new Siren(app);
            siren.start();
            wl = ((PowerManager) app.getSystemService(Context.POWER_SERVICE))
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "foldersec:guard");
            wl.acquire(Prefs.SIREN_MS + 15_000L);        // 타임아웃 있는 웨이크락
            main.postDelayed(timeout, Prefs.SIREN_MS);
        }
        launchCamera(app);
    }

    private static void launchCamera(Context a) {
        Intent i = new Intent(a, AlarmActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        // 1) "다른 앱 위에 표시" 권한이 있으면 바로 실행
        if (Settings.canDrawOverlays(a)) {
            try { a.startActivity(i); } catch (Exception ignored) { }
        }
        // 2) 백업: 전체화면 알림 (잠금/화면 꺼짐 상태에서 시스템이 대신 실행)
        try {
            NotificationManager nm = (NotificationManager) a.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(
                    "alert", "보안 경보", NotificationManager.IMPORTANCE_HIGH));
            PendingIntent pi = PendingIntent.getActivity(a, 0, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification n = new Notification.Builder(a, "alert")
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("잠금 해제 실패 감지")
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setFullScreenIntent(pi, true)
                    .setAutoCancel(true)
                    .build();
            nm.notify(NOTI_ID, n);
        } catch (Exception ignored) { }
    }

    static synchronized void stopAll(Context c) {
        Context a = c.getApplicationContext();
        main.removeCallbacks(timeout);
        if (siren != null) { siren.stop(); siren = null; }
        if (wl != null && wl.isHeld()) wl.release();
        wl = null;
        a.stopService(new Intent(a, KeepAliveService.class));
        AlarmActivity.finishIfOpen();
        try {
            ((NotificationManager) a.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTI_ID);
        } catch (Exception ignored) { }
    }
}
