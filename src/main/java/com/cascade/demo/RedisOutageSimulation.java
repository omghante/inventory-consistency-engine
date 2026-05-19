package com.cascade.demo;

import com.cascade.engine.CASCADEEngine;
import com.cascade.engine.MergeResult;
import com.cascade.delta.CausalDelta;
import com.cascade.delta.SourceType;
import java.time.Instant;
import java.util.Map;

/**
 * RedisOutageSimulation — Shows CASCADE degradation when metadata sources go down.
 */
public class RedisOutageSimulation {
    public static void main(String[] args) {
        System.out.println("\n  CASCADE — Redis Outage Simulation\n");
        CASCADEEngine engine = new CASCADEEngine();
        engine.registerProduct("PS5", 100);

        // Phase 1: Full metadata (Redis up)
        System.out.println("  Phase 1: Redis UP — Full metadata");
        for (int i = 1; i <= 5; i++) {
            MergeResult r = engine.merge(CausalDelta.builder("pre_" + i, "PS5", "MUM", -1)
                    .timestamp(Instant.now()).causalContext(Map.of("MUM", (long) i))
                    .source(SourceType.POS_AUTOMATED).sourceReliability(0.985).build());
            System.out.printf("    [%s] mode=%s stock=%d%n", r.getAction(), r.getResolutionMode(), engine.getStock("PS5"));
        }

        // Phase 2: Redis goes down — no causal context, no trust data
        System.out.println("\n  Phase 2: Redis DOWN — Bare deltas only");
        for (int i = 6; i <= 10; i++) {
            MergeResult r = engine.merge(CausalDelta.builder("outage_" + i, "PS5", "MUM", -1)
                    .timestamp(Instant.now()).build());
            System.out.printf("    [%s] mode=%s stock=%d%n", r.getAction(), r.getResolutionMode(), engine.getStock("PS5"));
        }

        // Phase 3: Redis recovers
        System.out.println("\n  Phase 3: Redis RECOVERED — Full metadata restored");
        for (int i = 11; i <= 15; i++) {
            MergeResult r = engine.merge(CausalDelta.builder("post_" + i, "PS5", "MUM", -1)
                    .timestamp(Instant.now()).causalContext(Map.of("MUM", (long) i))
                    .source(SourceType.POS_AUTOMATED).sourceReliability(0.985).build());
            System.out.printf("    [%s] mode=%s stock=%d%n", r.getAction(), r.getResolutionMode(), engine.getStock("PS5"));
        }
        System.out.println("\n  System NEVER crashed during Redis outage");
    }
}
