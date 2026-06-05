package org.jwellman.demo.engine;

public interface LoopStrategy {
    double calculateProgress(long elapsedMs, long durationMs);
}