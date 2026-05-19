package com.cascade.concurrency;

import com.cascade.engine.CASCADEEngine; import com.cascade.delta.*;
import com.cascade.queue.EventQueue; import com.cascade.worker.WorkerCluster;
import com.cascade.recovery.EventStore;
import org.junit.jupiter.api.*; import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Worker Cluster Load")
class WorkerClusterLoadTest {
    @Test @DisplayName("8 workers, 1000 events, consistent result")
    void clusterLoad() throws Exception {
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",10000);
        EventQueue queue=new EventQueue(); EventStore store=new EventStore();
        for(int i=0;i<1000;i++) queue.enqueue(CausalDelta.builder("e"+i,"PS5","W-"+(i%5),-1).timestamp(Instant.now()).build());
        WorkerCluster cluster=new WorkerCluster(queue,engine,store,8);
        cluster.startAll(); Thread.sleep(1000); cluster.shutdownAll();
        assertEquals(9000, engine.getStock("PS5")); assertEquals(1000, store.size());
    }
}
