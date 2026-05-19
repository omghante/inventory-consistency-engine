package com.cascade.queue;

import java.time.Duration;

/**
 * RetryPolicy — Configurable retry behavior with exponential backoff.
 */
public record RetryPolicy(int maxRetries, Duration baseBackoff) {
    public static RetryPolicy defaultPolicy() { return new RetryPolicy(3, Duration.ofMillis(100)); }
    public Duration getBackoffForAttempt(int attempt) {
        long ms = baseBackoff.toMillis() * (long) Math.pow(2, attempt - 1);
        return Duration.ofMillis(ms);
    }
    public boolean shouldRetry(int currentAttempt) { return currentAttempt < maxRetries; }
}
