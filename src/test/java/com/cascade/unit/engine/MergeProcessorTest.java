package com.cascade.unit.engine;

import com.cascade.engine.MergeProcessor;
import com.cascade.engine.ConflictResolver;
import com.cascade.engine.DegradationManager;
import com.cascade.engine.MergeResult;
import com.cascade.delta.CausalDelta;
import com.cascade.delta.SourceType;
import com.cascade.scoring.TrustScorer;
import com.cascade.state.ProductState;

import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MergeProcessor Tests")
class MergeProcessorTest {

    private MergeProcessor processor;
    private ProductState product;

    @BeforeEach
    void setUp() {
        TrustScorer scorer = new TrustScorer();
        DegradationManager degradation = new DegradationManager();
        ConflictResolver resolver = new ConflictResolver(scorer, degradation);
        processor = new MergeProcessor(resolver, degradation);
        product = new ProductState("PS5", 100);
    }

    @Test @DisplayName("Fresh event is applied")
    void freshEvent() {
        MergeResult r = processor.process(
                CausalDelta.builder("e1", "PS5", "MUM", -5).timestamp(Instant.now()).build(), product);
        assertTrue(r.isApplied());
        assertEquals(95, product.getQuantity());
    }

    @Test @DisplayName("Duplicate is rejected at step 1")
    void duplicateAtStep1() {
        processor.process(CausalDelta.builder("e1", "PS5", "MUM", -5).timestamp(Instant.now()).build(), product);
        MergeResult r = processor.process(
                CausalDelta.builder("e1", "PS5", "MUM", -5).timestamp(Instant.now()).build(), product);
        assertEquals(MergeResult.Action.DUPLICATE_REJECTED, r.getAction());
    }

    @Test @DisplayName("Precondition failure at step 2")
    void preconditionFailure() {
        MergeResult r = processor.process(
                CausalDelta.builder("e1", "PS5", "MUM", -200).timestamp(Instant.now())
                        .precondition(new com.cascade.delta.Preconditions(200)).build(), product);
        assertEquals(MergeResult.Action.CONDITION_FAILED, r.getAction());
    }

    @Test @DisplayName("Stale vector clock rejected at step 3")
    void staleRejected() {
        processor.process(CausalDelta.builder("e1", "PS5", "MUM", -5).timestamp(Instant.now())
                .causalContext(Map.of("MUM", 10L)).build(), product);
        MergeResult r = processor.process(
                CausalDelta.builder("e2", "PS5", "MUM", -3).timestamp(Instant.now())
                        .causalContext(Map.of("MUM", 3L)).source(SourceType.POS_AUTOMATED).sourceReliability(0.9).build(), product);
        assertEquals(MergeResult.Action.STALE_REJECTED, r.getAction());
    }

    @Test @DisplayName("Resolution mode is DIRECT for bare delta")
    void directMode() {
        MergeResult r = processor.process(
                CausalDelta.builder("e1", "PS5", "MUM", -1).timestamp(Instant.now()).build(), product);
        assertEquals(MergeResult.ResolutionMode.DIRECT, r.getResolutionMode());
    }

    @Test @DisplayName("Resolution mode is CAUSAL for delta with vector clock")
    void causalMode() {
        MergeResult r = processor.process(
                CausalDelta.builder("e1", "PS5", "MUM", -1).timestamp(Instant.now())
                        .causalContext(Map.of("MUM", 1L)).build(), product);
        assertEquals(MergeResult.ResolutionMode.CAUSAL, r.getResolutionMode());
    }
}
