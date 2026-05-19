package com.cascade.engine;

import com.cascade.delta.CausalDelta;
import com.cascade.scoring.TrustScorer;
import com.cascade.state.ProductState;
import com.cascade.state.StateRepository;

/**
 * CASCADEEngine — The unified inventory conflict resolution engine.
 *
 * <p><b>CASCADE</b> = <b>C</b>ausal <b>A</b>daptive <b>S</b>cored
 * <b>C</b>onflict-free <b>A</b>lgorithm for <b>D</b>istributed <b>E</b>vents</p>
 *
 * <p>Delegates to internal components:</p>
 * <ul>
 *   <li>{@link MergeProcessor} — The 4-step merge pipeline</li>
 *   <li>{@link ConflictResolver} — Trust-based conflict resolution</li>
 *   <li>{@link DegradationManager} — Graceful fallback to LWW</li>
 *   <li>{@link StateRepository} — Thread-safe product state storage</li>
 * </ul>
 *
 * <p>Thread Safety: Synchronizes on individual ProductState objects.
 * Different products can be processed in parallel.</p>
 */
public class CASCADEEngine {

    private final StateRepository stateRepository;
    private final MergeProcessor mergeProcessor;
    private final TrustScorer trustScorer;

    // Statistics
    private long totalMerges = 0;
    private long appliedCount = 0;
    private long rejectedCount = 0;
    private long conflictsDetected = 0;
    private long duplicatesBlocked = 0;
    private long oversellsPrevented = 0;

    public CASCADEEngine() {
        this(new TrustScorer());
    }

    public CASCADEEngine(TrustScorer trustScorer) {
        this.trustScorer = trustScorer;
        this.stateRepository = new StateRepository();
        DegradationManager degradationManager = new DegradationManager();
        ConflictResolver conflictResolver = new ConflictResolver(trustScorer, degradationManager);
        this.mergeProcessor = new MergeProcessor(conflictResolver, degradationManager);
    }

    public void registerProduct(String productId, int initialStock) {
        stateRepository.register(productId, initialStock);
    }

    /**
     * Processes a CausalDelta through the CASCADE merge pipeline.
     * This is the ONLY public method you need to call.
     */
    public MergeResult merge(CausalDelta incoming) {
        totalMerges++;

        ProductState product = stateRepository.get(incoming.getProductId());
        if (product == null) {
            rejectedCount++;
            return MergeResult.unknownProduct(incoming.getEventId(), incoming.getProductId());
        }

        synchronized (product) {
            MergeResult result = mergeProcessor.process(incoming, product);
            updateStats(result);
            return result;
        }
    }

    private void updateStats(MergeResult result) {
        switch (result.getAction()) {
            case APPLIED -> appliedCount++;
            case DUPLICATE_REJECTED -> { duplicatesBlocked++; rejectedCount++; }
            case CONDITION_FAILED -> { oversellsPrevented++; rejectedCount++; }
            case STALE_REJECTED, LOW_TRUST_REJECTED, TIMESTAMP_REJECTED, UNKNOWN_PRODUCT -> rejectedCount++;
        }
        if (result.getResolutionMode() == MergeResult.ResolutionMode.TRUST_SCORED) {
            conflictsDetected++;
        }
    }

    // Query methods
    public int getStock(String productId) { return stateRepository.getStock(productId); }
    public ProductState getProductState(String productId) { return stateRepository.get(productId); }
    public boolean hasProduct(String productId) { return stateRepository.exists(productId); }

    // Statistics
    public long getTotalMerges() { return totalMerges; }
    public long getAppliedCount() { return appliedCount; }
    public long getRejectedCount() { return rejectedCount; }
    public long getConflictsDetected() { return conflictsDetected; }
    public long getDuplicatesBlocked() { return duplicatesBlocked; }
    public long getOversellsPrevented() { return oversellsPrevented; }

    public String getStats() {
        return String.format("""
                ╔══════════════════════════════════════╗
                ║     CASCADE Engine Statistics         ║
                ╠══════════════════════════════════════╣
                ║  Total merges:       %,8d          ║
                ║  Applied:            %,8d          ║
                ║  Rejected:           %,8d          ║
                ║  Conflicts detected: %,8d          ║
                ║  Duplicates blocked: %,8d          ║
                ║  Oversells prevented:%,8d          ║
                ╚══════════════════════════════════════╝""",
                totalMerges, appliedCount, rejectedCount,
                conflictsDetected, duplicatesBlocked, oversellsPrevented);
    }
}
