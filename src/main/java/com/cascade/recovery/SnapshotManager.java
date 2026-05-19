package com.cascade.recovery;

import com.cascade.engine.CASCADEEngine;
import com.cascade.state.InventorySnapshot;
import com.cascade.state.ProductState;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SnapshotManager — Periodic checkpointing for faster recovery.
 */
public class SnapshotManager {
    private final ConcurrentHashMap<String, InventorySnapshot> snapshots = new ConcurrentHashMap<>();

    public InventorySnapshot takeSnapshot(ProductState product, long sequenceNumber) {
        InventorySnapshot snap = new InventorySnapshot(product.getProductId(), product.getQuantity(),
                product.getVectorClock().getClockState(), sequenceNumber, Instant.now());
        snapshots.put(product.getProductId(), snap);
        return snap;
    }

    public InventorySnapshot getLatestSnapshot(String productId) { return snapshots.get(productId); }
    public boolean hasSnapshot(String productId) { return snapshots.containsKey(productId); }
    public int getSnapshotCount() { return snapshots.size(); }
}
