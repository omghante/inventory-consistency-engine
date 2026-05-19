package com.cascade.engine;

import com.cascade.causality.CausalRelation;
import com.cascade.causality.VectorClock;
import com.cascade.delta.CausalDelta;
import com.cascade.state.ProductState;

/**
 * MergeProcessor — Executes the 4-step CASCADE merge pipeline.
 *
 * <p>Extracted from CASCADEEngine for single-responsibility.
 * The pipeline:</p>
 * <ol>
 *   <li>Idempotency check (eventId)</li>
 *   <li>Precondition check (minStock)</li>
 *   <li>Causal ordering (vector clocks)</li>
 *   <li>Delta application</li>
 * </ol>
 */
public class MergeProcessor {

    private final ConflictResolver conflictResolver;
    private final DegradationManager degradationManager;

    public MergeProcessor(ConflictResolver conflictResolver, DegradationManager degradationManager) {
        this.conflictResolver = conflictResolver;
        this.degradationManager = degradationManager;
    }

    /**
     * Processes a CausalDelta through the 4-step merge pipeline.
     *
     * @param incoming the inventory event
     * @param product  the current product state (caller holds lock)
     * @return the merge result
     */
    public MergeResult process(CausalDelta incoming, ProductState product) {

        // STEP 1: IDEMPOTENCY
        if (product.hasProcessed(incoming.getEventId())) {
            return MergeResult.duplicateRejected(incoming.getEventId());
        }

        // STEP 2: PRECONDITION
        if (incoming.hasPrecondition()) {
            if (!incoming.getPrecondition().isSatisfied(product.getQuantity())) {
                return MergeResult.conditionFailed(
                        incoming.getEventId(), product.getQuantity(),
                        incoming.getPrecondition().minStock());
            }
        }

        // STEP 3: CAUSALITY
        if (incoming.hasCausalContext() && !product.getVectorClock().isEmpty()) {
            VectorClock incomingVC = new VectorClock(incoming.getCausalContext());
            CausalRelation relation = incomingVC.compare(product.getVectorClock());

            switch (relation) {
                case BEFORE:
                    return MergeResult.staleRejected(incoming.getEventId());
                case CONCURRENT:
                    return conflictResolver.resolve(incoming, product);
                case AFTER:
                case EQUAL:
                    break; // fall through to step 4
            }
        }

        // STEP 4: APPLY DELTA
        MergeResult.ResolutionMode mode = degradationManager.determineMode(incoming);
        int oldQuantity = product.applyDelta(incoming, mode);
        return MergeResult.applied(incoming.getEventId(), oldQuantity, product.getQuantity(), mode);
    }
}
