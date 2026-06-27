package org.katacode.pipeline.engine;

import java.util.function.Function;

/**
 * A declarative building block wrapping atomic functional logic.
 * Enforces standardized signatures allowing complex pipelines to string together.
 */
public class PipelineStep {
    private final String id;
    private final String componentName;
    private final String inputContract;  // e.g., "ResultSet", "JSON"
    private final String outputContract; // e.g., "JSON", "None"
    
    // Internal generic type erasure allowing uniform composition
    private final Function<Message<Object>, Message<Object>> executionLogic;

    @SuppressWarnings("unchecked")
    public <I, O> PipelineStep(String id, String componentName, String inputContract, String outputContract, 
                               Function<Message<I>, Message<O>> logic) {
        this.id = id;
        this.componentName = componentName;
        this.inputContract = inputContract;
        this.outputContract = outputContract;
        
        // Erase types safely to Object levels for storage inside our workflow array
        this.executionLogic = (Function<Message<Object>, Message<Object>>) (Function<?, ?>) logic;
    }

    /**
     * Passes the message envelope directly into the wrapped functional block.
     * * @param input The incoming message state.
     * @return The transformed or verified message state, or null if filtered out.
     */
    public Message<Object> execute(Message<Object> input) {
        if (input == null) return null; // Safe fast-fail cascade down the chain
        return executionLogic.apply(input);
    }

    // --- UI/Metadata Binding Accessors ---
    public String getId() { return id; }
    public String getComponentName() { return componentName; }
    public String getInputContract() { return inputContract; }
    public String getOutputContract() { return outputContract; }

}
