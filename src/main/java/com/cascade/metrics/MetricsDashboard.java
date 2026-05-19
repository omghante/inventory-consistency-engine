package com.cascade.metrics;

import com.cascade.engine.CASCADEEngine;
import com.cascade.queue.EventQueue;
import com.cascade.worker.WorkerCluster;
import com.cascade.recovery.EventStore;
import java.time.Duration;
import java.time.Instant;

/**
 * MetricsDashboard — Aggregated system observability.
 */
public class MetricsDashboard {
    private final CASCADEEngine engine;
    private final EventQueue queue;
    private final WorkerCluster cluster;
    private final EventStore eventStore;
    private final Instant startTime = Instant.now();

    public MetricsDashboard(CASCADEEngine engine, EventQueue queue, WorkerCluster cluster, EventStore eventStore) {
        this.engine = engine; this.queue = queue; this.cluster = cluster; this.eventStore = eventStore;
    }

    public String render() {
        Duration uptime = Duration.between(startTime, Instant.now());
        long uptimeSec = Math.max(1, uptime.getSeconds());
        long mergesPerSec = engine.getTotalMerges() / uptimeSec;
        return String.format("""
                ╔══════════════════════════════════════════════════╗
                ║          CASCADE System Dashboard                ║
                ╠══════════════════════════════════════════════════╣
                ║  Uptime:              %02d:%02d:%02d                    ║
                ╠═══════════════ Engine ═══════════════════════════╣
                ║  Total merges:        %,10d                    ║
                ║  Applied:             %,10d                    ║
                ║  Rejected:            %,10d                    ║
                ║  Conflicts:           %,10d                    ║
                ║  Duplicates blocked:  %,10d                    ║
                ║  Oversells prevented: %,10d                    ║
                ║  Throughput:          %,10d merges/sec          ║
                ╠═══════════════ Queue ════════════════════════════╣
                ║  Enqueued:            %,10d                    ║
                ║  Processed:           %,10d                    ║
                ║  Queue depth:         %,10d                    ║
                ║  DLQ depth:           %,10d                    ║
                ╠═══════════════ Workers ══════════════════════════╣
                ║  Active:              %d / %d                        ║
                ╠═══════════════ Event Store ══════════════════════╣
                ║  Entries:             %,10d                    ║
                ╚══════════════════════════════════════════════════╝""",
                uptime.toHours(), uptime.toMinutesPart(), uptime.toSecondsPart(),
                engine.getTotalMerges(), engine.getAppliedCount(), engine.getRejectedCount(),
                engine.getConflictsDetected(), engine.getDuplicatesBlocked(), engine.getOversellsPrevented(),
                mergesPerSec, queue.getTotalEnqueued(), queue.getTotalProcessed(),
                queue.getQueueSize(), queue.getDLQSize(),
                cluster.getActiveCount(), cluster.getTotalCount(), eventStore.size());
    }
}
