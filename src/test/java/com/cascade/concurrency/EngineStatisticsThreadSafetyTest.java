package com.cascade.concurrency;

import com.cascade.engine.CASCADEEngine;
import com.cascade.engine.MergeResult;
import com.cascade.delta.CausalDelta;
import com.cascade.delta.Preconditions;

import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates that engine-level statistics (totalMerges, appliedCount, etc.)
 * are accurate under concurrent access. These counters were previously bare
 * {@code long} fields — a data race that could produce incorrect counts
 * under contention.
 *
 * <p>After migration to AtomicLong, these tests verify that no increments
 * are lost even under heavy multi-threaded load.</p>
 */
@DisplayName("Engine Statistics Thread Safety")
class EngineStatisticsThreadSafetyTest {

    @Test
    @DisplayName("Statistics counters are accurate under 10K concurrent merges")
    void statisticsAccurateUnderConcurrency() throws Exception {
        CASCADEEngine engine = new CASCADEEngine();
        engine.registerProduct("PS5", 1_000_000);

        int totalEvents = 10_000;
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(totalEvents);
        AtomicInteger counter = new AtomicInteger();

        for (int i = 0; i < totalEvents; i++) {
            pool.submit(() -> {
                int id = counter.incrementAndGet();
                engine.merge(CausalDelta.builder("e" + id, "PS5", "W-" + (id % 5), -1)
                        .timestamp(Instant.now()).build());
                latch.countDown();
            });
        }

        latch.await();
        pool.shutdown();

        // Every merge must be counted — no lost increments
        assertEquals(totalEvents, engine.getTotalMerges(),
                "totalMerges should equal total submitted events");
        assertEquals(totalEvents, engine.getAppliedCount(),
                "all events should be applied (unique IDs, sufficient stock)");
        assertEquals(0, engine.getRejectedCount(),
                "no events should be rejected");
        assertEquals(0, engine.getDuplicatesBlocked(),
                "no duplicates submitted");
    }

    @Test
    @DisplayName("Mixed accept/reject statistics under concurrent load")
    void mixedStatisticsUnderConcurrency() throws Exception {
        CASCADEEngine engine = new CASCADEEngine();
        engine.registerProduct("PS5", 200);

        int totalEvents = 1_000;
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(totalEvents);
        AtomicInteger counter = new AtomicInteger();

        for (int i = 0; i < totalEvents; i++) {
            pool.submit(() -> {
                int id = counter.incrementAndGet();
                engine.merge(CausalDelta.builder("o" + id, "PS5", "W-" + (id % 3), -1)
                        .timestamp(Instant.now())
                        .precondition(new Preconditions(1))
                        .build());
                latch.countDown();
            });
        }

        latch.await();
        pool.shutdown();

        // Total merges must match total submissions
        assertEquals(totalEvents, engine.getTotalMerges(),
                "totalMerges must equal total submitted events");

        // Applied + rejected must equal total (no events lost)
        assertEquals(totalEvents,
                engine.getAppliedCount() + engine.getRejectedCount(),
                "applied + rejected must equal totalMerges");

        // Exactly 200 should be applied (stock started at 200, -1 each)
        assertEquals(200, engine.getAppliedCount(),
                "exactly 200 orders should be fulfilled");
        assertEquals(800, engine.getOversellsPrevented(),
                "exactly 800 oversell attempts should be prevented");
    }

    @Test
    @DisplayName("Duplicate statistics under concurrent delivery of same eventIds")
    void duplicateStatisticsUnderConcurrency() throws Exception {
        CASCADEEngine engine = new CASCADEEngine();
        engine.registerProduct("PS5", 1_000_000);

        int uniqueEvents = 100;
        int retries = 10;
        int totalSubmissions = uniqueEvents * retries;
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(totalSubmissions);

        for (int i = 0; i < uniqueEvents; i++) {
            for (int r = 0; r < retries; r++) {
                final int eventNum = i;
                pool.submit(() -> {
                    engine.merge(CausalDelta.builder("e" + eventNum, "PS5",
                            "W-" + (eventNum % 5), -1)
                            .timestamp(Instant.now()).build());
                    latch.countDown();
                });
            }
        }

        latch.await();
        pool.shutdown();

        assertEquals(totalSubmissions, engine.getTotalMerges(),
                "totalMerges must count every submission");
        assertEquals(uniqueEvents, engine.getAppliedCount(),
                "only unique events should be applied");
        assertEquals(totalSubmissions - uniqueEvents, engine.getDuplicatesBlocked(),
                "all duplicates must be counted");
    }
}
