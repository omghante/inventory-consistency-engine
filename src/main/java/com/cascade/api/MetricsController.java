package com.cascade.api;

import com.cascade.engine.CASCADEEngine;

/**
 * MetricsController — REST API facade for engine metrics.
 */
public class MetricsController {
    private final CASCADEEngine engine;
    public MetricsController(CASCADEEngine engine) { this.engine = engine; }
    public String getStats() { return engine.getStats(); }
    public long getTotalMerges() { return engine.getTotalMerges(); }
    public long getConflicts() { return engine.getConflictsDetected(); }
}
