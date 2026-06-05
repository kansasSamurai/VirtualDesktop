package org.jwellman.demo;

import javax.swing.Timer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Engine {

    // Functional interface for our lightweight animation callbacks
    @FunctionalInterface
    public interface Callback {
        /**
         * @param context Contains timing data and pre-computed wave structures
         * @return true if the animation should keep running, false to unregister/destroy it
         */
        boolean onTick(TweenContext context);
    }

    // Context record passed to callbacks on every frame
    public static class TweenContext {
        public final long elapsedTimeMs;
        public final double sineWave; // Pre-computed smooth 0.0 to 1.0 wave over time

        public TweenContext(long elapsedTimeMs) {
            this.elapsedTimeMs = elapsedTimeMs;
            // Your exact math: cycle cleanly every 2*PI seconds (approx 6.28 seconds per full loop)
            // You can divide elapsedTimeMs differently to speed up or slow down the default cycle
            this.sineWave = (Math.sin(elapsedTimeMs / 1000.0) + 1.0) / 2.0;
        }
    }

    // Internal tracker to wrap registered animations
    private static class RegisteredAnimation {
        final long startTime;
        final Callback callback;

        RegisteredAnimation(Callback callback) {
            this.startTime = System.currentTimeMillis();
            this.callback = callback;
        }
    }

    // Thread-safe list to prevent ConcurrentModificationExceptions if an animation is added/removed mid-tick
    private final List<RegisteredAnimation> activeAnimations = new CopyOnWriteArrayList<>();
    private final Timer masterTimer;

    // Singleton instance for global app access
    private static final Engine INSTANCE = new Engine(16); // ~60 FPS Target

    public static Engine getInstance() {
        return INSTANCE;
    }

    private Engine(int delayMs) {
        // The SINGLE master Swing timer driving the entire desktop interface
        this.masterTimer = new Timer(delayMs, e -> processFrame());
        this.masterTimer.start();
    }

    /**
     * Register a new lightweight animation callback with the master loop
     */
    public void register(Callback callback) {
        activeAnimations.add(new RegisteredAnimation(callback));
    }

    private void processFrame() {
        if (activeAnimations.isEmpty()) return;

        long now = System.currentTimeMillis();
        for (RegisteredAnimation anim : activeAnimations) {
            long elapsed = now - anim.startTime;
            TweenContext context = new TweenContext(elapsed);
            
            // Invoke the callback. If it returns false, cleanly purge it from the engine
            boolean keepAlive = anim.callback.onTick(context);
            if (!keepAlive) {
                activeAnimations.remove(anim);
            }
        }
    }

}
