package com.example.achievekit.util;

import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SoundForPomodoro
 *
 * Lightweight sound utility for the Pomodoro timer.
 * - Place audio files under: src/main/resources/sounds/
 * - Supported by JavaFX Media; prefer small .wav files.
 *
 * Examples:
 *   SoundForPomodoro.play("focus_start.wav");
 *   SoundForPomodoro.incoming(); // generic "phase finished" sound
 */
public final class SoundForPomodoro {

    /** Relative path under resources. */
    private static final String BASE = "/sounds/";

    /** Clip cache to avoid reloading the same file multiple times. */
    private static final Map<String, AudioClip> CACHE = new ConcurrentHashMap<>();

    /** Global volume (0.0..1.0). */
    private static volatile double volume = 0.8;

    /** Global mute toggle. */
    private static volatile boolean muted = false;

    private SoundForPomodoro() {} // no instances

    /**
     * Play a sound file by name (e.g., "focus_start.wav").
     * Looks for the file under /resources/sounds/.
     */
    public static void play(String fileName) {
        if (muted) return;
        if (fileName == null || fileName.isBlank()) return;

        try {
            AudioClip clip = CACHE.get(fileName);
            if (clip == null) {
                URL url = SoundForPomodoro.class.getResource(BASE + fileName);
                if (url == null) {
                    System.err.println("⚠️ [SoundForPomodoro] File not found: " + BASE + fileName);
                    return;
                }
                System.out.println("Attempting to play: " + fileName);
                clip = new AudioClip(url.toString());
                clip.setVolume(volume);
                CACHE.put(fileName, clip);
            } else {
                // keep cached clip volume in sync with global volume
                if (!Objects.equals(clip.getVolume(), volume)) {
                    clip.setVolume(volume);
                }
            }
            clip.play();
        } catch (Exception ex) {
            System.err.println("⚠️ [SoundForPomodoro] Failed to play \"" + fileName + "\": " + ex.getMessage());
        }
    }

    /**
     * Convenience sound for “phase finished”.
     * Change the file name to whatever you like.
     */
    public static void incoming() {
        play("focus_end.wav");
    }

    /** Preload one or more files into the cache (optional). */
    public static void preload(String... fileNames) {
        if (fileNames == null) return;
        for (String f : fileNames) {
            if (f == null || f.isBlank()) continue;
            try {
                URL url = SoundForPomodoro.class.getResource(BASE + f);
                if (url != null) {
                    AudioClip clip = new AudioClip(url.toString());
                    clip.setVolume(volume);
                    CACHE.putIfAbsent(f, clip);
                }
            } catch (Exception ignored) {}
        }
    }

    /** Mute/unmute all future sounds. */
    public static void setMuted(boolean m) { muted = m; }

    /** Set global volume for future sounds (0.0 .. 1.0). */
    public static void setVolume(double v) { volume = Math.max(0.0, Math.min(1.0, v)); }

    /** Stop all currently playing cached clips. */
    public static void stopAll() {
        for (AudioClip clip : CACHE.values()) {
            try { clip.stop(); } catch (Exception ignored) {}
        }
    }
}
