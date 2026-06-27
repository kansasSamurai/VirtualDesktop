package org.katacode.pipeline.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * The core Orchestration Engine. Holds a structural chain of functional building blocks
 * and drives a Message envelope through them sequentially with precise validation.
 */
public class PipelineContext {

    private static final Logger log = Logger.getLogger(PipelineContext.class.getName());
    
    private final List<PipelineStep> steps = new ArrayList<>();

    /**
     * Appends a new structural card to the current canvas sequence.
     */
    public void addStep(PipelineStep step) {
        this.steps.add(step);
    }

    public void clearPipeline() {
        this.steps.clear();
    }

    public List<PipelineStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    /**
     * Executes the structural pipeline end-to-end.
     * Implements strict "Fail Fast" principles and performance profiling.
     *
     * @param incomingEnvelope The baseline request envelope (e.g., from a Controller or local component)
     * @return An unmodifiable execution profile containing states, payloads, and metrics.
     */
    @SuppressWarnings("unchecked")
    public PipelineExecutionResult execute(Message<?> incomingEnvelope) {
        long pipelineStart = System.currentTimeMillis();
        List<PipelineExecutionResult.StepMetric> metricsCollector = new ArrayList<>();
        
        // Safely upcast the initial typed message envelope into an Object baseline for uniform traversal
        Message<Object> currentExchange = (Message<Object>) incomingEnvelope;
        boolean executionHaltedByFilter = false;

        log.info(() -> String.format("Initiating functional pipeline execution. Registered Steps: %d", steps.size()));

        for (PipelineStep step : steps) {
            if (currentExchange == null) {
                // A prior step (like a Camel Message Filter) returned null, safely cutting execution short
                executionHaltedByFilter = true;
                break;
            }

            long stepStart = System.nanoTime();
            Message<Object> nextExchange;
            
            try {
                // Execute the pure atomic block execution function
                nextExchange = step.execute(currentExchange);
            } catch (Exception fatalError) {
                log.severe(() -> String.format("Fatal processing error inside Step [%s] (%s): %s", 
                        step.getId(), step.getComponentName(), fatalError.getMessage()));
                
                long totalDuration = System.currentTimeMillis() - pipelineStart;
                return new PipelineExecutionResult(totalDuration, false, metricsCollector, currentExchange);
            }
            
            long stepEnd = System.nanoTime();
            boolean filteredThisStep = (nextExchange == null);
            
            metricsCollector.add(new PipelineExecutionResult.StepMetric(
                step.getId(), 
                step.getComponentName(), 
                (stepEnd - stepStart), 
                filteredThisStep
            ));

            currentExchange = nextExchange;
        }

        // Finalize execution profiles
        long totalPipelineDuration = System.currentTimeMillis() - pipelineStart;
        boolean successfullyProcessed = !executionHaltedByFilter;

        log.info(() -> String.format("Pipeline execution finalized. Status: Completed=%b, Total Time: %dms", 
                successfullyProcessed, totalPipelineDuration));

        return new PipelineExecutionResult(
            totalPipelineDuration, 
            successfullyProcessed, 
            metricsCollector, 
            currentExchange
        );
    }

}
