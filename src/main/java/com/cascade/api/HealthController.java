package com.cascade.api;

import com.cascade.engine.CASCADEEngine;
import com.cascade.queue.EventQueue;
import com.cascade.worker.WorkerCluster;

/**
 * HealthController — Health check endpoint for load balancers.
 */
public class HealthController {
    private final CASCADEEngine engine;
    private final EventQueue queue;
    private final WorkerCluster cluster;

    public HealthController(CASCADEEngine engine, EventQueue queue, WorkerCluster cluster) {
        this.engine = engine; this.queue = queue; this.cluster = cluster;
    }
    public boolean isHealthy() { return cluster.getActiveCount() > 0 && !queue.isEmpty() || queue.isEmpty(); }
    public String getStatus() {
        return String.format("{\"status\":\"%s\",\"workers\":%d,\"queueDepth\":%d}",
                isHealthy() ? "UP" : "DEGRADED", cluster.getActiveCount(), queue.getQueueSize());
    }
}
