package com.foldersec.guard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;

/** 경보 중 프로세스가 종료되지 않도록 붙잡아 두는 포그라운드 서비스 (AlarmActivity가 시작). */
public class KeepAliveService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(
                new NotificationChannel("guard", "보안 경보 유지", NotificationManager.IMPORTANCE_LOW));
        Notification n = new Notification.Builder(this, "guard")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("폴더 가드 작동 중")
                .build();
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        return START_NOT_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
