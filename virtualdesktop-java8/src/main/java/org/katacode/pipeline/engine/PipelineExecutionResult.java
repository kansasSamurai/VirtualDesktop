package org.katacode.pipeline.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Capture structural metrics and performance profiling for a single pipeline run.
 */
public final class PipelineExecutionResult {
    private final long totalDurationMillis;
    private final boolean successfullyCompleted;
    private final List<StepMetric> stepMetrics;
    private final Message<Object> finalOutputState;

    public PipelineExecutionResult(long totalDurationMillis, boolean successfullyCompleted, 
                                   List<StepMetric> stepMetrics, Message<Object> finalOutputState) {
        this.totalDurationMillis = totalDurationMillis;
        this.successfullyCompleted = successfullyCompleted;
        // Java 8 equivalent for absolute immutability protection
        this.stepMetrics = Collections.unmodifiableList(new ArrayList<>(stepMetrics));
        // Java 10+
        // this.stepMetrics = List.copyOf(stepMetrics);
        this.finalOutputState = finalOutputState;
    }

    public long getTotalDurationMillis() { return totalDurationMillis; }
    public boolean isSuccessfullyCompleted() { return successfullyCompleted; }
    public List<StepMetric> getStepMetrics() { return stepMetrics; }
    public Message<Object> getFinalOutputState() { return finalOutputState; }

    /**
     * Micro-profile metric record for an individual card block.
     */
    public static final class StepMetric {
        private final String stepId;
        private final String componentName;
        private final long elapsedNanos;
        private final boolean wasFilteredOut;

        public StepMetric(String stepId, String componentName, long elapsedNanos, boolean wasFilteredOut) {
            this.stepId = stepId;
            this.componentName = componentName;
            this.elapsedNanos = elapsedNanos;
            this.wasFilteredOut = wasFilteredOut;
        }

        public String getStepId() { return stepId; }
        public String getComponentName() { return componentName; }
        public long getElapsedMillis() { return elapsedNanos / 1_000_000; }
        public boolean isWasFilteredOut() { return wasFilteredOut; }
    }

}
