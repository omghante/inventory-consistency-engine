package com.cascade.performance;

import com.cascade.engine.CASCADEEngine; import com.cascade.delta.*;
import org.junit.jupiter.api.*; import java.time.*; import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Throughput Benchmark")
class ThroughputBenchmarkTest {
    @Test @DisplayName("Sustains >100K merges/sec")
    void throughputAbove100K() throws Exception {
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",10_000_000);
        int total=50_000; ExecutorService pool=Executors.newFixedThreadPool(8);
        CountDownLatch latch=new CountDownLatch(total); AtomicInteger c=new AtomicInteger();
        Instant start=Instant.now();
        for(int i=0;i<total;i++) pool.submit(()->{int id=c.incrementAndGet();
            engine.merge(CausalDelta.builder("e"+id,"PS5","W-"+(id%5),-1).timestamp(Instant.now()).build()); latch.countDown();});
        latch.await(); pool.shutdown();
        long tp=(total*1000L)/Math.max(1,Duration.between(start,Instant.now()).toMillis());
        assertTrue(tp>100_000, "Throughput should be >100K, was "+tp);
    }
}
