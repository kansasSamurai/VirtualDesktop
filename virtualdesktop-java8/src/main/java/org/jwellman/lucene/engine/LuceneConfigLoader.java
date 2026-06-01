package org.jwellman.lucene.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jwellman.lucene.model.LuceneGlobalConfig;

import java.io.File;
import java.io.IOException;

/**
 * Loads and saves {@link LuceneGlobalConfig} from/to {@code config/lucene-config.json}.
 *
 * <p>Returns a default config (empty sandboxes list) if the file is absent,
 * allowing first-run initialization without user intervention.</p>
 */
public final class LuceneConfigLoader {

    private static final String CONFIG_PATH = "config/lucene-config.json";

    private LuceneConfigLoader() {
    }

    /**
     * Reads the config file. Returns a default {@link LuceneGlobalConfig} if absent.
     */
    public static LuceneGlobalConfig load() {
        File file = new File(CONFIG_PATH);
        if (!file.exists()) {
            return new LuceneGlobalConfig();
        }
        try {
            return new ObjectMapper().readValue(file, LuceneGlobalConfig.class);
        } catch (IOException e) {
            System.err.println("LuceneConfigLoader: failed to read " + CONFIG_PATH + ": " + e.getMessage());
            return new LuceneGlobalConfig();
        }
    }

    /**
     * Writes the config back to {@code config/lucene-config.json}.
     *
     * @param config the config to persist
     */
    public static void save(LuceneGlobalConfig config) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(new File(CONFIG_PATH), config);
        } catch (IOException e) {
            System.err.println("LuceneConfigLoader: failed to write " + CONFIG_PATH + ": " + e.getMessage());
        }
    }
}
