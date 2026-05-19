package com.cascade.state;

import java.time.Instant;
import java.util.Map;

/**
 * InventorySnapshot — An immutable point-in-time snapshot of product state.
 *
 * <p>Used by SnapshotManager for periodic checkpointing. Enables faster
 * recovery by replaying only events after the last snapshot.</p>
 */
public record InventorySnapshot(
        String productId,
        int quantity,
        Map<String, Long> vectorClockState,
        long lastSequenceNumber,
        Instant snapshotAt
) {
    @Override
    public String toString() {
        return String.format("Snapshot{product='%s', qty=%d, seq=%d, at=%s}",
                productId, quantity, lastSequenceNumber, snapshotAt);
    }
}
