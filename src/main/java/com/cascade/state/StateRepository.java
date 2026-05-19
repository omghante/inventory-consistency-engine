package com.cascade.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StateRepository — Thread-safe storage for product states.
 *
 * <p>Provides CRUD operations for product inventory state with
 * concurrent access support via ConcurrentHashMap.</p>
 */
public class StateRepository {

    private final Map<String, ProductState> products = new ConcurrentHashMap<>();

    public void register(String productId, int initialStock) {
        if (productId == null || productId.isBlank())
            throw new IllegalArgumentException("productId cannot be null or blank");
        if (initialStock < 0)
            throw new IllegalArgumentException("Initial stock cannot be negative: " + initialStock);
        products.put(productId, new ProductState(productId, initialStock));
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

    public int getProductCount() {
        return products.size();
    }
}
