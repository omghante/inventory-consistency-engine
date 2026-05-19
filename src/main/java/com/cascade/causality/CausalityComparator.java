package com.cascade.causality;

/**
 * CausalityComparator — Utility for comparing causal contexts.
 *
 * <p>Provides static methods for comparing vector clock maps directly,
 * without requiring VectorClock object construction. Useful for
 * lightweight comparisons in hot paths.</p>
 */
public final class CausalityComparator {

    private CausalityComparator() {}

    /**
     * Determines if the incoming clock is causally newer than the stored clock.
     *
     * @param incoming the incoming event's clock
     * @param stored   the current stored clock
     * @return true if incoming is strictly AFTER stored
     */
    public static boolean isNewer(VectorClock incoming, VectorClock stored) {
        return incoming.compare(stored) == CausalRelation.AFTER;
    }

    /**
     * Determines if two clocks represent concurrent events.
     *
     * @param a first clock
     * @param b second clock
     * @return true if neither dominates the other
     */
    public static boolean isConcurrent(VectorClock a, VectorClock b) {
        return a.compare(b) == CausalRelation.CONCURRENT;
    }

    /**
     * Determines if the incoming clock is outdated.
     *
     * @param incoming the incoming event's clock
     * @param stored   the current stored clock
     * @return true if incoming is strictly BEFORE stored
     */
    public static boolean isStale(VectorClock incoming, VectorClock stored) {
        return incoming.compare(stored) == CausalRelation.BEFORE;
    }
}
