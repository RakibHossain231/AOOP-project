package com.example.achievekit;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

import com.example.achievekit.util.SoundFX;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Keeps timer state global so it does NOT reset when you change pages. */
public class PomodoroState {


    // For group task mode
    public List<Integer> groupTaskQueue = new ArrayList<>();
    public int currentGroupIndex = 0;

    // PomodoroState.java  (fields)
    public boolean sessionFinished = false;
    private boolean isRunning = false;
    private volatile boolean running = false;
    // In PomodoroState.java (public fields used elsewhere in your project)
    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
    }


    // Call this ONLY when finishing the 4th phase (long break completed)
    public void finishSessionBlock() {
        sessionFinished = true;
        running = false;
    }

    // Call this to fully reset and start a fresh 4-session block.
    public void resetAllForNewSession() {
        pause();  // stop any running timer thread safely
        sessionFinished = false;  //  unlock after full session
        running = false;          //  stop ticking thread
        currentTaskId = null;

        // Reset all phase data
        phase = Phase.FOCUS;
        focusIndex = 1;
        cycleIndex = 1;
        remainingSeconds = getFocusMinutes() * 60;
        phaseStartAt = null;

        // Reset DB/session tracking
        sessionStartAt = null;
        currentSessionId = null;
        currentCycleId = null;
        completedCycles = 0;
    }



    public enum Phase { FOCUS, SHORT_BREAK, LONG_BREAK }
    // Track the current database session and cycle IDs
    public Integer currentSessionId = null;   // existing session in PomodoroSessions
    public Integer currentCycleId = null;     // active cycle in PomodoroCycles


    private static final PomodoroState INSTANCE = new PomodoroState();
    public static PomodoroState get() { return INSTANCE; }

    // live state
    Phase phase = Phase.FOCUS;
    private int focusMinutes = 25, shortBreakMinutes = 5, longBreakMinutes = 15;
    private boolean autoStart = false;

    private int focusIndex = 1;     // 1..4
    private int cycleIndex = 1;     // 1..8
    private int totalSeconds = 25 * 60;
    private int remainingSeconds = totalSeconds;

    private Timeline timeline;      // lives across controllers


    // DB/session bookkeeping
    LocalDateTime phaseStartAt = null;
    LocalDateTime sessionStartAt = null;
   // Integer currentSessionId = null;
    Integer currentTaskId = null; // may be null (dummy-free run)
    int completedCycles = 0;       // for PomodoroSessions.CompletedCycles

    // UI callback (controller registers here)
    public interface UiListener { void onTick(); }
    private UiListener ui;

    private PomodoroState() { }

    public void attach(UiListener ui) { this.ui = ui; tickUI(); }
    public void detach() { this.ui = null; }

    /* ========== getters ========== */
    public Phase getPhase() { return phase; }
    public int getFocusIndex() { return focusIndex; }
    public int getCycleIndex() { return cycleIndex; }
    public int getTotalSeconds() { return totalSeconds; }
    public int getRemainingSeconds() { return remainingSeconds; }
    public long getElapsedSeconds() {
        if (phaseStartAt == null) return 0;
        java.time.Duration d = java.time.Duration.between(phaseStartAt, java.time.LocalDateTime.now());
        return d.getSeconds();
    }



    public int getFocusMinutes() { return focusMinutes; }
    public int getShortBreakMinutes() { return shortBreakMinutes; }
    public int getLongBreakMinutes() { return longBreakMinutes; }
    public boolean isAutoStart() { return autoStart; }

    /* ========== setters for preferences ========== */
    public void setFocusMinutes(int m) { focusMinutes = clampMinutes(m); if (phase==Phase.FOCUS) recalcForPhase(); }
    public void setShortBreakMinutes(int m) { shortBreakMinutes = clampMinutes(m); if (phase==Phase.SHORT_BREAK) recalcForPhase(); }
    public void setLongBreakMinutes(int m) { longBreakMinutes = clampMinutes(m); if (phase==Phase.LONG_BREAK) recalcForPhase(); }
    public void setAutoStart(boolean v) { autoStart = v; }

    /* ========== control ========== */
    private Thread timerThread;

    /** Start the timer on a background thread (safe across scenes). */
    public synchronized void start() {
        if (running) return; // already running

        running = true;
        if (timerThread == null || !timerThread.isAlive()) {
            timerThread = new Thread(() -> {
                try {
                    while (running && remainingSeconds > 0) {
                        Thread.sleep(1000);
                        remainingSeconds--;
                        tickUI();

                        if (remainingSeconds <= 0) {
                            running = false;
                            Platform.runLater(() -> {
                                // phase complete
                                SoundFX.incoming();
                                if (ui != null) ui.onTick();
                            });
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            timerThread.setDaemon(true);
            timerThread.start();
        }
    }

    /** Pause timer but do not destroy thread state. */
    public synchronized void pause() {
        running = false;
    }

    /** Stop timer and reset to start of phase. */
//    public synchronized void resetPhase() {
//        running = false;
//        recalcForPhase();
//        tickUI();
//    }


    /** Resets remaining time to the start of current phase. */
    public void resetPhase() {
        recalcForPhase();
        tickUI();
    }

    public void gotoNextPhase() {
        // cycle math
        cycleIndex++;
        switch (phase) {
            case FOCUS -> {
                completedCycles++;
                if (focusIndex == 4) {
                    phase = Phase.LONG_BREAK;
                } else {
                    phase = Phase.SHORT_BREAK;
                }
            }
            case SHORT_BREAK -> {
                focusIndex++;
                phase = Phase.FOCUS;
            }
            case LONG_BREAK -> {
                // long break done – next full session begins
                phase = Phase.FOCUS;
                focusIndex = 1;
                cycleIndex = 1;
                completedCycles = 0;
            }
        }
        recalcForPhase();
        tickUI();
    }

    public void applyPhase(Phase p) {
        phase = p;
        recalcForPhase();
        tickUI();
    }

    private void stopInternal() {
        if (timeline != null) timeline.stop();
        running = false;
        tickUI();
    }


    private int clampMinutes(int m) { return Math.max(1, Math.min(600, m)); }

    private void recalcForPhase() {
        totalSeconds = switch (phase) {
            case FOCUS -> focusMinutes * 60;
            case SHORT_BREAK -> shortBreakMinutes * 60;
            case LONG_BREAK  -> longBreakMinutes * 60;
        };
        if (!running) remainingSeconds = totalSeconds;
    }

    private void tickUI() { if (ui != null) Platform.runLater(ui::onTick); }
}
