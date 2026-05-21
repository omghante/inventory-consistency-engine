package com.cascade.engine;

import com.cascade.delta.CausalDelta;
import com.cascade.scoring.TrustScorer;
import com.cascade.state.ProductState;
import com.cascade.state.StateRepository;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;

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
 * <p>Thread Safety: Uses StampedLock per ProductState for concurrent access.
 * Write operations (merge) acquire a write lock on the individual product.
 * Read operations (getStock) use optimistic reads for zero-contention queries.
 * Different products are fully independent — no cross-product locking.</p>
 *
 * <p>All engine-level statistics use AtomicLong counters for lock-free
 * thread-safe accumulation under concurrent load.</p>
 */
public class CASCADEEngine {

    private final StateRepository stateRepository;
    private final MergeProcessor mergeProcessor;
    private final TrustScorer trustScorer;

    // Thread-safe statistics — AtomicLong eliminates data races on counters
    // that are incremented from multiple worker threads concurrently.
    private final AtomicLong totalMerges = new AtomicLong(0);
    private final AtomicLong appliedCount = new AtomicLong(0);
    private final AtomicLong rejectedCount = new AtomicLong(0);
    private final AtomicLong conflictsDetected = new AtomicLong(0);
    private final AtomicLong duplicatesBlocked = new AtomicLong(0);
    private final AtomicLong oversellsPrevented = new AtomicLong(0);

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
     *
     * <p>Uses StampedLock write-lock on the target product for mutation safety.
     * Different products acquire independent locks — no global bottleneck.</p>
     */
    public MergeResult merge(CausalDelta incoming) {
        totalMerges.incrementAndGet();

        ProductState product = stateRepository.get(incoming.getProductId());
        if (product == null) {
            rejectedCount.incrementAndGet();
            return MergeResult.unknownProduct(incoming.getEventId(), incoming.getProductId());
        }

        StampedLock lock = stateRepository.getLock(incoming.getProductId());
        long stamp = lock.writeLock();
        try {
            MergeResult result = mergeProcessor.process(incoming, product);
            updateStats(result);
            return result;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    private void updateStats(MergeResult result) {
        switch (result.getAction()) {
            case APPLIED -> appliedCount.incrementAndGet();
            case DUPLICATE_REJECTED -> { duplicatesBlocked.incrementAndGet(); rejectedCount.incrementAndGet(); }
            case CONDITION_FAILED -> { oversellsPrevented.incrementAndGet(); rejectedCount.incrementAndGet(); }
            case STALE_REJECTED, LOW_TRUST_REJECTED, TIMESTAMP_REJECTED, UNKNOWN_PRODUCT -> rejectedCount.incrementAndGet();
        }
        if (result.getResolutionMode() == MergeResult.ResolutionMode.TRUST_SCORED) {
            conflictsDetected.incrementAndGet();
        }
    }

    /**
     * Returns current stock for a product using optimistic read.
     *
     * <p>StampedLock optimistic reads avoid acquiring any lock for read-only
     * queries. Under flash sale conditions where stock is queried far more
     * frequently than modified, this eliminates read-write contention entirely.</p>
     *
     * <p>If the optimistic read detects a concurrent write (stamp validation
     * fails), it falls back to a full read lock for a consistent snapshot.</p>
     */
    public int getStock(String productId) {
        StampedLock lock = stateRepository.getLock(productId);
        if (lock == null) return -1;

        // Optimistic read — no lock acquired, zero contention
        long stamp = lock.tryOptimisticRead();
        int stock = stateRepository.getStock(productId);
        if (lock.validate(stamp)) {
            return stock;
        }

        // Concurrent write detected — fall back to read lock
        stamp = lock.readLock();
        try {
            return stateRepository.getStock(productId);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public ProductState getProductState(String productId) { return stateRepository.get(productId); }
    public boolean hasProduct(String productId) { return stateRepository.exists(productId); }

    // Statistics — all reads are atomic, no tearing on 64-bit values
    public long getTotalMerges() { return totalMerges.get(); }
    public long getAppliedCount() { return appliedCount.get(); }
    public long getRejectedCount() { return rejectedCount.get(); }
    public long getConflictsDetected() { return conflictsDetected.get(); }
    public long getDuplicatesBlocked() { return duplicatesBlocked.get(); }
    public long getOversellsPrevented() { return oversellsPrevented.get(); }

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
                totalMerges.get(), appliedCount.get(), rejectedCount.get(),
                conflictsDetected.get(), duplicatesBlocked.get(), oversellsPrevented.get());
    }
}
