package com.cascade.recovery;

import com.cascade.delta.CausalDelta;
import com.cascade.engine.CASCADEEngine;
import com.cascade.engine.MergeResult;
import com.cascade.state.InventorySnapshot;
import java.util.List;

/**
 * ReplayEngine — Rebuilds engine state by replaying events from EventStore.
 */
public class ReplayEngine {
    private final EventStore eventStore;

    public ReplayEngine(EventStore eventStore) { this.eventStore = eventStore; }

    public ReplayResult replayAll(CASCADEEngine targetEngine, String productId, int initialStock) {
        targetEngine.registerProduct(productId, initialStock);
        List<EventStore.EventEntry> events = eventStore.getAppliedEventsForProduct(productId);
        int applied = 0; int skipped = 0;
        for (EventStore.EventEntry entry : events) {
            MergeResult result = targetEngine.merge(entry.delta());
            if (result.isApplied()) applied++; else skipped++;
        }
        return new ReplayResult(events.size(), applied, skipped, targetEngine.getStock(productId));
    }

    public ReplayResult replayAfter(CASCADEEngine targetEngine, long afterSequence) {
        List<EventStore.EventEntry> events = eventStore.getEventsAfter(afterSequence);
        int applied = 0; int skipped = 0;
        for (EventStore.EventEntry entry : events) {
            if (!entry.result().isApplied()) { skipped++; continue; }
            MergeResult result = targetEngine.merge(entry.delta());
            if (result.isApplied()) applied++; else skipped++;
        }
        return new ReplayResult(events.size(), applied, skipped, -1);
    }

    public record ReplayResult(int totalEvents, int applied, int skipped, int finalStock) {
        @Override public String toString() {
            return String.format("Replay: %d events, %d applied, %d skipped, stock=%d",
                    totalEvents, applied, skipped, finalStock);
        }
    }
}
