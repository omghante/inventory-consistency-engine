package com.cascade.delta;

import java.time.Instant;
import java.util.Map;

/**
 * DeltaMetadata — Optional metadata attached to a CausalDelta.
 *
 * <p>Encapsulates all optional fields that enable CASCADE's adaptive behavior.
 * The richness of this metadata determines which resolution strategies are available:</p>
 *
 * <pre>
 *   Full metadata  → Vector Clocks + Trust Scoring + Preconditions
 *   No trust data  → Vector Clocks + Timestamp Fallback
 *   No VC          → Direct application
 * </pre>
 */
public record DeltaMetadata(
        Map<String, Long> causalContext,
        SourceType source,
        Double sourceReliability,
        Preconditions preconditions
) {
    /** Returns true if causal context (vector clock) is available. */
    public boolean hasCausalContext() {
        return causalContext != null && !causalContext.isEmpty();
    }

    /** Returns true if trust metadata is available. */
    public boolean hasTrustMetadata() {
        return source != null || sourceReliability != null;
    }

    /** Returns true if preconditions are attached. */
    public boolean hasPreconditions() {
        return preconditions != null;
    }

    /** Creates metadata with no optional fields (bare delta). */
    public static DeltaMetadata empty() {
        return new DeltaMetadata(null, null, null, null);
    }
}
