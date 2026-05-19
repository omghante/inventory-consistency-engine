package com.cascade.queue;

import com.cascade.delta.CausalDelta;

/**
 * QueueProducer — Interface for publishing events to the queue.
 */
public interface QueueProducer {
    boolean enqueue(CausalDelta delta);
    int getQueueSize();
}
