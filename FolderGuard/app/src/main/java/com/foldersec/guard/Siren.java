package com.foldersec.guard;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * 알람 스트림 사이렌. 2초짜리 파형을 한 번만 만들어 MODE_STATIC으로 반복 재생하므로
 * 재생 중 CPU 사용이 거의 없다. 알람 스트림이라 일반 무음/진동 모드에서도 울린다.
 */
final class Siren {
    private static final int RATE = 16000;
    private final AudioManager am;
    private AudioTrack track;
    private int oldVolume = -1;

    Siren(Context c) {
        am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
    }

    void start() {
        try {
            oldVolume = am.getStreamVolume(AudioManager.STREAM_ALARM);
            am.setStreamVolume(AudioManager.STREAM_ALARM,
                    am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
        } catch (Exception ignored) {
            // 방해금지 모드 정책 등으로 거부될 수 있음 - 현재 볼륨으로 계속 진행
        }
        try {
            short[] pcm = build();
            track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(pcm.length * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();
            track.write(pcm, 0, pcm.length);
            track.setLoopPoints(0, pcm.length, -1);
            track.setVolume(1f);
            track.play();
        } catch (Exception e) {
            track = null;
        }
    }

    void stop() {
        if (track != null) {
            try { track.stop(); } catch (Exception ignored) { }
            track.release();
            track = null;
        }
        if (oldVolume >= 0) {
            try { am.setStreamVolume(AudioManager.STREAM_ALARM, oldVolume, 0); } catch (Exception ignored) { }
            oldVolume = -1;
        }
    }

    /** 900Hz~2200Hz를 2초 주기로 오르내리는 경보음 (배음 추가로 작은 스피커에서도 크게 들림). */
    private static short[] build() {
        int n = RATE * 2;
        short[] out = new short[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            double f = 900 + 1300 * (0.5 - 0.5 * Math.cos(2 * Math.PI * t));
            phase += 2 * Math.PI * f / RATE;
            double s = (Math.sin(phase) + 0.5 * Math.sin(3 * phase)) / 1.5;
            out[i] = (short) (s * 32000);
        }
        return out;
    }
}
