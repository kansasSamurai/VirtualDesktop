package org.jwellman.virtualdesktop.config;

import java.util.ArrayList;
import java.util.List;

public class DbConfig {
    private List<DbConnection> connections = new ArrayList<>();

    public DbConfig() {}

    public List<DbConnection> getConnections() { return connections; }
    public void setConnections(List<DbConnection> connections) { this.connections = connections; }
}
