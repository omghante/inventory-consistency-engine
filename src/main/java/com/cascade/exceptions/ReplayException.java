package com.cascade.exceptions;

/**
 * ReplayException — Thrown when event replay fails.
 */
public class ReplayException extends CascadeException {
    private final long failedAtSequence;
    public ReplayException(long failedAtSequence, String message) {
        super(message); this.failedAtSequence = failedAtSequence;
    }
    public long getFailedAtSequence() { return failedAtSequence; }
}
