package org.jwellman.demo;

import javax.swing.Timer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Engine {

    public enum Easing {
        LINEAR,
        SINE_WAVE
    }

    // NEW: Define how the time boundaries behave when reaching the duration limit
    public enum LoopMode {
        ONCE,        // Run from 0 to 1 and terminate
        SAWTOOTH,    // 0 -> 1, snap back to 0, repeat indefinitely
        TRIANGLE     // 0 -> 1 -> 0 -> 1 (ping-pong) repeat indefinitely
    }

    @FunctionalInterface
    public interface Callback {
        boolean onTick(TweenContext context);
    }

    public static class TweenContext {
        public final long elapsedTimeMs;
        public final long totalDurationMs;
        
        public final double linear;
        public final double sine;
        public final double value;

        public TweenContext(long elapsedMs, long durationMs, Easing easing, LoopMode loopMode) {
            this.totalDurationMs = durationMs;
            long calculatedElapsed = elapsedMs;

            // --- NEW: Loop Modifier Math ---
            if (loopMode == LoopMode.SAWTOOTH && durationMs > 0) {
                // Modulo resets our position back to 0 the millisecond we hit durationMs
                calculatedElapsed = elapsedMs % durationMs;
            } else if (loopMode == LoopMode.TRIANGLE && durationMs > 0) {
                long cycle = elapsedMs / durationMs;
                long remainder = elapsedMs % durationMs;
                if (cycle % 2 == 0) {
                    // Even cycle: traveling forward from 0 to durationMs
                    calculatedElapsed = remainder;
                } else {
                    // Odd cycle: traveling backward from durationMs down to 0
                    calculatedElapsed = durationMs - remainder;
                }
            }
            this.elapsedTimeMs = calculatedElapsed;

            // --- Standard Easing Assignments ---
            if (durationMs <= 0) {
                this.linear = 1.0;
            } else {
                // If running ONCE, clamp it at 1.0. Otherwise, loop math handled boundaries.
                double rawProg = (double) calculatedElapsed / durationMs;
                this.linear = (loopMode == LoopMode.ONCE) ? Math.min(1.0, Math.max(0.0, rawProg)) : rawProg;
            }

            // Map 0->1 progress cleanly to 0 -> PI/2 radians
            this.sine = Math.sin(this.linear * (Math.PI / 2.0));
            this.value = (easing == Easing.SINE_WAVE) ? this.sine : this.linear;
        }
    }

    private static class RegisteredAnimation {
        final long startTime;
        final long durationMs;
        final Easing easing;
        final LoopMode loopMode; // Track loop status
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
    private static final Engine INSTANCE = new Engine(16);

    public static Engine getInstance() {
        return INSTANCE;
    }

    private Engine(int delayMs) {
        this.masterTimer = new Timer(delayMs, e -> processFrame());
        this.masterTimer.start();
    }

    /**
     * Upgraded Register call accepting LoopMode configurations
     */
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
            
            // Auto-kill ONLY if explicitly requested by component or if ONCE has finished
            if (!keepAlive || (anim.loopMode == LoopMode.ONCE && elapsed >= anim.durationMs)) {
                activeAnimations.remove(anim);
            }
        }
    }

}
