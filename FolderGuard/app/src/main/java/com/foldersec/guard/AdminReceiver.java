package com.foldersec.guard;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

/** 시스템이 잠금화면 인증 실패/성공을 알려주는 수신기. 상시 서비스가 필요 없어 배터리 소모가 거의 없다. */
public class AdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        int n = Prefs.incFail(context);
        if (Prefs.enabled(context) && n >= Prefs.THRESHOLD) {
            IntruderService.trigger(context, false);
        }
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        Prefs.resetFail(context);
        context.stopService(new Intent(context, IntruderService.class));
    }
}
