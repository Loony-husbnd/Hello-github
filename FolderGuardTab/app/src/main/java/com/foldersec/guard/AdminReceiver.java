package com.foldersec.guard;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

public class AdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        int n = Prefs.incFail(context);
        if (Prefs.enabled(context) && n >= Prefs.THRESHOLD) Guard.trigger(context);
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        Prefs.resetFail(context);
        Guard.stopAll(context);
    }
}
