package com.cascade.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * ConflictMetrics — Tracks conflict patterns by product and warehouse.
 */
public class ConflictMetrics {
    private final Map<String, LongAdder> conflictsByProduct = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> conflictsByWarehouse = new ConcurrentHashMap<>();

    public void recordConflict(String productId, String warehouseId) {
        conflictsByProduct.computeIfAbsent(productId, k -> new LongAdder()).increment();
        conflictsByWarehouse.computeIfAbsent(warehouseId, k -> new LongAdder()).increment();
    }

    public long getConflictsForProduct(String productId) {
        LongAdder a = conflictsByProduct.get(productId);
        return a != null ? a.sum() : 0;
    }
    public long getConflictsForWarehouse(String warehouseId) {
        LongAdder a = conflictsByWarehouse.get(warehouseId);
        return a != null ? a.sum() : 0;
    }
    public int getAffectedProductCount() { return conflictsByProduct.size(); }
}
