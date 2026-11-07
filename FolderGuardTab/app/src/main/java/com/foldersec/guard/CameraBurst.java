package com.foldersec.guard;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * 셀카 카메라로 프리뷰 화면 없이 연속 촬영. 가볍고 구형 기기 호환성이 좋은 Camera1 API 사용.
 * 반드시 Looper가 있는 스레드(handler)에서 start()를 호출해야 콜백이 같은 스레드로 온다.
 */
final class CameraBurst {
    private final File dir;
    private final Handler handler;
    private final Runnable onDone;
    private Camera cam;
    private int taken = 0;
    private boolean finished = false;
    private final String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());

    CameraBurst(File dir, Handler handler, Runnable onDone) {
        this.dir = dir;
        this.handler = handler;
        this.onDone = onDone;
    }

    void start() {
        try {
            dir.mkdirs();
            Camera.CameraInfo info = new Camera.CameraInfo();
            int id = 0;
            int count = Camera.getNumberOfCameras();
            for (int i = 0; i < count; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) { id = i; break; }
            }
            Camera.getCameraInfo(id, info);
            cam = Camera.open(id);

            Camera.Parameters p = cam.getParameters();
            Camera.Size best = null;   // 1MP 근처의 작은 해상도: 메모리/속도 절약
            for (Camera.Size s : p.getSupportedPictureSizes()) {
                if (best == null || Math.abs(s.width * s.height - 1_000_000)
                        < Math.abs(best.width * best.height - 1_000_000)) best = s;
            }
            if (best != null) p.setPictureSize(best.width, best.height);
            p.setJpegQuality(75);
            p.setRotation(info.orientation);   // EXIF 회전
            cam.setParameters(p);

            cam.setPreviewTexture(new SurfaceTexture(10));
            cam.startPreview();
            handler.postDelayed(shoot, 700);   // 노출/초점 안정화 대기
        } catch (Exception e) {
            finish();
        }
    }

    private final Runnable shoot = new Runnable() {
        @Override public void run() {
            if (finished || cam == null) return;
            try {
                cam.takePicture(null, null, jpeg);
            } catch (Exception e) {
                finish();
            }
        }
    };

    private final Camera.PictureCallback jpeg = new Camera.PictureCallback() {
        @Override public void onPictureTaken(byte[] data, Camera c) {
            taken++;
            try (FileOutputStream fo = new FileOutputStream(
                    new File(dir, "intruder_" + stamp + "_" + taken + ".jpg"))) {
                fo.write(data);
            } catch (Exception ignored) { }
            if (taken >= Prefs.PHOTO_COUNT) {
                finish();
            } else {
                try {
                    c.startPreview();
                    handler.postDelayed(shoot, 200);   // 바바박
                } catch (Exception e) {
                    finish();
                }
            }
        }
    };

    void finish() {
        if (finished) return;
        finished = true;
        handler.removeCallbacks(shoot);
        if (cam != null) {
            try { cam.stopPreview(); } catch (Exception ignored) { }
            try { cam.release(); } catch (Exception ignored) { }
            cam = null;
        }
        prune();
        if (onDone != null) onDone.run();
    }

    /** 오래된 사진부터 삭제해 저장공간을 보호한다. */
    private void prune() {
        File[] fs = dir.listFiles();
        if (fs == null || fs.length <= Prefs.MAX_KEEP) return;
        Arrays.sort(fs);
        for (int i = 0; i < fs.length - Prefs.MAX_KEEP; i++) fs[i].delete();
    }
}
