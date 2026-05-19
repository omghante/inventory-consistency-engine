package com.cascade.queue;

/**
 * BackpressureController — Monitors queue saturation and applies flow control.
 */
public class BackpressureController {
    private final int capacity;
    private final double warningThreshold;
    public BackpressureController(int capacity) { this(capacity, 0.8); }
    public BackpressureController(int capacity, double warningThreshold) {
        this.capacity = capacity; this.warningThreshold = warningThreshold;
    }
    public boolean isUnderPressure(int currentSize) { return (double) currentSize / capacity >= warningThreshold; }
    public boolean isFull(int currentSize) { return currentSize >= capacity; }
    public double getSaturationRatio(int currentSize) { return (double) currentSize / capacity; }
}
