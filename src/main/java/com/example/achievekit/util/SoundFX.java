package com.example.achievekit.util;

/**
 * Cross-platform notification beeps for Pomodoro phases.
 * Works without javafx.media — uses java.awt.Toolkit beep only.
 */
public final class SoundFX {
    private SoundFX() { }

    /** Simple, safe system beep */
    private static void beep() {
        try {
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Throwable ignored) {
            // ignore if AWT unavailable (e.g., headless system)
        }
    }

    /** 🔊 Called when the timer starts or resumes */
    public static void start() {
        beep();
    }

    /** 🔊 Called when a focus phase finishes successfully */
    public static void incoming() {
        beep();
    }

    /** 🔊 Called when a break finishes */
    public static void breakOver() {
        // double beep for distinction
        beep();
        try { Thread.sleep(150); } catch (InterruptedException ignored) { }
        beep();
    }

    /** 🔊 Called when the user skips a phase */
    public static void skip() {
        // lower-frequency style (three quick beeps)
        beep();
        try { Thread.sleep(80); } catch (InterruptedException ignored) { }
        beep();
        try { Thread.sleep(80); } catch (InterruptedException ignored) { }
        beep();
    }
}
