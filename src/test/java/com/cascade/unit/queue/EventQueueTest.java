package com.cascade.unit.queue;

import com.cascade.queue.EventQueue;
import com.cascade.queue.EventQueue.QueuedEvent;
import com.cascade.delta.CausalDelta;
import org.junit.jupiter.api.*;
import java.time.Duration;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EventQueue Tests")
class EventQueueTest {
    @Test void enqueueAndPoll() throws Exception {
        EventQueue q = new EventQueue();
        assertTrue(q.enqueue(CausalDelta.builder("e1","PS5","MUM",-1).timestamp(Instant.now()).build()));
        QueuedEvent e = q.poll(Duration.ofMillis(100));
        assertNotNull(e); assertEquals("e1", e.delta().getEventId()); }

    @Test void backpressure() {
        EventQueue q = new EventQueue(3, 3, Duration.ofMillis(100));
        for (int i=0;i<3;i++) assertTrue(q.enqueue(CausalDelta.builder("e"+i,"PS5","MUM",-1).timestamp(Instant.now()).build()));
        assertFalse(q.enqueue(CausalDelta.builder("overflow","PS5","MUM",-1).timestamp(Instant.now()).build()));
        assertEquals(1, q.getTotalDropped()); }

    @Test void pollTimeout() throws Exception {
        assertNull(new EventQueue().poll(Duration.ofMillis(50))); }

    @Test void deadLetterQueue() {
        EventQueue q = new EventQueue(100,2,Duration.ofMillis(10));
        QueuedEvent e = QueuedEvent.fresh(CausalDelta.builder("e1","PS5","MUM",-1).timestamp(Instant.now()).build());
        q.reportFailure(e, "err1"); q.reportFailure(e.retry("err1"), "err2");
        assertEquals(1, q.getDLQSize()); }

    @Test void metrics() {
        EventQueue q = new EventQueue();
        q.enqueue(CausalDelta.builder("e1","PS5","MUM",-1).timestamp(Instant.now()).build());
        q.enqueue(CausalDelta.builder("e2","PS5","MUM",-1).timestamp(Instant.now()).build());
        assertEquals(2, q.getTotalEnqueued()); assertEquals(2, q.getQueueSize()); }
}
