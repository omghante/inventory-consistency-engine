package com.cascade.exceptions;

/**
 * ConflictException — Thrown when an unresolvable conflict is detected.
 */
public class ConflictException extends CascadeException {
    private final String eventId;
    private final String productId;
    public ConflictException(String eventId, String productId, String message) {
        super(message); this.eventId = eventId; this.productId = productId;
    }
    public String getEventId() { return eventId; }
    public String getProductId() { return productId; }
}
