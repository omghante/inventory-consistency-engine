package com.cascade.causality;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * VectorClock — Tracks causal ordering across distributed nodes.
 *
 * <p>Each node (warehouse) maintains a counter. The vector clock is the
 * collection of all counters. By comparing two vector clocks, we can
 * determine if one event "happened before" another, or if they are
 * truly concurrent (independent) updates.</p>
 *
 * <p>This is the same technique used in Amazon's Dynamo paper (2007).</p>
 *
 * <p>Supports bounded mode for production deployments with many warehouse
 * nodes. When maxEntries is set, the clock automatically prunes the
 * least-significant entries (lowest counters) after merge operations
 * to prevent unbounded memory growth. Amazon Dynamo uses a similar
 * pruning strategy based on timestamp age.</p>
 */
public class VectorClock {

    /** Unbounded — clock grows without limit. */
    public static final int UNBOUNDED = -1;

    private final Map<String, Long> clock;
    private final int maxEntries;

    public VectorClock() {
        this.clock = new HashMap<>();
        this.maxEntries = UNBOUNDED;
    }

    public VectorClock(Map<String, Long> initial) {
        this.clock = new HashMap<>(initial);
        this.maxEntries = UNBOUNDED;
    }

    /**
     * Creates a bounded vector clock with a maximum number of entries.
     *
     * <p>When the clock exceeds maxEntries after a merge, the entries
     * with the lowest counters are pruned. This prevents unbounded growth
     * in systems with thousands of warehouse nodes while preserving causal
     * accuracy for the most active nodes.</p>
     *
     * @param initial    initial clock state
     * @param maxEntries maximum number of node entries to retain
     * @return a bounded vector clock
     */
    public static VectorClock bounded(Map<String, Long> initial, int maxEntries) {
        if (maxEntries < 1) throw new IllegalArgumentException("maxEntries must be >= 1");
        return new VectorClock(initial, maxEntries);
    }

    /**
     * Creates a bounded empty vector clock.
     *
     * @param maxEntries maximum number of node entries to retain
     * @return a bounded vector clock
     */
    public static VectorClock bounded(int maxEntries) {
        if (maxEntries < 1) throw new IllegalArgumentException("maxEntries must be >= 1");
        return new VectorClock(new HashMap<>(), maxEntries);
    }

    private VectorClock(Map<String, Long> initial, int maxEntries) {
        this.clock = new HashMap<>(initial);
        this.maxEntries = maxEntries;
        prune();
    }

    /**
     * Increments the counter for the given node.
     *
     * @param nodeId the warehouse/node identifier
     * @return this clock (for chaining)
     */
    public VectorClock increment(String nodeId) {
        this.clock.merge(nodeId, 1L, Long::sum);
        return this;
    }

    /**
     * Compares this clock with another to determine causal relationship.
     *
     * @param other the clock to compare against
     * @return the causal relationship
     */
    public CausalRelation compare(VectorClock other) {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(this.clock.keySet());
        allKeys.addAll(other.clock.keySet());

        boolean thisGreater = false;
        boolean otherGreater = false;

        for (String key : allKeys) {
            long thisVal = this.clock.getOrDefault(key, 0L);
            long otherVal = other.clock.getOrDefault(key, 0L);

            if (thisVal > otherVal) thisGreater = true;
            if (otherVal > thisVal) otherGreater = true;
        }

        if (!thisGreater && !otherGreater) return CausalRelation.EQUAL;
        if (thisGreater && !otherGreater) return CausalRelation.AFTER;
        if (!thisGreater) return CausalRelation.BEFORE;
        return CausalRelation.CONCURRENT;
    }

    /**
     * Merges two clocks by taking the maximum of each counter.
     * CRDT merge — commutative, associative, idempotent.
     *
     * <p>The resulting clock inherits this clock's maxEntries bound.
     * If bounded, entries exceeding the limit are pruned after merge,
     * retaining nodes with the highest counters (most active).</p>
     *
     * @param other the clock to merge with
     * @return a new merged clock
     */
    public VectorClock merge(VectorClock other) {
        Map<String, Long> mergedMap = new HashMap<>(this.clock);
        for (Map.Entry<String, Long> entry : other.clock.entrySet()) {
            mergedMap.merge(entry.getKey(), entry.getValue(), Math::max);
        }
        return new VectorClock(mergedMap, this.maxEntries);
    }

    /**
     * Prunes the clock to maxEntries by removing entries with the
     * lowest counters. Nodes with higher counters are more causally
     * significant — they represent the most active participants.
     *
     * <p>This is a no-op when maxEntries is UNBOUNDED or the clock
     * is within its size limit.</p>
     */
    private void prune() {
        if (maxEntries == UNBOUNDED || clock.size() <= maxEntries) return;

        // Sort entries by counter value descending, keep top maxEntries
        clock.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .skip(maxEntries)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(clock::remove);
    }

    public VectorClock copy() {
        return new VectorClock(this.clock, this.maxEntries);
    }

    public boolean isEmpty() {
        return this.clock.isEmpty();
    }

    /**
     * Returns the number of node entries in this clock.
     * Useful for monitoring clock growth in production.
     */
    public int size() {
        return this.clock.size();
    }

    /**
     * Returns the configured maximum entries for this clock,
     * or UNBOUNDED (-1) if no limit is set.
     */
    public int getMaxEntries() {
        return this.maxEntries;
    }

    public Map<String, Long> getClockState() {
        return Map.copyOf(this.clock);
    }

    @Override
    public String toString() {
        return "VC" + this.clock.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VectorClock other)) return false;
        return this.clock.equals(other.clock);
    }

    @Override
    public int hashCode() {
        return this.clock.hashCode();
    }
}

