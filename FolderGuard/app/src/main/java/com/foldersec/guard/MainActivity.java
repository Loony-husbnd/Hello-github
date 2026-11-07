package com.foldersec.guard;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
        root.addView(button("2. 카메라 권한 허용", new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23) requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            }
        }));

        CheckBox enabled = new CheckBox(this);
        enabled.setText("보안 기능 사용 (" + Prefs.THRESHOLD + "회 이상 실패 시)");
        enabled.setChecked(Prefs.enabled(this));
        enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton c, boolean on) { Prefs.setEnabled(MainActivity.this, on); }
        });
        root.addView(enabled);

        root.addView(button("3. 테스트 (사이렌 + 사진 5장)", new View.OnClickListener() {
            @Override public void onClick(View v) { IntruderService.trigger(MainActivity.this, true); }
        }));
        root.addView(button("사이렌 중지", new View.OnClickListener() {
            @Override public void onClick(View v) { stopService(new Intent(MainActivity.this, IntruderService.class)); }
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
        boolean cam = Build.VERSION.SDK_INT < 23
                || checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        status.setText("기기 관리자: " + (adm ? "켜짐 ✔" : "꺼짐 ✘")
                + "\n카메라 권한: " + (cam ? "허용 ✔" : "필요 ✘")
                + "\n" + (adm && cam ? "준비 완료. 이제 보호 중입니다." : "위 항목을 순서대로 설정하세요."));
    }

    private Button button(String text, View.OnClickListener l) {
        Button bt = new Button(this);
        bt.setText(text);
        bt.setOnClickListener(l);
        return bt;
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}
