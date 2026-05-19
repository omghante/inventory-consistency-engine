package com.cascade.recovery;

import com.cascade.engine.CASCADEEngine;
import com.cascade.state.InventorySnapshot;

/**
 * RecoveryCoordinator — Orchestrates full recovery from snapshot + event log.
 */
public class RecoveryCoordinator {
    private final SnapshotManager snapshotManager;
    private final ReplayEngine replayEngine;

    public RecoveryCoordinator(SnapshotManager snapshotManager, ReplayEngine replayEngine) {
        this.snapshotManager = snapshotManager; this.replayEngine = replayEngine;
    }

    public RecoveryResult recover(CASCADEEngine targetEngine, String productId, int defaultInitialStock) {
        InventorySnapshot snapshot = snapshotManager.getLatestSnapshot(productId);
        if (snapshot != null) {
            targetEngine.registerProduct(productId, snapshot.quantity());
            ReplayEngine.ReplayResult replay = replayEngine.replayAfter(targetEngine, snapshot.lastSequenceNumber());
            return new RecoveryResult(true, snapshot.lastSequenceNumber(), replay.applied(), targetEngine.getStock(productId));
        }
        ReplayEngine.ReplayResult replay = replayEngine.replayAll(targetEngine, productId, defaultInitialStock);
        return new RecoveryResult(false, 0, replay.applied(), replay.finalStock());
    }

    public record RecoveryResult(boolean fromSnapshot, long snapshotSequence, int eventsReplayed, int finalStock) {
        @Override public String toString() {
            return String.format("Recovery: snapshot=%s, seq=%d, replayed=%d, stock=%d",
                    fromSnapshot, snapshotSequence, eventsReplayed, finalStock);
        }
    }
}
