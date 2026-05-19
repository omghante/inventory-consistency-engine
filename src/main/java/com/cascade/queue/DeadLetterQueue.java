package com.cascade.queue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * DeadLetterQueue — Captures permanently failed events for manual inspection.
 */
public class DeadLetterQueue {
    private final CopyOnWriteArrayList<EventQueue.QueuedEvent> events = new CopyOnWriteArrayList<>();
    public void add(EventQueue.QueuedEvent event) { events.add(event); }
    public List<EventQueue.QueuedEvent> getAll() { return Collections.unmodifiableList(events); }
    public int size() { return events.size(); }
    public void clear() { events.clear(); }
}
