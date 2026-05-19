package com.cascade.performance;

import com.cascade.engine.CASCADEEngine; import com.cascade.delta.*;
import org.junit.jupiter.api.*; import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Latency Benchmark")
class LatencyBenchmarkTest {
    @Test @DisplayName("Single merge completes in <1ms")
    void singleMergeLatency() {
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",100);
        // Warmup
        for(int i=0;i<100;i++) engine.merge(CausalDelta.builder("w"+i,"PS5","MUM",-0).timestamp(Instant.now()).build());
        // Measure
        Instant start=Instant.now();
        engine.merge(CausalDelta.builder("measure","PS5","MUM",-1).timestamp(Instant.now()).build());
        long nanos=Duration.between(start,Instant.now()).toNanos();
        assertTrue(nanos<1_000_000, "Single merge should be <1ms, was "+nanos+"ns");
    }
}
