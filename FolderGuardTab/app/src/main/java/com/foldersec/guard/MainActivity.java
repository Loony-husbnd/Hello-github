package com.foldersec.guard;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.app.NotificationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 설정 화면. 코드로 UI를 구성해 리소스/라이브러리 로딩을 최소화했다. */
public class MainActivity extends Activity {
    private TextView status;
    private ComponentName admin;
    private DevicePolicyManager dpm;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        admin = new ComponentName(this, AdminReceiver.class);
        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        status = new TextView(this);
        status.setTextSize(15);
        root.addView(status);

        root.addView(button("1. 기기 관리자 권한 켜기", new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "잠금 해제 실패 횟수를 감지하기 위해 필요합니다.");
                startActivity(i);
            }
        }));
        root.addView(button("2. 카메라·알림 권한 허용", new View.OnClickListener() {
            @Override public void onClick(View v) {
                requestPermissions(new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }));
        root.addView(button("3. '다른 앱 위에 표시' 허용", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            }
        }));
        root.addView(button("4. 전체화면 알림 허용 (안드로이드 14)", new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 34) {
                    startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:" + getPackageName())));
                }
            }
        }));

        CheckBox enabled = new CheckBox(this);
        enabled.setText("보안 기능 사용 (" + Prefs.THRESHOLD + "회 이상 실패 시)");
        enabled.setChecked(Prefs.enabled(this));
        enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton c, boolean on) { Prefs.setEnabled(MainActivity.this, on); }
        });
        root.addView(enabled);

        root.addView(button("5. 테스트 (사이렌 + 사진 5장)", new View.OnClickListener() {
            @Override public void onClick(View v) { Guard.trigger(MainActivity.this); }
        }));
        root.addView(button("사이렌 중지", new View.OnClickListener() {
            @Override public void onClick(View v) { Guard.stopAll(MainActivity.this); }
        }));
        root.addView(button("촬영된 사진 보기", new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(MainActivity.this, GalleryActivity.class)); }
        }));

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean adm = dpm.isAdminActive(admin);
        boolean cam = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean ovl = Settings.canDrawOverlays(this);
        boolean fsi = Build.VERSION.SDK_INT < 34
                || ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).canUseFullScreenIntent();
        status.setText("기기 관리자: " + mark(adm) + "
카메라 권한: " + mark(cam)
                + "
다른 앱 위에 표시: " + mark(ovl) + "
전체화면 알림: " + mark(fsi)
                + "
" + (adm && cam && (ovl || fsi) ? "준비 완료. 이제 보호 중입니다." : "위 항목을 순서대로 설정하세요."));
    }

    private static String mark(boolean ok) { return ok ? "켜짐 ✔" : "필요 ✘"; }

    private Button button(String text, View.OnClickListener l) {
        Button bt = new Button(this);
        bt.setText(text);
        bt.setOnClickListener(l);
        return bt;
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}
