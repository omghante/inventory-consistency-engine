package com.cascade.delta;

import java.time.Instant;
import java.util.Map;

/**
 * CausalDelta — The self-describing inventory event.
 *
 * <p>Core data structure of CASCADE. Every inventory update is wrapped
 * in a CausalDelta that carries enough metadata to enable the best
 * possible resolution strategy — and gracefully degrades when metadata
 * is missing.</p>
 */
public class CausalDelta {

    private final String eventId;
    private final String productId;
    private final String warehouseId;
    private final int delta;
    private final Instant timestamp;
    private final DeltaMetadata metadata;

    private CausalDelta(Builder builder) {
        this.eventId = builder.eventId;
        this.productId = builder.productId;
        this.warehouseId = builder.warehouseId;
        this.delta = builder.delta;
        this.timestamp = builder.timestamp;
        this.metadata = new DeltaMetadata(
                builder.causalContext,
                builder.source,
                builder.sourceReliability,
                builder.preconditions
        );
    }

    public static class Builder {
        private final String eventId;
        private final String productId;
        private final String warehouseId;
        private final int delta;
        private Instant timestamp = Instant.now();
        private Map<String, Long> causalContext = null;
        private SourceType source = null;
        private Double sourceReliability = null;
        private Preconditions preconditions = null;

        public Builder(String eventId, String productId, String warehouseId, int delta) {
            this.eventId = eventId;
            this.productId = productId;
            this.warehouseId = warehouseId;
            this.delta = delta;
        }

        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder causalContext(Map<String, Long> ctx) {
            this.causalContext = ctx != null ? Map.copyOf(ctx) : null; return this;
        }
        public Builder source(SourceType source) { this.source = source; return this; }
        public Builder sourceReliability(double r) { this.sourceReliability = r; return this; }
        public Builder precondition(Preconditions p) { this.preconditions = p; return this; }

        public CausalDelta build() {
            if (eventId == null || eventId.isBlank())
                throw new IllegalArgumentException("eventId is required");
            if (productId == null || productId.isBlank())
                throw new IllegalArgumentException("productId is required");
            if (warehouseId == null || warehouseId.isBlank())
                throw new IllegalArgumentException("warehouseId is required");
            return new CausalDelta(this);
        }
    }

    public static Builder builder(String eventId, String productId, String warehouseId, int delta) {
        return new Builder(eventId, productId, warehouseId, delta);
    }

    // Metadata-aware checks (delegate to DeltaMetadata)
    public boolean hasCausalContext() { return metadata.hasCausalContext(); }
    public boolean hasTrustMetadata() { return metadata.hasTrustMetadata(); }
    public boolean hasPrecondition() { return metadata.hasPreconditions(); }

    // Getters
    public String getEventId() { return eventId; }
    public String getProductId() { return productId; }
    public String getWarehouseId() { return warehouseId; }
    public int getDelta() { return delta; }
    public Instant getTimestamp() { return timestamp; }
    public DeltaMetadata getMetadata() { return metadata; }
    public Map<String, Long> getCausalContext() { return metadata.causalContext(); }
    public SourceType getSource() { return metadata.source(); }
    public Double getSourceReliability() { return metadata.sourceReliability(); }
    public Preconditions getPrecondition() { return metadata.preconditions(); }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CausalDelta{");
        sb.append("id='").append(eventId).append('\'');
        sb.append(", product='").append(productId).append('\'');
        sb.append(", warehouse='").append(warehouseId).append('\'');
        sb.append(", delta=").append(delta > 0 ? "+" : "").append(delta);
        if (hasCausalContext()) sb.append(", vc=").append(getCausalContext());
        if (getSource() != null) sb.append(", src=").append(getSource());
        if (hasPrecondition()) sb.append(", minStock=").append(getPrecondition().minStock());
        sb.append('}');
        return sb.toString();
    }
}
