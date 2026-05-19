package com.cascade.engine;

import com.cascade.delta.CausalDelta;
import com.cascade.scoring.TrustScorer;
import com.cascade.state.ProductState;

/**
 * ConflictResolver — Resolves concurrent conflicts using trust scoring.
 *
 * <p>When vector clocks detect a CONCURRENT relationship (true conflict),
 * the ConflictResolver uses the TrustScorer to decide whether to accept
 * or reject the incoming delta.</p>
 *
 * <p>Falls back to DegradationManager's timestamp-based resolution
 * when no trust metadata is available.</p>
 */
public class ConflictResolver {

    private final TrustScorer trustScorer;
    private final DegradationManager degradationManager;

    public ConflictResolver(TrustScorer trustScorer, DegradationManager degradationManager) {
        this.trustScorer = trustScorer;
        this.degradationManager = degradationManager;
    }

    /**
     * Resolves a concurrent conflict.
     *
     * @param incoming the conflicting delta
     * @param product  the current product state
     * @return merge result
     */
    public MergeResult resolve(CausalDelta incoming, ProductState product) {
        Double score = trustScorer.computeScore(incoming);

        // No trust metadata → fall back to LWW
        if (score == null) {
            return degradationManager.resolveWithTimestamp(incoming, product);
        }

        // Trust is high enough → apply (MERGE, not overwrite)
        if (trustScorer.isAcceptable(score)) {
            int oldQuantity = product.applyDelta(incoming, MergeResult.ResolutionMode.TRUST_SCORED);
            return MergeResult.appliedWithTrust(
                    incoming.getEventId(), oldQuantity, product.getQuantity(), score);
        }

        // Trust too low → reject
        return MergeResult.lowTrustRejected(incoming.getEventId(), score);
    }
}
