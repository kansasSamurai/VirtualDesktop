package org.jwellman.demo;

import javax.swing.Timer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Engine {

    public enum Easing {
        LINEAR,
        SINE_WAVE
    }

    public enum LoopMode {
        ONCE,
        SAWTOOTH,
        TRIANGLE,
        COSINE, // Pure, continuous oscillation with zero turnaround velocity
        SINE // Pure, continuous oscillation with ...
    }

    @FunctionalInterface
    public interface Callback {
        boolean onTick(TweenContext context);
    }

    public static class TweenContext {
        public final long elapsedTimeMs;
        public final long totalDurationMs;
        public final double value; // The final calculated progress (0.0 to 1.0)

        public TweenContext(long elapsedMs, long durationMs, Easing easing, LoopMode loopMode) {
            this.totalDurationMs = durationMs;
            this.elapsedTimeMs = elapsedMs;

            // 1. PHASE ONE: Determine macro timeline progress (0.0 to 1.0) based on LoopMode
            double timelineProgress = 0.0;
            
            if (durationMs > 0) {
                if (loopMode == LoopMode.ONCE) {
                    timelineProgress = Math.min(1.0, Math.max(0.0, (double) elapsedMs / durationMs));
                } else if (loopMode == LoopMode.SAWTOOTH) {
                    timelineProgress = (double) (elapsedMs % durationMs) / durationMs;
                } else if (loopMode == LoopMode.TRIANGLE) {
                    long cycle = elapsedMs / durationMs;
                    double remainder = (double) (elapsedMs % durationMs) / durationMs;
                    timelineProgress = (cycle % 2 == 0) ? remainder : (1.0 - remainder);
                } else if (loopMode == LoopMode.COSINE) {
                    // TRUE SEPARATION: The infinite, smooth harmonic wave lives here now!
                    // A full 360-degree round trip period completes every durationMs
                    double radians = (2.0 * Math.PI * elapsedMs) / durationMs;
                    timelineProgress = (1.0 - Math.cos(radians)) / 2.0;
                }
//                else if (loopMode == LoopMode.SINE) {
//                    // --- EXACT ORIGINAL PROTOTYPE RHYTHM ---
//                    // A pure, traditional harmonic loop period mapping 0 -> 2*PI radians
//                    double radians = (2.0 * Math.PI * elapsedMs) / durationMs;
//                    timelineProgress = (Math.sin(radians) + 1.0) / 2.0;
//                }
                else if (loopMode == LoopMode.SINE) {
                    // 1. Calculate the base radians exactly like before
                    double radians = (2.0 * Math.PI * elapsedMs) / durationMs;
                    
                    // 2. NEW: Subtract PI/2 to phase-shift the wave backward by 90 degrees.
                    // This forces sin(0 - PI/2) to equal -1.0, making our starting value exactly 0.0!
                    timelineProgress = (Math.sin(radians - (Math.PI / 2.0)) + 1.0) / 2.0;
                }
            } else {
                timelineProgress = 1.0;
            }

            // 2. PHASE TWO: Apply acceleration adjustment based on Easing
            if (easing == Easing.SINE_WAVE) {
                // Apply a standard trigonometric curve adjustment to the timeline progress
                this.value = Math.sin(timelineProgress * (Math.PI / 2.0));
            } else {
                // Easing.LINEAR: Keep the timeline progress exactly as it was generated
                this.value = timelineProgress;
            }
        }
    }

    private static class RegisteredAnimation {
        final long startTime;
        final long durationMs;
        final Easing easing;
        final LoopMode loopMode;
        final Callback callback;

        RegisteredAnimation(long durationMs, Easing easing, LoopMode loopMode, Callback callback) {
            this.startTime = System.currentTimeMillis();
            this.durationMs = durationMs;
            this.easing = easing;
            this.loopMode = loopMode;
            this.callback = callback;
        }
    }

    private final List<RegisteredAnimation> activeAnimations = new CopyOnWriteArrayList<>();
    private final Timer masterTimer;
    private static final Engine INSTANCE = new Engine(16); // ~60 FPS

    public static Engine getInstance() {
        return INSTANCE;
    }

    private Engine(int delayMs) {
        this.masterTimer = new Timer(delayMs, e -> processFrame());
        this.masterTimer.start();
    }

    public void register(long durationMs, Easing easing, LoopMode loopMode, Callback callback) {
        activeAnimations.add(new RegisteredAnimation(durationMs, easing, loopMode, callback));
    }

    private void processFrame() {
        if (activeAnimations.isEmpty()) return;

        long now = System.currentTimeMillis();
        for (RegisteredAnimation anim : activeAnimations) {
            long elapsed = now - anim.startTime;
            TweenContext context = new TweenContext(elapsed, anim.durationMs, anim.easing, anim.loopMode);
            
            boolean keepAlive = anim.callback.onTick(context);
            if (!keepAlive || (anim.loopMode == LoopMode.ONCE && elapsed >= anim.durationMs)) {
                activeAnimations.remove(anim);
            }
        }
    }

}
