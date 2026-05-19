package com.cascade.integration;

import com.cascade.engine.CASCADEEngine;
import com.cascade.delta.*; import com.cascade.queue.EventQueue;
import com.cascade.worker.WorkerCluster; import com.cascade.recovery.EventStore;
import org.junit.jupiter.api.*; import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Full Pipeline Integration")
class FullPipelineIntegrationTest {
    @Test @DisplayName("Queue → Workers → Engine → EventStore flow")
    void fullFlow() throws Exception {
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",100);
        EventQueue queue=new EventQueue(); EventStore store=new EventStore();
        for(int i=0;i<50;i++) queue.enqueue(CausalDelta.builder("e"+i,"PS5","MUM",-1).timestamp(Instant.now()).build());
        WorkerCluster cluster=new WorkerCluster(queue,engine,store,4);
        cluster.startAll(); Thread.sleep(500); cluster.shutdownAll();
        assertEquals(50, engine.getStock("PS5")); assertEquals(50, store.size());
        assertEquals(50, store.reconstructState("PS5",100));
    }
}
