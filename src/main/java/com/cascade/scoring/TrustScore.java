package com.cascade.scoring;

/**
 * TrustScore — Immutable value object representing a computed trust score.
 */
public record TrustScore(double value, double reliability, double freshness, double sourceBonus) {
    public boolean isAcceptable(double threshold) { return value >= threshold; }

    @Override
    public String toString() {
        return String.format("TrustScore{%.3f (rel=%.2f, fresh=%.2f, src=%.2f)}",
                value, reliability, freshness, sourceBonus);
    }
}
