package com.cascade.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.LongAdder;

/**
 * ThroughputMetrics — Sliding-window throughput measurement.
 */
public class ThroughputMetrics {
    private final LongAdder[] windows;
    private final int windowCount;
    private volatile int currentWindow = 0;

    public ThroughputMetrics() { this(10); }
    public ThroughputMetrics(int windowCount) {
        this.windowCount = windowCount;
        this.windows = new LongAdder[windowCount];
        for (int i = 0; i < windowCount; i++) windows[i] = new LongAdder();
    }

    public void record() { windows[currentWindow % windowCount].increment(); }
    public void advanceWindow() { currentWindow++; windows[currentWindow % windowCount].reset(); }
    public long getCurrentWindowCount() { return windows[currentWindow % windowCount].sum(); }
    public long getTotalCount() {
        long total = 0; for (LongAdder w : windows) total += w.sum(); return total;
    }
}
