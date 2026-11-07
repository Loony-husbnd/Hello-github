package com.foldersec.guard;

import android.content.Context;
import android.content.SharedPreferences;

/** 설정/실패 횟수 저장소. */
final class Prefs {
    static final int THRESHOLD = 5;          // 이 횟수 이상 실패 시 작동
    static final long SIREN_MS = 60_000L;    // 사이렌 최대 지속 시간
    static final int PHOTO_COUNT = 5;
    static final int MAX_KEEP = 50;          // 보관할 최대 사진 수

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("guard", Context.MODE_PRIVATE);
    }

    static boolean enabled(Context c) { return sp(c).getBoolean("enabled", true); }
    static void setEnabled(Context c, boolean v) { sp(c).edit().putBoolean("enabled", v).apply(); }

    static int incFail(Context c) {
        SharedPreferences p = sp(c);
        int n = p.getInt("fail", 0) + 1;
        p.edit().putInt("fail", n).apply();
        return n;
    }

    static void resetFail(Context c) { sp(c).edit().putInt("fail", 0).apply(); }
}
