package com.foldersec.guard;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

/** 촬영된 사진 목록(최신순, 최대 20장, 축소 디코딩으로 메모리 절약). */
public class GalleryActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16);

        File[] fs = IntruderService.photoDir(this).listFiles();
        if (fs == null || fs.length == 0) {
            TextView t = new TextView(this);
            t.setText("저장된 사진이 없습니다.");
            root.addView(t);
        } else {
            Arrays.sort(fs, Collections.reverseOrder());
            for (int i = 0; i < Math.min(fs.length, 20); i++) {
                TextView name = new TextView(this);
                name.setText(fs[i].getName());
                root.addView(name);
                ImageView iv = new ImageView(this);
                iv.setAdjustViewBounds(true);
                Bitmap bm = load(fs[i]);
                if (bm != null) iv.setImageBitmap(bm);
                root.addView(iv);
            }
        }
        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    private Bitmap load(File f) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 4;
            Bitmap bm = BitmapFactory.decodeFile(f.getPath(), o);
            if (bm == null) return null;
            int deg;
            switch (new ExifInterface(f.getPath()).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                case ExifInterface.ORIENTATION_ROTATE_90: deg = 90; break;
                case ExifInterface.ORIENTATION_ROTATE_180: deg = 180; break;
                case ExifInterface.ORIENTATION_ROTATE_270: deg = 270; break;
                default: deg = 0;
            }
            if (deg == 0) return bm;
            Matrix m = new Matrix();
            m.postRotate(deg);
            return Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }
}
