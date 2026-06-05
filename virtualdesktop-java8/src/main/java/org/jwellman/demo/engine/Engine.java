package org.jwellman.demo.engine;

import javax.swing.Timer;

import org.jwellman.demo.engine.Strategy.OnceStrategy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Engine {

    @FunctionalInterface
    public interface Callback {
        boolean onTick(TweenContext context);
    }

    public static class TweenContext {
        private final LoopStrategy loopStrategy;
        private final EasingStrategy easingStrategy;
        private final long totalDurationMs;

        public long elapsedTimeMs;
        public double value;

        public TweenContext(long durationMs, LoopStrategy loop, EasingStrategy ease) {
            this.totalDurationMs = durationMs;
            this.loopStrategy = loop;
            this.easingStrategy = ease;
        }

        public void updateElapsed(long elapsedMs) {
            this.elapsedTimeMs = elapsedMs;
    
            double rawProgress = loopStrategy.calculateProgress(elapsedMs, totalDurationMs);
            double calculated = easingStrategy.ease(rawProgress);

            // Engine Security Guard: If the timeline strategy is completely done,
            // the output value MUST be a clean 1.0!
            if (rawProgress >= 1.0 && loopStrategy instanceof OnceStrategy) {
                this.value = 1.0;
            } else {
                this.value = calculated;
            }
        }

    }

    private static class RegisteredAnimation {
        final long startTime;
        final long durationMs;
        final LoopStrategy loopStrategy;
        final TweenContext context;
        final Callback callback;

        // Modified constructor accepting the explicit anchor time
        RegisteredAnimation(long durationMs, LoopStrategy loop, EasingStrategy ease, long startTime, Callback callback) {
            this.startTime = startTime; // Directly lock to the injected timestamp!
            this.durationMs = durationMs;
            this.loopStrategy = loop;
            this.callback = callback;
            this.context = new TweenContext(durationMs, loop, ease);
        }
    }

    private final Timer masterTimer;
    private static final Engine INSTANCE = new Engine(16); // ~60 FPS
    private final List<RegisteredAnimation> activeAnimations = new CopyOnWriteArrayList<>();

    public static Engine getInstance() {
        return INSTANCE;
    }

    private Engine(int delayMs) {
        this.masterTimer = new Timer(delayMs, e -> processFrame());
        this.masterTimer.start();
    }

    public void register(long durationMs, LoopStrategy loop, EasingStrategy ease, Callback callback) {
        // Simply forwards to the anchor method using "now"
        register(durationMs, loop, ease, System.currentTimeMillis(), callback);
    }

    // --- NEW OVERLOAD: PERMISSIVE TIMESTAMPS FOR LOCK-STEP SYNCHRONIZATION ---
    public void register(long durationMs, LoopStrategy loop, EasingStrategy ease, long anchorStartTime, Callback callback) {
        activeAnimations.add(new RegisteredAnimation(durationMs, loop, ease, anchorStartTime, callback));
    }

    private void processFrame() {
        if (activeAnimations.isEmpty())
            return;

        long now = System.currentTimeMillis();
        for (RegisteredAnimation anim : activeAnimations) {
            long elapsed = now - anim.startTime;

            // --- NEW OPTIMIZATION GUARD ---
            // If the timeline has hit or exceeded the deadline under OnceStrategy,
            // force elapsed to exactly match durationMs to wipe out floating-point noise!
            if (anim.loopStrategy instanceof OnceStrategy && elapsed >= anim.durationMs) {
                elapsed = anim.durationMs;
            }

            anim.context.updateElapsed(elapsed);
            boolean keepAlive = anim.callback.onTick(anim.context);

            if (!keepAlive || (anim.loopStrategy instanceof OnceStrategy && elapsed >= anim.durationMs)) {
                activeAnimations.remove(anim);
            }
        }
    }

}
