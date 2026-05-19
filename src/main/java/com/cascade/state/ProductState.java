package com.cascade.state;

import com.cascade.causality.VectorClock;
import com.cascade.delta.CausalDelta;
import com.cascade.engine.MergeResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * ProductState — The current state of a product within the CASCADE engine.
 */
public class ProductState {

    private final String productId;
    private int quantity;
    private VectorClock vectorClock;
    private final AppliedEventRegistry appliedEvents;
    private final List<EventRecord> eventHistory;

    public record EventRecord(
            String eventId, String warehouseId, int delta,
            Instant timestamp, MergeResult.ResolutionMode resolutionMode
    ) {
        @Override
        public String toString() {
            return String.format("[%s] %s delta=%+d at %s (%s)",
                    eventId, warehouseId, delta, timestamp, resolutionMode);
        }
    }

    public ProductState(String productId, int initialStock) {
        this.productId = productId;
        this.quantity = initialStock;
        this.vectorClock = new VectorClock();
        this.appliedEvents = new AppliedEventRegistry();
        this.eventHistory = new ArrayList<>();
    }

    public boolean hasProcessed(String eventId) {
        return appliedEvents.contains(eventId);
    }

    public int applyDelta(CausalDelta delta, MergeResult.ResolutionMode mode) {
        int oldQuantity = this.quantity;
        this.quantity += delta.getDelta();
        if (delta.hasCausalContext()) {
            this.vectorClock = this.vectorClock.merge(new VectorClock(delta.getCausalContext()));
        }
        this.appliedEvents.register(delta.getEventId());
        this.eventHistory.add(new EventRecord(
                delta.getEventId(), delta.getWarehouseId(),
                delta.getDelta(), delta.getTimestamp(), mode));
        return oldQuantity;
    }

    public Instant getLastEventTimestamp() {
        if (eventHistory.isEmpty()) return null;
        return eventHistory.get(eventHistory.size() - 1).timestamp();
    }

    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public VectorClock getVectorClock() { return vectorClock; }
    public int getEventCount() { return eventHistory.size(); }
    public List<EventRecord> getEventHistory() { return List.copyOf(eventHistory); }

    @Override
    public String toString() {
        return String.format("ProductState{id='%s', qty=%d, events=%d, vc=%s}",
                productId, quantity, eventHistory.size(), vectorClock);
    }
}
