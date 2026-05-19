package com.cascade.concurrency;

import com.cascade.engine.CASCADEEngine; import com.cascade.delta.*;
import org.junit.jupiter.api.*; import java.time.Instant;
import java.util.concurrent.*; import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Flash Sale Simulation")
class FlashSaleSimulationTest {
    @Test @DisplayName("Zero overselling under concurrent load")
    void zeroOverselling() throws Exception {
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",200);
        int total=1000; ExecutorService pool=Executors.newFixedThreadPool(16);
        CountDownLatch latch=new CountDownLatch(total); AtomicInteger c=new AtomicInteger();
        for(int i=0;i<total;i++) pool.submit(()->{int id=c.incrementAndGet();
            engine.merge(CausalDelta.builder("o"+id,"PS5","W-"+(id%3),-1).timestamp(Instant.now())
                    .precondition(new Preconditions(1)).build()); latch.countDown();});
        latch.await(); pool.shutdown();
        assertTrue(engine.getStock("PS5")>=0); assertEquals(0, engine.getStock("PS5"));
    }
}
