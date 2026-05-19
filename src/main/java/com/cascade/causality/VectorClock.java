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
 */
public class VectorClock {

    private final Map<String, Long> clock;

    public VectorClock() {
        this.clock = new HashMap<>();
    }

    public VectorClock(Map<String, Long> initial) {
        this.clock = new HashMap<>(initial);
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
     * @param other the clock to merge with
     * @return a new merged clock
     */
    public VectorClock merge(VectorClock other) {
        VectorClock merged = new VectorClock(this.clock);
        for (Map.Entry<String, Long> entry : other.clock.entrySet()) {
            merged.clock.merge(entry.getKey(), entry.getValue(), Math::max);
        }
        return merged;
    }

    public VectorClock copy() {
        return new VectorClock(this.clock);
    }

    public boolean isEmpty() {
        return this.clock.isEmpty();
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
