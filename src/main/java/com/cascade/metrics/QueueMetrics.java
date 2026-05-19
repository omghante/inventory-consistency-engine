package com.cascade.metrics;

import com.cascade.queue.EventQueue;

/**
 * QueueMetrics — Queue-specific observability.
 */
public class QueueMetrics {
    private final EventQueue queue;
    public QueueMetrics(EventQueue queue) { this.queue = queue; }
    public long getEnqueued() { return queue.getTotalEnqueued(); }
    public long getProcessed() { return queue.getTotalProcessed(); }
    public long getRetries() { return queue.getTotalRetries(); }
    public long getDeadLettered() { return queue.getTotalDeadLettered(); }
    public int getDepth() { return queue.getQueueSize(); }
    public double getProcessingRate() {
        long enqueued = queue.getTotalEnqueued();
        long processed = queue.getTotalProcessed();
        return enqueued == 0 ? 1.0 : (double) processed / enqueued;
    }
}
