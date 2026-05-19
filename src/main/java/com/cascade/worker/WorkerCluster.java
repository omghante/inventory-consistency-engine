package com.cascade.worker;

import com.cascade.engine.CASCADEEngine;
import com.cascade.queue.EventQueue;
import com.cascade.recovery.EventStore;
import java.util.*;

/**
 * WorkerCluster — Manages a pool of CASCADE workers for horizontal scaling.
 */
public class WorkerCluster {
    private final EventQueue queue;
    private final CASCADEEngine engine;
    private final EventStore eventStore;
    private final List<Worker> workers;

    public WorkerCluster(EventQueue queue, CASCADEEngine engine, EventStore eventStore, int count) {
        this.queue = queue; this.engine = engine; this.eventStore = eventStore;
        this.workers = new ArrayList<>();
        for (int i = 1; i <= count; i++) workers.add(new Worker("worker-" + i, queue, engine, eventStore));
    }

    public void startAll() { workers.forEach(Worker::start); }
    public void shutdownAll() { workers.forEach(Worker::shutdown); }
    public String scaleUp() {
        int id = workers.size() + 1;
        Worker w = new Worker("worker-" + id, queue, engine, eventStore);
        workers.add(w); w.start(); return w.getWorkerId();
    }
    public String scaleDown() {
        if (workers.isEmpty()) return null;
        Worker w = workers.remove(workers.size() - 1); w.shutdown(); return w.getWorkerId();
    }
    public int getActiveCount() { return (int) workers.stream().filter(Worker::isRunning).count(); }
    public int getTotalCount() { return workers.size(); }
    public List<Worker> getWorkers() { return Collections.unmodifiableList(workers); }

    public String getStats() {
        long tp = workers.stream().mapToLong(Worker::getEventsProcessed).sum();
        long ts = workers.stream().mapToLong(Worker::getEventsSucceeded).sum();
        long tf = workers.stream().mapToLong(Worker::getEventsFailed).sum();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Worker Cluster Stats:\n  Active: %d/%d\n  Processed: %,d\n  Succeeded: %,d\n  Failed: %,d\n",
                getActiveCount(), getTotalCount(), tp, ts, tf));
        sb.append("  Per-worker:\n");
        for (Worker w : workers) sb.append("    ").append(w.getStats()).append("\n");
        return sb.toString();
    }
}
