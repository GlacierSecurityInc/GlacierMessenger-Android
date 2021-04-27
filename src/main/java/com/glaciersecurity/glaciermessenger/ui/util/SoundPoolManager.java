package com.glaciersecurity.glaciermessenger.ui.util;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.glaciersecurity.glaciermessenger.R;

import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;

public class SoundPoolManager {

    private boolean playing = false;
    private boolean loaded = false;
    private boolean playingCalled = false;
    private float volume;
    private SoundPool soundPool;
    private int ringingSoundId;
    private int ringingStreamId;
    private int disconnectSoundId;
    private int joingSoundId;

    private boolean speaker = false;
    private int audioMode;

    Ringtone ringtone;

    private static SoundPoolManager instance;

    private AudioManager audioManager;
    private Vibrator vibrator;

    private SoundPoolManager(Context context) {
        // AudioManager audio settings for adjusting the volume
        audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume = actualVolume / maxVolume;

        vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);

        Uri soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"+ context.getPackageName() + "/" + R.raw.outgoing_ring);
        ringtone = RingtoneManager.getRingtone(context,soundUri);

        // Load the sounds
        int maxStreams = 2;
        soundPool = new SoundPool.Builder()
                .setMaxStreams(maxStreams)
                .build();

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                loaded = true;
                if (playingCalled) {
                    playRinging();
                    playingCalled = false;
                }
            }

        });
        ringingSoundId = soundPool.load(context, R.raw.outgoing_ring, 1);
        disconnectSoundId = soundPool.load(context, R.raw.disconnect_end_call, 1);
        joingSoundId = soundPool.load(context, R.raw.join_call, 1);
    }

    public static SoundPoolManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundPoolManager(context);
        }
        return instance;
    }

    public void playRinging() {
        if (loaded && !playing) {
            ringtone.play();
            vibrateIfNeeded();

            playing = true;
        } else {
            playingCalled = true;
        }
    }

    public void vibrateIfNeeded() {
        if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE ||
            audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            // Start without a delay, Vibrate for 1000 milliseconds, Sleep for 1000 milliseconds
            long[] pattern = {0, 1000, 1000};
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    public void stopRinging() {
        vibrator.cancel();
        if (playing) {
            soundPool.stop(ringingStreamId);
            if (ringtone != null) {
                ringtone.stop();
            }
            playing = false;
        }
    }

    public void playDisconnect() {
        if (loaded && !playing) {
            soundPool.play(disconnectSoundId, volume, volume, 1, 0, 1f);
            playing = false;
        }
        setSpeakerOn(false);
    }

    public void playJoin() {
        if (loaded && !playing) {
            soundPool.play(joingSoundId, volume, volume, 1, 0, 1f);
            playing = false;
        }
    }

    public void setSpeakerOn(boolean on) {
        speaker = on;
    }

    public boolean getSpeakerOn() {
        return speaker;
    }

    public void setPreviousAudioMode(int mode) {
        audioMode = mode;
    }

    public int getPreviousAudioMode() {
        return audioMode;
    }

    public void release() {
        if (soundPool != null) {
            soundPool.unload(ringingSoundId);
            soundPool.unload(disconnectSoundId);
            soundPool.unload(joingSoundId);
            soundPool.release();
            soundPool = null;
        }
        instance = null;
    }

}
