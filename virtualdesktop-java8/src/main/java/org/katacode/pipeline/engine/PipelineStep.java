package org.katacode.pipeline.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.katacode.pipeline.engine.meta.PropertyDescriptor;
import org.katacode.pipeline.engine.meta.PropertyDescriptor.PropertyType;

/**
 * A declarative building block wrapping atomic functional logic.
 * Enforces standardized signatures allowing complex pipelines to string together.
 */
public class PipelineStep {

    private final String id;
    private final String componentName;
    private final String inputContract;  // e.g., "ResultSet", "JSON"
    private final String outputContract; // e.g., "JSON", "None"

    private final Map<String, String> propertyValues = new HashMap<>();
    private final List<PropertyDescriptor> propertyDescriptors = new ArrayList<>();

    public List<PropertyDescriptor> getPropertyDescriptors() {
        return propertyDescriptors;
    }

    public Map<String, String> getPropertyValues() {
        return propertyValues;
    }

    public void setPropertyValue(String key, String value) {
        propertyValues.put(key, value);
    }
    
    // Internal generic type erasure allowing uniform composition
    private final Function<Message<Object>, Message<Object>> executionLogic;

    @SuppressWarnings("unchecked")
    public <I, O> PipelineStep(String id, String componentName, String inputContract, String outputContract, 
                               Function<Message<I>, Message<O>> logic) {
        this.id = id;
        this.componentName = componentName;
        this.inputContract = inputContract;
        this.outputContract = outputContract;
        
        // Automatically populate property fields based on component type signatures
        configureDefaultMetadata();

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

    /**
     * High-Craft Metadata Population. Bakes specific fields into the component
     * data dictionary based on its functional role.
     */
    private void configureDefaultMetadata() {
        String name = componentName.toLowerCase();
        System.out.println("name: " + name);

        if (name.contains("reader") || name.contains("source")) {
            // DB Reader Specific Specs
            propertyDescriptors.add(new PropertyDescriptor("jdbc.url", "JDBC Connection URL", PropertyType.TEXT));
            propertyDescriptors.add(new PropertyDescriptor("fetch.size", "Statement Fetch Size", PropertyType.TEXT));
            propertyDescriptors.add(new PropertyDescriptor("sql.query", "Extraction SQL Query", PropertyType.TEXT));
            
            // Sensible Defaults
            propertyValues.put("jdbc.url", "jdbc:postgresql://localhost:5432/analytics_db");
            propertyValues.put("fetch.size", "1000");
            propertyValues.put("sql.query", "SELECT * FROM transactions WHERE status = 'NEW'");

        } else if (name.contains("transformer") || name.contains("converter")) {
            // JSON Transformer Specific Specs
            propertyDescriptors.add(new PropertyDescriptor("json.minify", "Minify Output Payload", PropertyType.BOOLEAN));
            propertyDescriptors.add(new PropertyDescriptor("json.charset", "Target Character Encoding", 
                PropertyType.COMPONENT_CHOICE, new String[]{"UTF-8", "ISO-8859-1", "US-ASCII"}));
            
            // Sensible Defaults
            propertyValues.put("json.minify", "true");
            propertyValues.put("json.charset", "UTF-8");

        } else if (name.contains("filter") || name.contains("router")) {
            // Filter Specific Specs
            propertyDescriptors.add(new PropertyDescriptor("filter.regex", "Exclusion Regular Expression", PropertyType.TEXT));
            propertyDescriptors.add(new PropertyDescriptor("filter.halt", "Halt On Failure Condition", PropertyType.BOOLEAN));
            
            // Sensible Defaults
            propertyValues.put("filter.regex", "^[0-9]+");
            propertyValues.put("filter.halt", "false");

        } else if (name.contains("dispatcher") || name.contains("sink")) {
            // REST Dispatcher / Sink Specific Specs
            propertyDescriptors.add(new PropertyDescriptor("http.endpoint", "Target REST Endpoint URI", PropertyType.TEXT));
            propertyDescriptors.add(new PropertyDescriptor("http.method", "HTTP Method Verb", 
                PropertyType.COMPONENT_CHOICE, new String[]{"POST", "PUT", "GET"}));
            
            // Sensible Defaults
            propertyValues.put("http.endpoint", "https://api.katacode.org/v2/upload");
            propertyValues.put("http.method", "POST");
        }
    }
    
    // --- UI/Metadata Binding Accessors ---
    public String getId() { return id; }
    public String getComponentName() { return componentName; }
    public String getInputContract() { return inputContract; }
    public String getOutputContract() { return outputContract; }

}
