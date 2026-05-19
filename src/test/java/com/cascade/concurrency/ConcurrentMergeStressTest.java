package com.cascade.concurrency;

import com.cascade.engine.CASCADEEngine; import com.cascade.delta.*;
import org.junit.jupiter.api.*; import java.time.Instant;
import java.util.concurrent.*; import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Concurrent Merge Stress")
class ConcurrentMergeStressTest {
    @Test @DisplayName("10K concurrent merges — zero data loss")
    void tenKConcurrent() throws Exception {
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",1_000_000);
        int total=10_000,threads=8; ExecutorService pool=Executors.newFixedThreadPool(threads);
        CountDownLatch latch=new CountDownLatch(total); AtomicInteger c=new AtomicInteger();
        for(int i=0;i<total;i++) pool.submit(()->{int id=c.incrementAndGet();
            engine.merge(CausalDelta.builder("e"+id,"PS5","W-"+(id%5),-1).timestamp(Instant.now()).build()); latch.countDown();});
        latch.await(); pool.shutdown();
        assertEquals(1_000_000-total, engine.getStock("PS5"));
    }
}
