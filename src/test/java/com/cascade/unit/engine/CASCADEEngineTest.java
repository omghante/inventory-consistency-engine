package com.cascade.unit.engine;

import com.cascade.engine.CASCADEEngine;
import com.cascade.engine.MergeResult;
import com.cascade.delta.CausalDelta;
import com.cascade.delta.Preconditions;
import com.cascade.delta.SourceType;

import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CASCADEEngine Tests")
class CASCADEEngineTest {

    private CASCADEEngine engine;

    @BeforeEach void setUp() { engine = new CASCADEEngine(); engine.registerProduct("PS5", 100); }

    @Nested @DisplayName("Idempotency")
    class IdempotencyTests {
        @Test @DisplayName("Duplicate eventId is rejected")
        void duplicateRejected() {
            engine.merge(CausalDelta.builder("e1","PS5","MUM",-5).timestamp(Instant.now()).build());
            MergeResult r = engine.merge(CausalDelta.builder("e1","PS5","MUM",-5).timestamp(Instant.now()).build());
            assertEquals(MergeResult.Action.DUPLICATE_REJECTED, r.getAction());
            assertEquals(95, engine.getStock("PS5"));
        }
        @Test @DisplayName("Different eventIds are independent")
        void differentIds() {
            engine.merge(CausalDelta.builder("e1","PS5","MUM",-5).timestamp(Instant.now()).build());
            MergeResult r = engine.merge(CausalDelta.builder("e2","PS5","MUM",-5).timestamp(Instant.now()).build());
            assertTrue(r.isApplied());
            assertEquals(90, engine.getStock("PS5"));
        }
        @Test @DisplayName("Duplicate with different delta still rejected")
        void duplicateDifferentDelta() {
            engine.merge(CausalDelta.builder("e1","PS5","MUM",-5).timestamp(Instant.now()).build());
            MergeResult r = engine.merge(CausalDelta.builder("e1","PS5","MUM",-10).timestamp(Instant.now()).build());
            assertEquals(MergeResult.Action.DUPLICATE_REJECTED, r.getAction());
        }
        @Test @DisplayName("10 retries — only first applies")
        void multipleRetries() {
            for (int i = 0; i < 10; i++) engine.merge(CausalDelta.builder("e1","PS5","MUM",-1).timestamp(Instant.now()).build());
            assertEquals(99, engine.getStock("PS5"));
            assertEquals(9, engine.getDuplicatesBlocked());
        }
        @Test @DisplayName("Same ID on different products are independent")
        void sameIdDifferentProducts() {
            engine.registerProduct("XBOX", 50);
            engine.merge(CausalDelta.builder("e1","PS5","MUM",-1).timestamp(Instant.now()).build());
            MergeResult r = engine.merge(CausalDelta.builder("e1","XBOX","MUM",-1).timestamp(Instant.now()).build());
            assertTrue(r.isApplied());
        }
    }

    @Nested @DisplayName("Preconditions")
    class PreconditionTests {
        @Test @DisplayName("Satisfied precondition allows merge")
        void satisfied() {
            MergeResult r = engine.merge(CausalDelta.builder("e1","PS5","MUM",-5).timestamp(Instant.now())
                    .precondition(new Preconditions(5)).build());
            assertTrue(r.isApplied());
        }
        @Test @DisplayName("Unsatisfied precondition rejects merge")
        void unsatisfied() {
            MergeResult r = engine.merge(CausalDelta.builder("e1","PS5","MUM",-200).timestamp(Instant.now())
                    .precondition(new Preconditions(200)).build());
            assertEquals(MergeResult.Action.CONDITION_FAILED, r.getAction());
        }
        @Test @DisplayName("Exact boundary condition")
        void exactBoundary() {
            MergeResult r = engine.merge(CausalDelta.builder("e1","PS5","MUM",-100).timestamp(Instant.now())
                    .precondition(new Preconditions(100)).build());
            assertTrue(r.isApplied());
            assertEquals(0, engine.getStock("PS5"));
        }
        @Test @DisplayName("Sequential orders drain stock correctly")
        void sequentialOrders() {
            for (int i = 0; i < 100; i++) engine.merge(CausalDelta.builder("o_"+i,"PS5","MUM",-1).timestamp(Instant.now())
                    .precondition(new Preconditions(1)).build());
            assertEquals(0, engine.getStock("PS5"));
            MergeResult r = engine.merge(CausalDelta.builder("o_100","PS5","MUM",-1).timestamp(Instant.now())
                    .precondition(new Preconditions(1)).build());
            assertEquals(MergeResult.Action.CONDITION_FAILED, r.getAction());
        }
        @Test @DisplayName("No precondition allows negative stock")
        void noPrecondition() {
            MergeResult r = engine.merge(CausalDelta.builder("e1","PS5","MUM",-200).timestamp(Instant.now()).build());
            assertTrue(r.isApplied());
            assertEquals(-100, engine.getStock("PS5"));
        }
    }

