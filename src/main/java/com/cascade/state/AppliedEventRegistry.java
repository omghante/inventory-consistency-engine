package com.cascade.state;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * AppliedEventRegistry — Tracks which event IDs have been processed.
 *
 * <p>Core of CASCADE's idempotency guarantee. Prevents duplicate
 * processing even during network retries and queue re-deliveries.</p>
 */
public class AppliedEventRegistry {

    private final Set<String> processedIds;

    public AppliedEventRegistry() {
        this.processedIds = new HashSet<>();
    }

    /** Checks if an event has already been processed. */
    public boolean contains(String eventId) {
        return processedIds.contains(eventId);
    }

    /** Registers an event as processed. */
    public void register(String eventId) {
        processedIds.add(eventId);
    }

    /** Returns the number of processed events. */
    public int size() {
        return processedIds.size();
    }

    /** Returns an unmodifiable view of all processed event IDs. */
    public Set<String> getAll() {
        return Collections.unmodifiableSet(processedIds);
    }
}
