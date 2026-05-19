package com.cascade.performance;

import com.cascade.engine.CASCADEEngine; import com.cascade.delta.*;
import org.junit.jupiter.api.*; import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Memory Usage Benchmark")
class MemoryUsageBenchmarkTest {
    @Test @DisplayName("10K events don't cause OOM")
    void memoryStability() {
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",1_000_000);
        long before=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        for(int i=0;i<10_000;i++) engine.merge(CausalDelta.builder("e"+i,"PS5","MUM",-1).timestamp(Instant.now()).build());
        long after=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        long usedMB=(after-before)/1024/1024;
        assertTrue(usedMB<100, "Memory growth should be <100MB for 10K events, was "+usedMB+"MB");
    }
}
