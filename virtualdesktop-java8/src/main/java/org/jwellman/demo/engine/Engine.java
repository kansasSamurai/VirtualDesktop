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
            this.value = easingStrategy.ease(rawProgress);
        }
    }

    private static class RegisteredAnimation {
        final long startTime;
        final long durationMs;
        final LoopStrategy loopStrategy;
        final TweenContext context;
        final Callback callback;

        RegisteredAnimation(long durationMs, LoopStrategy loop, EasingStrategy ease, Callback callback) {
            this.startTime = System.currentTimeMillis();
            this.durationMs = durationMs;
            this.loopStrategy = loop;
            this.callback = callback;
            this.context = new TweenContext(durationMs, loop, ease);
        }
    }

    private final List<RegisteredAnimation> activeAnimations = new CopyOnWriteArrayList<>();
    private final Timer masterTimer;
    private static final Engine INSTANCE = new Engine(16); // ~60 FPS

    public static Engine getInstance() { return INSTANCE; }

    private Engine(int delayMs) {
        this.masterTimer = new Timer(delayMs, e -> processFrame());
        this.masterTimer.start();
    }

    public void register(long durationMs, LoopStrategy loop, EasingStrategy ease, Callback callback) {
        activeAnimations.add(new RegisteredAnimation(durationMs, loop, ease, callback));
    }

    private void processFrame() {
        if (activeAnimations.isEmpty()) return;

        long now = System.currentTimeMillis();
        for (RegisteredAnimation anim : activeAnimations) {
            long elapsed = now - anim.startTime;
            
            anim.context.updateElapsed(elapsed);
            boolean keepAlive = anim.callback.onTick(anim.context);
            
            if (!keepAlive || (anim.loopStrategy instanceof OnceStrategy && elapsed >= anim.durationMs)) {
                activeAnimations.remove(anim);
            }
        }
    }

}
