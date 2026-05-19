package com.cascade.utils;

import java.time.Duration;
import java.time.Instant;

/**
 * BenchmarkUtils — Timing utilities for performance measurement.
 */
public final class BenchmarkUtils {
    private BenchmarkUtils() {}

    public static TimedResult<Void> time(Runnable task) {
        Instant start = Instant.now();
        task.run();
        return new TimedResult<>(null, Duration.between(start, Instant.now()));
    }

    public static <T> TimedResult<T> time(java.util.function.Supplier<T> task) {
        Instant start = Instant.now();
        T result = task.get();
        return new TimedResult<>(result, Duration.between(start, Instant.now()));
    }

    public record TimedResult<T>(T result, Duration elapsed) {
        public long elapsedMs() { return elapsed.toMillis(); }
        public long throughput(long operationCount) {
            return (operationCount * 1000L) / Math.max(1, elapsed.toMillis());
        }
    }
}
