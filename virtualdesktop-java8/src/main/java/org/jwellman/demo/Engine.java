package org.jwellman.demo;

import javax.swing.Timer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Engine {

    // The core types of easing curves available to your components
    public enum Easing {
        LINEAR,
        SINE_WAVE
    }

    @FunctionalInterface
    public interface Callback {
        /**
         * @param context Timing and calculated tween values
         * @return true to keep animating, false to immediately stop/unregister
         */
        boolean onTick(TweenContext context);
    }

    public static class TweenContext {
        public final long elapsedTimeMs;
        public final long totalDurationMs;
        
        /** A raw linear value from 0.0 to 1.0 based strictly on elapsed time / duration */
        public final double linear;
        
        /** A computed value from 0.0 to 1.0 mapped along a smooth sine curve */
        public final double sine;
        
        /** A helper field that matches whatever Easing type was explicitly requested */
        public final double value;

        public TweenContext(long elapsedTimeMs, long totalDurationMs, Easing easing) {
            this.elapsedTimeMs = elapsedTimeMs;
            this.totalDurationMs = totalDurationMs;

            // 1. Calculate raw linear progress clamped between 0.0 and 1.0
            if (totalDurationMs <= 0) {
                this.linear = 1.0;
            } else {
                this.linear = Math.min(1.0, Math.max(0.0, (double) elapsedTimeMs / totalDurationMs));
            }

            // 2. Calculate the sine mapping
            // For a single point-to-point transition, we map 0->1 linear progress to 0 -> PI/2 radians
            this.sine = Math.sin(this.linear * (Math.PI / 2.0));

            // 3. Assign the shortcut convenience value
            this.value = (easing == Easing.SINE_WAVE) ? this.sine : this.linear;
        }
    }

    private static class RegisteredAnimation {
        final long startTime;
        final long durationMs;
        final Easing easing;
        final Callback callback;

        RegisteredAnimation(long durationMs, Easing easing, Callback callback) {
            this.startTime = System.currentTimeMillis();
            this.durationMs = durationMs;
            this.easing = easing;
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

    /**
     * Register a timed animation with a specific duration and easing style.
     * The engine automatically unregisters it when duration is reached!
     */
    public void register(long durationMs, Easing easing, Callback callback) {
        activeAnimations.add(new RegisteredAnimation(durationMs, easing, callback));
    }

    private void processFrame() {
        if (activeAnimations.isEmpty()) return;

        long now = System.currentTimeMillis();
        for (RegisteredAnimation anim : activeAnimations) {
            long elapsed = now - anim.startTime;
            
            // Build the rich context object containing both linear and sine variations
            TweenContext context = new TweenContext(elapsed, anim.durationMs, anim.easing);
            
            // Invoke callback logic
            boolean keepAlive = anim.callback.onTick(context);
            
            // Auto-kill conditions: 
            // 1. If the component itself explicitly cancels by returning false
            // 2. If the current elapsed time has surpassed our target duration limit
            if (!keepAlive || elapsed >= anim.durationMs) {
                activeAnimations.remove(anim);
            }
        }
    }

}
