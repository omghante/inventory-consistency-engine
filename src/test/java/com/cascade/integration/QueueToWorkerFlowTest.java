package com.cascade.integration;

import com.cascade.engine.CASCADEEngine; import com.cascade.delta.*;
import com.cascade.queue.EventQueue; import com.cascade.worker.Worker;
import com.cascade.recovery.EventStore;
import org.junit.jupiter.api.*; import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Queue-to-Worker Flow")
class QueueToWorkerFlowTest {
    @Test void singleWorkerProcessesAll() throws Exception {
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",100);
        EventQueue queue=new EventQueue(); EventStore store=new EventStore();
        for(int i=0;i<20;i++) queue.enqueue(CausalDelta.builder("e"+i,"PS5","MUM",-1).timestamp(Instant.now()).build());
        Worker w=new Worker("w1",queue,engine,store); w.start(); Thread.sleep(300); w.shutdown();
        assertEquals(80, engine.getStock("PS5")); assertEquals(20, w.getEventsSucceeded());
    }
}
