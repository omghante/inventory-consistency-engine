package com.cascade.worker;

import com.cascade.engine.CASCADEEngine;
import com.cascade.engine.MergeResult;
import com.cascade.queue.EventQueue;
import com.cascade.queue.EventQueue.QueuedEvent;
import com.cascade.recovery.EventStore;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Worker — Processes inventory events from the queue through CASCADE.
 */
public class Worker implements Runnable {
    private final String workerId;
    private final EventQueue queue;
    private final CASCADEEngine engine;
    private final EventStore eventStore;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Duration pollTimeout;
    private final AtomicLong eventsProcessed = new AtomicLong(0);
    private final AtomicLong eventsSucceeded = new AtomicLong(0);
    private final AtomicLong eventsFailed = new AtomicLong(0);
    private Consumer<WorkerEvent> eventListener;
    private Thread workerThread;

    public record WorkerEvent(String workerId, String type, String eventId,
                              MergeResult result, String error) {}

    public Worker(String workerId, EventQueue queue, CASCADEEngine engine, EventStore eventStore) {
        this(workerId, queue, engine, eventStore, Duration.ofMillis(100));
    }
    public Worker(String workerId, EventQueue queue, CASCADEEngine engine,
                  EventStore eventStore, Duration pollTimeout) {
        this.workerId = workerId; this.queue = queue; this.engine = engine;
        this.eventStore = eventStore; this.pollTimeout = pollTimeout;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            workerThread = new Thread(this, "cascade-worker-" + workerId);
            workerThread.setDaemon(true); workerThread.start();
            emit("STARTED", null, null, null);
        }
    }
    public void shutdown() {
        running.set(false);
        if (workerThread != null) workerThread.interrupt();
        emit("STOPPED", null, null, null);
    }
    public boolean isRunning() { return running.get(); }

    @Override
    public void run() {
        while (running.get()) {
            try {
                QueuedEvent event = queue.poll(pollTimeout);
                if (event == null) continue;
                processEvent(event);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
              catch (Exception e) { System.err.printf("[%s] Unexpected error: %s%n", workerId, e.getMessage()); }
        }
    }

    private void processEvent(QueuedEvent queuedEvent) {
        eventsProcessed.incrementAndGet();
        try {
            MergeResult result = engine.merge(queuedEvent.delta());
            if (eventStore != null) eventStore.record(queuedEvent.delta(), result, workerId);
            queue.acknowledge(queuedEvent); eventsSucceeded.incrementAndGet();
            emit("PROCESSED", queuedEvent.delta().getEventId(), result, null);
        } catch (Exception e) {
            eventsFailed.incrementAndGet();
            queue.reportFailure(queuedEvent, e.getMessage());
            emit("FAILED", queuedEvent.delta().getEventId(), null, e.getMessage());
        }
    }

    public void setEventListener(Consumer<WorkerEvent> listener) { this.eventListener = listener; }
    private void emit(String type, String eventId, MergeResult result, String error) {
        if (eventListener != null) eventListener.accept(new WorkerEvent(workerId, type, eventId, result, error));
    }

    public String getWorkerId() { return workerId; }
    public long getEventsProcessed() { return eventsProcessed.get(); }
    public long getEventsSucceeded() { return eventsSucceeded.get(); }
    public long getEventsFailed() { return eventsFailed.get(); }
    public String getStats() {
        return String.format("[%s] processed=%d, succeeded=%d, failed=%d, running=%s",
                workerId, eventsProcessed.get(), eventsSucceeded.get(), eventsFailed.get(), running.get());
    }
}
