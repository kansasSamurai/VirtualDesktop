package org.jwellman.virtualdesktop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class ConfigStore<T> {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String CONFIG_DIR = "config";

    private final String filename;
    private final Class<T> type;
    private T cached;

    public ConfigStore(String filename, Class<T> type) {
        this.filename = filename;
        this.type = type;
    }

    /** Load config from disk (ignores cache) */
    public T load() throws IOException {
        File configFile = new File(CONFIG_DIR, filename);
        cached = mapper.readValue(configFile, type);
        return cached;
    }

    /** Save config to disk and update cache */
    public void save(T config) throws IOException {
        File configFile = new File(CONFIG_DIR, filename);
        mapper.writerWithDefaultPrettyPrinter().writeValue(configFile, config);
        cached = config;
    }

    /** Get cached config, loading if necessary */
    public T get() {
        if (cached == null) {
            try {
                load();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return cached;
    }

    /** Force reload from disk */
    public void reload() {
        try {
            load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Check if config file exists */
    public boolean exists() {
        return new File(CONFIG_DIR, filename).exists();
    }
}