    @Nested @DisplayName("Causality")
    class CausalityTests {
        @Test @DisplayName("Newer vector clock is applied (AFTER)")
        void newerApplied() {
            engine.merge(CausalDelta.builder("e1","PS5","MUM",-5).timestamp(Instant.now()).causalContext(Map.of("MUM",1L)).build());
            MergeResult r = engine.merge(CausalDelta.builder("e2","PS5","MUM",-3).timestamp(Instant.now()).causalContext(Map.of("MUM",2L)).build());
            assertTrue(r.isApplied());
        }
        @Test @DisplayName("Older vector clock is rejected (BEFORE)")
        void olderRejected() {
            engine.merge(CausalDelta.builder("e1","PS5","MUM",-5).timestamp(Instant.now()).causalContext(Map.of("MUM",5L)).build());
            MergeResult r = engine.merge(CausalDelta.builder("e2","PS5","MUM",-3).timestamp(Instant.now()).causalContext(Map.of("MUM",2L))
                    .source(SourceType.POS_AUTOMATED).sourceReliability(0.9).build());
            assertEquals(MergeResult.Action.STALE_REJECTED, r.getAction());
        }
        @Test @DisplayName("Concurrent with high trust is applied")
        void concurrentHighTrust() {
            engine.merge(CausalDelta.builder("e1","PS5","MUM",-5).timestamp(Instant.now()).causalContext(Map.of("MUM",3L)).build());
            MergeResult r = engine.merge(CausalDelta.builder("e2","PS5","BLR",-3).timestamp(Instant.now()).causalContext(Map.of("BLR",3L))
                    .source(SourceType.POS_AUTOMATED).sourceReliability(0.99).build());
            assertTrue(r.isApplied());
        }
        @Test @DisplayName("Concurrent with low trust is rejected")
        void concurrentLowTrust() {
            engine.merge(CausalDelta.builder("e1","PS5","MUM",-5).timestamp(Instant.now()).causalContext(Map.of("MUM",3L)).build());
            MergeResult r = engine.merge(CausalDelta.builder("e2","PS5","BLR",-3).timestamp(Instant.parse("2026-05-18T06:00:00Z"))
                    .causalContext(Map.of("BLR",3L)).source(SourceType.MANUAL_ENTRY).sourceReliability(0.1).build());
            assertEquals(MergeResult.Action.LOW_TRUST_REJECTED, r.getAction());
        }
    }

    @Nested @DisplayName("Degradation")
    class DegradationTests {
        @Test @DisplayName("Bare delta applies in DIRECT mode")
        void bareDelta() {
            MergeResult r = engine.merge(CausalDelta.builder("e1","PS5","MUM",-1).timestamp(Instant.now()).build());
            assertTrue(r.isApplied());
            assertEquals(MergeResult.ResolutionMode.DIRECT, r.getResolutionMode());
        }
        @Test @DisplayName("Full metadata uses CAUSAL mode")
        void fullMetadata() {
            MergeResult r = engine.merge(CausalDelta.builder("e1","PS5","MUM",-1).timestamp(Instant.now())
                    .causalContext(Map.of("MUM",1L)).source(SourceType.POS_AUTOMATED).sourceReliability(0.985).build());
            assertTrue(r.isApplied());
            assertEquals(MergeResult.ResolutionMode.CAUSAL, r.getResolutionMode());
        }
        @Test @DisplayName("Sequential full then bare works")
        void fullThenBare() {
            engine.merge(CausalDelta.builder("e1","PS5","MUM",-5).timestamp(Instant.now())
                    .causalContext(Map.of("MUM",1L)).source(SourceType.POS_AUTOMATED).sourceReliability(0.985).build());
            MergeResult r = engine.merge(CausalDelta.builder("e2","PS5","MUM",-3).timestamp(Instant.now()).build());
            assertTrue(r.isApplied());
            assertEquals(92, engine.getStock("PS5"));
        }
    }

    @Nested @DisplayName("Edge Cases")
    class EdgeCaseTests {
        @Test @DisplayName("Unknown product returns error")
        void unknownProduct() {
            MergeResult r = engine.merge(CausalDelta.builder("e1","UNKNOWN","MUM",-1).timestamp(Instant.now()).build());
            assertEquals(MergeResult.Action.UNKNOWN_PRODUCT, r.getAction());
        }
        @Test @DisplayName("Zero delta is valid")
        void zeroDelta() {
            MergeResult r = engine.merge(CausalDelta.builder("e1","PS5","MUM",0).timestamp(Instant.now()).build());
            assertTrue(r.isApplied());
            assertEquals(100, engine.getStock("PS5"));
        }
        @Test @DisplayName("Positive delta (restock) works")
        void positiveDelta() {
            MergeResult r = engine.merge(CausalDelta.builder("e1","PS5","MUM",+50).timestamp(Instant.now()).build());
            assertTrue(r.isApplied());
            assertEquals(150, engine.getStock("PS5"));
        }
        @Test @DisplayName("Null eventId throws")
        void nullEventId() { assertThrows(IllegalArgumentException.class, () ->
            CausalDelta.builder(null,"PS5","MUM",-1).build()); }
        @Test @DisplayName("Blank productId throws")
        void blankProductId() { assertThrows(IllegalArgumentException.class, () ->
            CausalDelta.builder("e1","","MUM",-1).build()); }
    }

    @Nested @DisplayName("Statistics")
    class StatsTests {
        @Test @DisplayName("Stats track all merge types")
        void statsTracking() {
            engine.merge(CausalDelta.builder("e1","PS5","MUM",-1).timestamp(Instant.now()).build());
            engine.merge(CausalDelta.builder("e1","PS5","MUM",-1).timestamp(Instant.now()).build()); // dup
            engine.merge(CausalDelta.builder("e2","PS5","MUM",-200).timestamp(Instant.now()).precondition(new Preconditions(200)).build()); // fail
            assertEquals(3, engine.getTotalMerges());
            assertEquals(1, engine.getAppliedCount());
            assertEquals(1, engine.getDuplicatesBlocked());
            assertEquals(1, engine.getOversellsPrevented());
        }
    }
}
