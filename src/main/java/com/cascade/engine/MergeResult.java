package com.cascade.engine;

import java.time.Instant;

/**
 * MergeResult — The outcome of processing a CausalDelta through CASCADE.
 */
public class MergeResult {

    public enum Action {
        APPLIED, DUPLICATE_REJECTED, CONDITION_FAILED,
        STALE_REJECTED, LOW_TRUST_REJECTED, TIMESTAMP_REJECTED, UNKNOWN_PRODUCT
    }

    public enum ResolutionMode {
        DIRECT, CAUSAL, TRUST_SCORED, TIMESTAMP_FALLBACK
    }

    private final Action action;
    private final String reason;
    private final String eventId;
    private final ResolutionMode resolutionMode;
    private final Integer oldQuantity;
    private final Integer newQuantity;
    private final Double trustScore;
    private final Instant processedAt;

    private MergeResult(Action action, String eventId, String reason,
                        ResolutionMode mode, Integer oldQty, Integer newQty, Double trust) {
        this.action = action; this.eventId = eventId; this.reason = reason;
        this.resolutionMode = mode; this.oldQuantity = oldQty;
        this.newQuantity = newQty; this.trustScore = trust;
        this.processedAt = Instant.now();
    }

    // Factory methods
    public static MergeResult applied(String eventId, int oldQty, int newQty, ResolutionMode mode) {
        return new MergeResult(Action.APPLIED, eventId, "Delta applied successfully", mode, oldQty, newQty, null);
    }
    public static MergeResult duplicateRejected(String eventId) {
        return new MergeResult(Action.DUPLICATE_REJECTED, eventId, "Event already processed (idempotency)", null, null, null, null);
    }
    public static MergeResult conditionFailed(String eventId, int currentStock, int requiredStock) {
        return new MergeResult(Action.CONDITION_FAILED, eventId,
                String.format("Stock %d < required %d (oversell prevention)", currentStock, requiredStock),
                null, currentStock, null, null);
    }
    public static MergeResult staleRejected(String eventId) {
        return new MergeResult(Action.STALE_REJECTED, eventId, "Causally outdated — vector clock is behind current state", null, null, null, null);
    }
    public static MergeResult lowTrustRejected(String eventId, double score) {
        return new MergeResult(Action.LOW_TRUST_REJECTED, eventId,
                String.format("Trust score %.3f below threshold (concurrent conflict)", score),
                null, null, null, score);
    }
    public static MergeResult timestampRejected(String eventId) {
        return new MergeResult(Action.TIMESTAMP_REJECTED, eventId, "Older timestamp in LWW fallback mode (degraded)", ResolutionMode.TIMESTAMP_FALLBACK, null, null, null);
    }
    public static MergeResult unknownProduct(String eventId, String productId) {
        return new MergeResult(Action.UNKNOWN_PRODUCT, eventId, "Product '" + productId + "' not registered in engine", null, null, null, null);
    }
    public static MergeResult appliedWithTrust(String eventId, int oldQty, int newQty, double score) {
        return new MergeResult(Action.APPLIED, eventId,
                String.format("Concurrent conflict resolved via trust scoring (%.3f)", score),
                ResolutionMode.TRUST_SCORED, oldQty, newQty, score);
    }

    public boolean isApplied() { return action == Action.APPLIED; }
    public boolean isRejected() { return action != Action.APPLIED; }
    public Action getAction() { return action; }
    public String getReason() { return reason; }
    public String getEventId() { return eventId; }
    public ResolutionMode getResolutionMode() { return resolutionMode; }
    public Integer getOldQuantity() { return oldQuantity; }
    public Integer getNewQuantity() { return newQuantity; }
    public Double getTrustScore() { return trustScore; }
    public Instant getProcessedAt() { return processedAt; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(isApplied() ? "" : "").append(action).append(" [").append(eventId).append("] ");
        if (oldQuantity != null && newQuantity != null)
            sb.append("stock: ").append(oldQuantity).append(" → ").append(newQuantity).append(" | ");
        sb.append(reason);
        if (resolutionMode != null) sb.append(" (mode: ").append(resolutionMode).append(")");
        return sb.toString();
    }
}
