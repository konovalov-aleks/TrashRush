package dev.av.konovalov.trashrush;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;
import java.util.HashMap;

public class SoundManager {
    private static final String TAG = "SoundManager";
    private SoundPool soundPool;
    private HashMap<Integer, Integer> soundMap;
    private boolean soundsEnabled = false;

    public static final int SOUND_CORRECT = 1;
    public static final int SOUND_WRONG = 2;
    public static final int SOUND_MISS = 3;
    public static final int SOUND_LEVEL_UP = 4;
    public static final int SOUND_GAME_OVER = 5;
    public static final int SOUND_CLICK = 6;

    public SoundManager(Context context) {
        initSoundPool();
        loadSounds(context);
    }

    private void initSoundPool() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(attributes)
                    .build();
        } else {
            soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        }

        soundMap = new HashMap<>();
    }

    private void loadSounds(Context context) {
        // dummy sounds (use system sounds as placeholders)
        try {
            soundMap.put(SOUND_CORRECT, 1);
            soundMap.put(SOUND_WRONG, 1);
            soundMap.put(SOUND_MISS, 1);
            soundMap.put(SOUND_LEVEL_UP, 1);
            soundMap.put(SOUND_GAME_OVER, 1);
            soundMap.put(SOUND_CLICK, 1);
        } catch (Exception e) {
            Log.e(TAG, "Error while loading sounds: " + e.getMessage());
            soundsEnabled = false;
        }
    }

    public void playSound(int soundId) {
        if (!soundsEnabled || soundPool == null) {
            return;
        }

        Integer sound = soundMap.get(soundId);
        if (sound != null) {
            soundPool.play(sound, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    public void setSoundsEnabled(boolean enabled) {
        this.soundsEnabled = enabled;
    }

    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}