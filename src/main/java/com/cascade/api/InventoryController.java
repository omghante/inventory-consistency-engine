package com.cascade.api;

import com.cascade.delta.CausalDelta;
import com.cascade.engine.CASCADEEngine;
import com.cascade.engine.MergeResult;

/**
 * InventoryController — REST API facade for inventory operations.
 */
public class InventoryController {
    private final CASCADEEngine engine;
    public InventoryController(CASCADEEngine engine) { this.engine = engine; }

    public MergeResult processEvent(CausalDelta delta) { return engine.merge(delta); }
    public int getStock(String productId) { return engine.getStock(productId); }
    public void registerProduct(String productId, int initialStock) { engine.registerProduct(productId, initialStock); }
}
