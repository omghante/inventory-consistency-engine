package com.cascade.causality;

/**
 * CausalRelation — Possible causal relationships between two vector clocks.
 *
 * <p>Based on Lamport's "happens-before" relation and used in Amazon's Dynamo
 * to determine whether events are causally ordered or truly concurrent.</p>
 */
public enum CausalRelation {
    /** This clock is strictly newer (dominates the other). */
    AFTER,
    /** This clock is strictly older (dominated by the other). */
    BEFORE,
    /** Neither dominates — true concurrent conflict. */
    CONCURRENT,
    /** Both clocks are identical. */
    EQUAL
}
