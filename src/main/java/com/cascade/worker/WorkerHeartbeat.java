package com.cascade.worker;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;

/**
 * WorkerHeartbeat — Monitors worker health via periodic heartbeats.
 */
public class WorkerHeartbeat {
    private final ConcurrentHashMap<String, Instant> lastHeartbeat = new ConcurrentHashMap<>();
    private final Duration timeout;

    public WorkerHeartbeat(Duration timeout) { this.timeout = timeout; }
    public void recordHeartbeat(String workerId) { lastHeartbeat.put(workerId, Instant.now()); }
    public boolean isHealthy(String workerId) {
        Instant last = lastHeartbeat.get(workerId);
        return last != null && Duration.between(last, Instant.now()).compareTo(timeout) < 0;
    }
    public int getHealthyCount() { return (int) lastHeartbeat.entrySet().stream()
            .filter(e -> Duration.between(e.getValue(), Instant.now()).compareTo(timeout) < 0).count(); }
}
