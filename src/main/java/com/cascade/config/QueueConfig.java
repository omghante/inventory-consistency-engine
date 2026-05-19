package com.cascade.config;

import java.time.Duration;

/**
 * QueueConfig — Configuration for the event queue.
 */
public record QueueConfig(int capacity, int maxRetries, Duration baseBackoff, double warningThreshold) {
    public static QueueConfig defaults() {
        return new QueueConfig(10_000, 3, Duration.ofMillis(100), 0.8);
    }
}
