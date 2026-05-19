package com.cascade.engine;

import com.cascade.delta.CausalDelta;
import com.cascade.state.ProductState;

import java.time.Instant;

/**
 * DegradationManager — Controls graceful degradation behavior.
 *
 * <p>When metadata is missing, CASCADE doesn't crash — it degrades
 * to simpler resolution strategies. This manager handles the
 * timestamp-based LWW fallback.</p>
 *
 * <p>Degradation ladder:</p>
 * <pre>
 *   Full metadata  → CAUSAL + TRUST_SCORED
 *   No trust data  → CAUSAL + TIMESTAMP_FALLBACK
 *   No vector clock → DIRECT
 *   Only eventId   → DIRECT (still idempotent)
 * </pre>
 */
public class DegradationManager {

    /**
     * Resolves a conflict using timestamp comparison (Last-Write-Wins).
     * Used as final fallback when no trust metadata is available.
     *
     * @param incoming the delta to resolve
     * @param product  the current product state
     * @return merge result
     */
    public MergeResult resolveWithTimestamp(CausalDelta incoming, ProductState product) {
        Instant lastTimestamp = product.getLastEventTimestamp();

        // No previous events → apply
        if (lastTimestamp == null) {
            return applyDelta(incoming, product, MergeResult.ResolutionMode.TIMESTAMP_FALLBACK);
        }

        // Incoming is newer → apply
        if (incoming.getTimestamp().isAfter(lastTimestamp)) {
            return applyDelta(incoming, product, MergeResult.ResolutionMode.TIMESTAMP_FALLBACK);
        }

        // Incoming is older → reject
        return MergeResult.timestampRejected(incoming.getEventId());
    }

    /**
     * Determines the resolution mode based on available metadata.
     *
     * @param delta the incoming delta
     * @return the appropriate resolution mode
     */
    public MergeResult.ResolutionMode determineMode(CausalDelta delta) {
        if (delta.hasCausalContext()) return MergeResult.ResolutionMode.CAUSAL;
        return MergeResult.ResolutionMode.DIRECT;
    }

    private MergeResult applyDelta(CausalDelta incoming, ProductState product,
                                   MergeResult.ResolutionMode mode) {
        int oldQuantity = product.applyDelta(incoming, mode);
        return MergeResult.applied(incoming.getEventId(), oldQuantity, product.getQuantity(), mode);
    }
}
