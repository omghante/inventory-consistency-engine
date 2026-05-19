package com.cascade.scoring;

import com.cascade.delta.CausalDelta;
import java.time.Duration;
import java.time.Instant;

/**
 * TrustScorer — Computes trustworthiness of an inventory update.
 */
public class TrustScorer {

    private final Duration maxAcceptableAge;
    private final double acceptanceThreshold;

    public TrustScorer() { this(Duration.ofMinutes(5), 0.5); }

    public TrustScorer(Duration maxAcceptableAge, double acceptanceThreshold) {
        this.maxAcceptableAge = maxAcceptableAge;
        this.acceptanceThreshold = acceptanceThreshold;
    }

    public Double computeScore(CausalDelta delta) {
        if (!delta.hasTrustMetadata()) return null;
        double score = 0; int factors = 0;
        if (delta.getSourceReliability() != null) { score += delta.getSourceReliability(); factors++; }
        score += computeFreshness(delta.getTimestamp()); factors++;
        if (delta.getSource() != null) { score += delta.getSource().getTrustBonus(); factors++; }
        return factors > 0 ? score / factors : null;
    }

    public double computeFreshness(Instant eventTimestamp) {
        long ageMs = Duration.between(eventTimestamp, Instant.now()).toMillis();
        if (ageMs < 0) ageMs = 0;
        return Math.max(0.0, Math.min(1.0, 1.0 - (double) ageMs / maxAcceptableAge.toMillis()));
    }

    public boolean isAcceptable(double score) { return score >= acceptanceThreshold; }
    public double getAcceptanceThreshold() { return acceptanceThreshold; }
}
