package com.cascade.queue;

import com.cascade.delta.CausalDelta;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * EventQueue — Async event queue with retry, backoff, DLQ, and backpressure.
 */
public class EventQueue implements QueueConsumer, QueueProducer {

    public record QueuedEvent(CausalDelta delta, int attemptCount, Instant enqueuedAt,
                              Instant lastAttemptAt, String lastError) {
        public static QueuedEvent fresh(CausalDelta delta) {
            return new QueuedEvent(delta, 0, Instant.now(), null, null);
        }
        public QueuedEvent retry(String error) {
            return new QueuedEvent(delta, attemptCount + 1, enqueuedAt, Instant.now(), error);
        }
        public Duration queueLatency() { return Duration.between(enqueuedAt, Instant.now()); }
    }

    private final BlockingQueue<QueuedEvent> mainQueue;
    private final DeadLetterQueue dlq;
    private final RetryPolicy retryPolicy;
    private final BackpressureController backpressure;
    private final AtomicLong totalEnqueued = new AtomicLong(0);
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong totalRetries = new AtomicLong(0);
    private final AtomicLong totalDropped = new AtomicLong(0);

    public EventQueue() { this(10_000, 3, Duration.ofMillis(100)); }
    public EventQueue(int capacity, int maxRetries, Duration baseBackoff) {
        this.mainQueue = new LinkedBlockingQueue<>(capacity);
        this.dlq = new DeadLetterQueue();
        this.retryPolicy = new RetryPolicy(maxRetries, baseBackoff);
        this.backpressure = new BackpressureController(capacity);
    }

    @Override
    public boolean enqueue(CausalDelta delta) {
        boolean added = mainQueue.offer(QueuedEvent.fresh(delta));
        if (added) totalEnqueued.incrementAndGet(); else totalDropped.incrementAndGet();
        return added;
    }

    @Override
    public QueuedEvent poll(Duration timeout) throws InterruptedException {
        return mainQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void acknowledge(QueuedEvent event) { totalProcessed.incrementAndGet(); }

    @Override
    public void reportFailure(QueuedEvent event, String error) {
        QueuedEvent retryEvent = event.retry(error);
        if (!retryPolicy.shouldRetry(retryEvent.attemptCount())) {
            dlq.add(retryEvent); return;
        }
        totalRetries.incrementAndGet();
        long backoffMs = retryPolicy.getBackoffForAttempt(retryEvent.attemptCount()).toMillis();
        CompletableFuture.delayedExecutor(backoffMs, TimeUnit.MILLISECONDS)
                .execute(() -> mainQueue.offer(retryEvent));
    }

    @Override public int getQueueSize() { return mainQueue.size(); }
    public List<QueuedEvent> getDLQEvents() { return dlq.getAll(); }
    public int getDLQSize() { return dlq.size(); }
    public boolean isEmpty() { return mainQueue.isEmpty(); }
    public long getTotalEnqueued() { return totalEnqueued.get(); }
    public long getTotalProcessed() { return totalProcessed.get(); }
    public long getTotalRetries() { return totalRetries.get(); }
    public long getTotalDeadLettered() { return dlq.size(); }
    public long getTotalDropped() { return totalDropped.get(); }

    public String getStats() {
        return String.format("Queue Stats:\n  Enqueued: %,d\n  Processed: %,d\n  Retries: %,d\n  Dead Lettered: %,d\n  Dropped: %,d\n  Pending: %,d",
                totalEnqueued.get(), totalProcessed.get(), totalRetries.get(), dlq.size(), totalDropped.get(), mainQueue.size());
    }
}
