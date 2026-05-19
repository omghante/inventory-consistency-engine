package com.cascade.recovery;

import com.cascade.delta.CausalDelta;
import com.cascade.engine.MergeResult;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * EventStore — Append-only log of all inventory events for replay and recovery.
 */
public class EventStore {
    public record EventEntry(long sequenceNumber, CausalDelta delta, MergeResult result,
                             String workerId, Instant recordedAt) {
        @Override public String toString() {
            String icon = result.isApplied() ? "" : "";
            return String.format("#%d %s [%s] %s delta=%+d → %s (worker: %s)",
                    sequenceNumber, icon, delta.getEventId(), delta.getProductId(),
                    delta.getDelta(), result.getAction(), workerId);
        }
    }

    private final CopyOnWriteArrayList<EventEntry> log = new CopyOnWriteArrayList<>();
    private long sequenceCounter = 0;

    public synchronized long record(CausalDelta delta, MergeResult result, String workerId) {
        long seq = ++sequenceCounter;
        log.add(new EventEntry(seq, delta, result, workerId, Instant.now()));
        return seq;
    }

    public List<EventEntry> getEventsForProduct(String productId) {
        return log.stream().filter(e -> e.delta().getProductId().equals(productId)).collect(Collectors.toList());
    }
    public List<EventEntry> getAppliedEventsForProduct(String productId) {
        return log.stream().filter(e -> e.delta().getProductId().equals(productId))
                .filter(e -> e.result().isApplied()).collect(Collectors.toList());
    }
    public List<EventEntry> getEventsAfter(long afterSequence) {
        return log.stream().filter(e -> e.sequenceNumber() > afterSequence).collect(Collectors.toList());
    }
    public List<EventEntry> getFailedEvents() {
        return log.stream().filter(e -> e.result().isRejected()).collect(Collectors.toList());
    }
    public int reconstructState(String productId, int initialStock) {
        int stock = initialStock;
        for (EventEntry entry : getAppliedEventsForProduct(productId)) stock += entry.delta().getDelta();
        return stock;
    }
    public int size() { return log.size(); }
    public List<EventEntry> getFullLog() { return Collections.unmodifiableList(new ArrayList<>(log)); }
    public synchronized long getLatestSequence() { return sequenceCounter; }

    public String getStats() {
        long applied = log.stream().filter(e -> e.result().isApplied()).count();
        long rejected = log.stream().filter(e -> e.result().isRejected()).count();
        return String.format("Event Store Stats:\n  Total: %,d\n  Applied: %,d\n  Rejected: %,d\n  Latest seq: %d",
                log.size(), applied, rejected, sequenceCounter);
    }
}
