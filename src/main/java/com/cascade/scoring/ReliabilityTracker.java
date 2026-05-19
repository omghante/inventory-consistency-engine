package com.cascade.scoring;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ReliabilityTracker — Tracks historical accuracy of each source/warehouse.
 *
 * <p>Learns from past merge results to refine trust scoring over time.</p>
 */
public class ReliabilityTracker {

    private final Map<String, SourceStats> sourceStats = new ConcurrentHashMap<>();

    private record SourceStats(AtomicLong total, AtomicLong successful) {
        SourceStats() { this(new AtomicLong(0), new AtomicLong(0)); }
        double reliability() { return total.get() == 0 ? 0.5 : (double) successful.get() / total.get(); }
    }

    public void recordSuccess(String sourceId) {
        sourceStats.computeIfAbsent(sourceId, k -> new SourceStats()).total().incrementAndGet();
        sourceStats.get(sourceId).successful().incrementAndGet();
    }

    public void recordFailure(String sourceId) {
        sourceStats.computeIfAbsent(sourceId, k -> new SourceStats()).total().incrementAndGet();
    }

    public double getReliability(String sourceId) {
        SourceStats stats = sourceStats.get(sourceId);
        return stats != null ? stats.reliability() : 0.5; // neutral default
    }

    public int getTrackedSources() { return sourceStats.size(); }
}
