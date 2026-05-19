package com.cascade.exceptions;

/**
 * QueueOverflowException — Thrown when the event queue rejects due to backpressure.
 */
public class QueueOverflowException extends CascadeException {
    private final int queueSize;
    private final int capacity;
    public QueueOverflowException(int queueSize, int capacity) {
        super(String.format("Queue full: %d/%d", queueSize, capacity));
        this.queueSize = queueSize; this.capacity = capacity;
    }
    public int getQueueSize() { return queueSize; }
    public int getCapacity() { return capacity; }
}
