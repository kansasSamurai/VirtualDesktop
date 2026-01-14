package org.jwellman.demo.timer;

import javax.swing.*;
import javax.swing.Timer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CentralTimer {

    private final int intervalMs;
    private final Timer timer;
    private final Map<String, ScheduledTask> tasks;

    // Singleton
    private static CentralTimer instance;

    private CentralTimer(int intervalMs) {
        this.intervalMs = intervalMs;
        this.tasks = new ConcurrentHashMap<>();
        
        // Single timer that fires regularly
        this.timer = new Timer(intervalMs, e -> {
            long currentTime = System.currentTimeMillis();

            // Check each task to see if it should run
            // TODO to truly optimize, create a reusable list object and clear
            List<ScheduledTask> removeList = new ArrayList<>();
            tasks.forEach((key, task)->{
                if (task.shouldRun(currentTime)) {
                    task.run(currentTime);
                    if (task.oneShot) {
                        removeList.add(task);
                    }
                }
            });
            for (ScheduledTask task : removeList) {
                this.cancelTask(task.id);
            }
        });
        
        timer.start();
    }
    
    // Singleton access
    public static synchronized CentralTimer getInstance() {
        if (instance == null) {
            instance = new CentralTimer(50); // Default 50ms tick (20 FPS)
        }
        return instance;
    }
    
    public static synchronized CentralTimer getInstance(int intervalMs) {
        if (instance == null) {
            instance = new CentralTimer(intervalMs);
        }
        return instance;
    }
    
    /**
     * Schedule a task to run at a specific interval
     * @param taskId Unique identifier for this task
     * @param intervalMs How often to run (in milliseconds)
     * @param callback The work to perform
     * @return The task ID for later reference
     */
    public String scheduleTask(String taskId, int intervalMs, Runnable callback) {
        ScheduledTask task = new ScheduledTask(taskId, intervalMs, callback, false);
        tasks.put(taskId, task);
        return taskId;
    }
    
    /**
     * Schedule a one-time delayed task
     */
    public String scheduleOnce(String taskId, int delayMs, Runnable callback) {
        ScheduledTask task = new ScheduledTask(taskId, delayMs, callback, true);
        tasks.put(taskId, task);
        return taskId;
    }
    
    /**
     * Cancel a scheduled task
     */
    public void cancelTask(String taskId) {
        tasks.remove(taskId);
    }
    
    /**
     * Pause a task without removing it
     */
    public void pauseTask(String taskId) {
        ScheduledTask task = tasks.get(taskId);
        if (task != null) {
            task.pause();
        }
    }
    
    /**
     * Resume a paused task
     */
    public void resumeTask(String taskId) {
        ScheduledTask task = tasks.get(taskId);
        if (task != null) {
            task.resume();
        }
    }
    
    /**
     * Stop the central timer (stops all tasks)
     */
    public void shutdown() {
        timer.stop();
        tasks.clear();
    }
    
    /**
     * Get the number of active tasks
     */
    public int getActiveTaskCount() {
        return tasks.size();
    }
    
    // Inner class representing a scheduled task
    private static class ScheduledTask {

        private final int intervalMs;
        private final String id;
        private final boolean oneShot;
        private final Runnable callback;
        private long lastRunTime;
        private boolean paused;
        private boolean oneShotRan;

        public ScheduledTask(String id, int intervalMs, Runnable callback, boolean oneShot) {
            this.id = id;
            this.intervalMs = intervalMs;
            this.callback = callback;
            this.oneShot = oneShot;
            this.oneShotRan = false;
            this.lastRunTime = System.currentTimeMillis();
            this.paused = false;
        }
        
        public boolean shouldRun(long currentTime) {
            if (paused) return false;
            return (currentTime - lastRunTime) >= intervalMs;
        }

        public void run(long currentTime) {
            lastRunTime = currentTime;

            // Execute callback on EDT (we're already on EDT via Swing Timer)
            callback.run();

            // If one-shot, mark for removal (would need reference to parent map)
            if (oneShot) {
                // The parent will handle removal
                oneShotRan = true;
            }
        }

        public void pause() {
            paused = true;
        }

        public void resume() {
            paused = false;
            lastRunTime = System.currentTimeMillis(); // Reset timing
        }

    }

}
