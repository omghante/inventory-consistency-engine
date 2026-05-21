package com.cascade.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

/**
 * StateRepository — Thread-safe storage for product states.
 *
 * <p>Provides CRUD operations for product inventory state with
 * concurrent access support via ConcurrentHashMap.</p>
 *
 * <p>Each registered product is assigned an independent StampedLock,
 * enabling fine-grained concurrency control at the product level.
 * Write operations acquire the write lock for mutation safety.
 * Read operations use optimistic reads for zero-contention queries.</p>
 */
public class StateRepository {

    private final Map<String, ProductState> products = new ConcurrentHashMap<>();
    private final Map<String, StampedLock> locks = new ConcurrentHashMap<>();

    public void register(String productId, int initialStock) {
        if (productId == null || productId.isBlank())
            throw new IllegalArgumentException("productId cannot be null or blank");
        if (initialStock < 0)
            throw new IllegalArgumentException("Initial stock cannot be negative: " + initialStock);
        products.put(productId, new ProductState(productId, initialStock));
        locks.put(productId, new StampedLock());
    }

    public ProductState get(String productId) {
        return products.get(productId);
    }

    public boolean exists(String productId) {
        return products.containsKey(productId);
    }

    public int getStock(String productId) {
        ProductState state = products.get(productId);
        return state != null ? state.getQuantity() : -1;
    }

    /**
     * Returns the StampedLock for a product, or null if the product
     * is not registered.
     *
     * <p>Callers use this lock for fine-grained concurrency control:
     * write-lock for merge operations, optimistic-read for stock queries.</p>
     */
    public StampedLock getLock(String productId) {
        return locks.get(productId);
    }

    public int getProductCount() {
        return products.size();
    }
}

