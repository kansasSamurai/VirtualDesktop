package org.katacode.pipeline.scenario;

import java.util.ArrayList;
import java.util.List;

import org.katacode.pipeline.engine.ComponentFactory;
import org.katacode.pipeline.engine.Message;
import org.katacode.pipeline.engine.PipelineContext;
import org.katacode.pipeline.engine.PipelineExecutionResult;
import org.katacode.pipeline.engine.PipelineStep;

/**
 * Functional verification harness implementing Scenario A (The Analytical Ingestion Pipeline).
 */
public class PipelineScenarioATest {

    public static void main(String[] args) {
        System.out.println("=== INITIALIZING SCENARIO A RUNTIME ===");

        // 1. Instantiate our core observable Orchestration Engine Context
        PipelineContext context = new PipelineContext();

        // 2. Step 01: Simulating an Inbound Source Mapping step (No-op wrapper for raw data entry)
        PipelineStep step1Source = new PipelineStep(
            "step-01", "DB Reader", "None", "ResultSet",
            (Message<TransactionExchange> msg) -> msg // Pass-through ingestion point
        );

        // 3. Step 02: Camel EIP Pattern - Content-Based Message Filter
        // Explicit logic: Drop account codes matching "TEST_ACC"
        PipelineStep step2Filter = new PipelineStep(
            "step-02", "Content-Based Router", "ResultSet", "ResultSet",
            ComponentFactory.createFilter((TransactionExchange tx) -> !tx.getAccountCode().equalsIgnoreCase("TEST_ACC"))
        );

        // 4. Step 03: Camel EIP Pattern - Message Transformer
        // Converts the rich POJO payload directly into a minified JSON record format string
        PipelineStep step3Transform = new PipelineStep(
            "step-03", "JSON Converter", "ResultSet", "JSON",
            ComponentFactory.createTransformer((TransactionExchange tx) -> 
                String.format("{\"txId\":\"%s\", \"acc\":\"%s\", \"amt\":%.2f, \"ts\":%d}", 
                        tx.getTransactionId(), tx.getAccountCode(), tx.getAmount(), tx.getTimestamp())
            )
        );

        // 5. Step 04: Side-Effect Processing Sink (Simulated Local File System Serialization)
        PipelineStep step4Sink = new PipelineStep(
            "step-04", "File Writer", "JSON", "None",
            (Message<String> msg) -> {
                System.out.println("   >>> [DISK I/O SINK] Writing payload safely to katacode local storage vault...");
                System.out.println("   >>> Content: " + msg.getBody());
                return msg; // Terminal sinks return payload to finalize chain metrics
            }
        );

        // Assemble the blocks sequentially onto our Pipeline Context Canvas
        context.addStep(step1Source);
        context.addStep(step2Filter);
        context.addStep(step3Transform);
        context.addStep(step4Sink);

        // 6. Generate Mock Analytical Dataset (One real account, one test exclusion)
        // Java 10+
//        List<TransactionExchange> dataset = List.of(
//            new TransactionExchange("TX-9981", "RETAIL_01", 1250.75, System.currentTimeMillis()),
//            new TransactionExchange("TX-0000", "TEST_ACC", 0.00, System.currentTimeMillis())
//        );
        // Java 8 equivalent for absolute immutability protection
        List<TransactionExchange> dataset = new ArrayList<>();
        dataset.add(new TransactionExchange("TX-9981", "RETAIL_01", 1250.75, System.currentTimeMillis()));
        dataset.add(new TransactionExchange("TX-0000", "TEST_ACC", 0.00, System.currentTimeMillis()));


        // 7. Fire Execution Cycle
        for (TransactionExchange record : dataset) {
            System.out.println("\n------------------------------------------------");
            System.out.println("Processing Inbound Record: " + record);
            
            // Wrap raw stream input inside our HTTP-Request-style message envelope
            // Java 9+ (Old line)
            // Message<TransactionExchange> envelope = new Message<>(record, Map.of("origin", "Analytics_DB"));
            // Java 8 Compliant replacement
            java.util.Map<String, Object> mockHeaders = new java.util.HashMap<>();
            mockHeaders.put("origin", "Analytics_DB");
            Message<TransactionExchange> envelope = new Message<>(record, mockHeaders);
         
            // Trigger deterministic engine execution
            PipelineExecutionResult result = context.execute(envelope);

            // 8. Print Profiling Performance Summaries (The data that fuels our UI Data View)
            System.out.println("Execution Completed: " + result.isSuccessfullyCompleted());
            System.out.println("Total Execution Duration: " + result.getTotalDurationMillis() + "ms");
            
            for (PipelineExecutionResult.StepMetric metric : result.getStepMetrics()) {
                System.out.printf(" -> Step [%s] (%s): %d ms | Filtered Halted: %b\n",
                        metric.getStepId(), metric.getComponentName(), metric.getElapsedMillis(), metric.isWasFilteredOut());
            }
            
            if (result.getFinalOutputState() != null && result.isSuccessfullyCompleted()) {
                System.out.println("Final System Pipeline Message Out: " + result.getFinalOutputState().getBody());
            } else {
                System.out.println("Final System Pipeline Message Out: [NULL] (Pipeline Flow Intentionally Terminated)");
            }
        }
    }

}
