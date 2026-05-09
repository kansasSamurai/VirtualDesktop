package org.jwellman.demo.d3ish;

public class Transition {
    private double start, target;
    private long startTime;
    private int duration;
    private boolean running = false;

    public void start(double from, double to, int ms) {
        this.start = from;
        this.target = to;
        this.duration = ms;
        this.startTime = System.currentTimeMillis();
        this.running = true;
    }

    public double getValue() {
        if (!running) return target;
        long elapsed = System.currentTimeMillis() - startTime;
        double progress = Math.min(1.0, (double) elapsed / duration);
        
        if (progress >= 1.0) running = false;
        
        // Simple Ease-In-Out
        double eased = progress < 0.5 ? 2 * progress * progress : -1 + (4 - 2 * progress) * progress;
        return start + (target - start) * eased;
    }

    public boolean isRunning() {
        return running;
    }

}