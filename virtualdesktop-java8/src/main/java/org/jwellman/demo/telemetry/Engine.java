package org.jwellman.demo.telemetry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Timer;

import org.jwellman.demo.gauge.VRadialIndicator;

public class Engine {

    public enum Easing {
        LINEAR,
        SINE_WAVE
    }

    public enum LoopMode {
        ONCE,
        SAWTOOTH,
        TRIANGLE,
        COSINE,
        SINE_LOOP
    }

    @FunctionalInterface
    public interface Callback {
        boolean onTick(TweenContext context);
    }

    // --- NEW: REUSABLE PRODUCTION TELEMETRY TRACKER ---
    public static class TweenState {
        private double startValue = 0.0;
        private double targetValue = 0.0;
        private double currentValue = 0.0;
        
        private long lastUpdateTimeMs = 0;
        private final long debounceWindowMs;
        private final long animationDurationMs;
        private final Easing easing;

        /**
         * @param initialValue Starting value of the stream
         * @param durationMs How long a single visual transition should take
         * @param sampleWindowMs The minimum "breathing room" time before accepting a new value
         */
        public TweenState(double initialValue, long durationMs, long sampleWindowMs, Easing easing) {
            this.currentValue = initialValue;
            this.startValue = initialValue;
            this.targetValue = initialValue;
            this.animationDurationMs = durationMs;
            this.debounceWindowMs = sampleWindowMs;
            this.easing = easing;
        }

        /**
         * Pushes a raw, noisy telemetry value into the state.
         * The state will automatically decide if it should throttle it or smoothly animate to it.
         */
        public void updateTelemetry(double rawValue, VRadialIndicator targetComponent) {
            long now = System.currentTimeMillis();
            
            // If telemetry is spamming faster than our defined sample window, drop/ignore the noise
            if (now - lastUpdateTimeMs < debounceWindowMs) {
                return; 
            }
            
            this.lastUpdateTimeMs = now;
            this.startValue = this.currentValue;
            this.targetValue = rawValue;

            // Automatically register a self-contained, point-to-point flight with the engine
            Engine.getInstance().register(animationDurationMs, this.easing, LoopMode.ONCE, context -> {
                if (!targetComponent.isDisplayable()) return false;

                double delta = this.targetValue - this.startValue;
                this.currentValue = this.startValue + (delta * context.value);
                
                targetComponent.setPercentage(this.currentValue);
                return true;
            });
        }

        public double getCurrentValue() { return this.currentValue; }
        public double getTargetValue() { return this.targetValue; }
    }

    public static class TweenContext {
        public final long elapsedTimeMs;
        public final long totalDurationMs;
        public final double value;

        public TweenContext(long elapsedMs, long durationMs, Easing easing, LoopMode loopMode) {
            this.totalDurationMs = durationMs;
            this.elapsedTimeMs = elapsedMs;

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
                    double radians = (2.0 * Math.PI * elapsedMs) / durationMs;
                    timelineProgress = (1.0 - Math.cos(radians)) / 2.0;
                } else if (loopMode == LoopMode.SINE_LOOP) {
                    double radians = (2.0 * Math.PI * elapsedMs) / durationMs;
                    timelineProgress = (Math.sin(radians - (Math.PI / 2.0)) + 1.0) / 2.0;
                }
            } else {
                timelineProgress = 1.0;
            }

            if (easing == Easing.SINE_WAVE) {
                this.value = Math.sin(timelineProgress * (Math.PI / 2.0));
            } else {
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
    private static final Engine INSTANCE = new Engine(16);

    public static Engine getInstance() { return INSTANCE; }

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
