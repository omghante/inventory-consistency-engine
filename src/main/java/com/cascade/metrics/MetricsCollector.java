package com.cascade.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * MetricsCollector — Low-overhead metric collection using LongAdder for hot paths.
 */
public class MetricsCollector {
    private final LongAdder mergeCount = new LongAdder();
    private final LongAdder totalLatencyNanos = new LongAdder();
    private final AtomicLong maxLatencyNanos = new AtomicLong(0);
    private final Instant startTime = Instant.now();

    public void recordMerge(Duration latency) {
        mergeCount.increment();
        long nanos = latency.toNanos();
        totalLatencyNanos.add(nanos);
        maxLatencyNanos.accumulateAndGet(nanos, Math::max);
    }

    public long getMergeCount() { return mergeCount.sum(); }
    public double getAvgLatencyMs() {
        long count = mergeCount.sum();
        return count == 0 ? 0 : (totalLatencyNanos.sum() / (double) count) / 1_000_000.0;
    }
    public double getMaxLatencyMs() { return maxLatencyNanos.get() / 1_000_000.0; }
    public Duration getUptime() { return Duration.between(startTime, Instant.now()); }
    public long getThroughput() {
        long secs = Math.max(1, getUptime().getSeconds());
        return mergeCount.sum() / secs;
    }
}
