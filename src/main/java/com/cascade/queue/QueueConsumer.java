package com.cascade.queue;

import java.time.Duration;

/**
 * QueueConsumer — Interface for consuming events from the queue.
 */
public interface QueueConsumer {
    EventQueue.QueuedEvent poll(Duration timeout) throws InterruptedException;
    void acknowledge(EventQueue.QueuedEvent event);
    void reportFailure(EventQueue.QueuedEvent event, String error);
}
