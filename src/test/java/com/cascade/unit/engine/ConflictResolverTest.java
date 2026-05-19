package com.cascade.unit.engine;

import com.cascade.engine.*;
import com.cascade.delta.*;
import com.cascade.scoring.TrustScorer;
import com.cascade.state.ProductState;
import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConflictResolver Tests")
class ConflictResolverTest {
    private ConflictResolver resolver;
    private ProductState product;

    @BeforeEach void setUp() {
        resolver = new ConflictResolver(new TrustScorer(), new DegradationManager());
        product = new ProductState("PS5", 100);
        // Set initial VC state
        product.applyDelta(CausalDelta.builder("setup","PS5","MUM",-1).timestamp(Instant.now())
                .causalContext(Map.of("MUM",5L)).build(), MergeResult.ResolutionMode.CAUSAL);
    }

    @Test @DisplayName("High-trust concurrent event is accepted")
    void highTrustAccepted() {
        MergeResult r = resolver.resolve(CausalDelta.builder("e1","PS5","BLR",-3).timestamp(Instant.now())
                .causalContext(Map.of("BLR",5L)).source(SourceType.POS_AUTOMATED).sourceReliability(0.99).build(), product);
        assertTrue(r.isApplied());
        assertEquals(MergeResult.ResolutionMode.TRUST_SCORED, r.getResolutionMode());
    }

    @Test @DisplayName("Low-trust concurrent event is rejected")
    void lowTrustRejected() {
        MergeResult r = resolver.resolve(CausalDelta.builder("e1","PS5","BLR",-3)
                .timestamp(Instant.parse("2026-05-18T06:00:00Z"))
                .causalContext(Map.of("BLR",5L)).source(SourceType.MANUAL_ENTRY).sourceReliability(0.1).build(), product);
        assertEquals(MergeResult.Action.LOW_TRUST_REJECTED, r.getAction());
    }

    @Test @DisplayName("No trust metadata falls back to timestamp")
    void noTrustFallback() {
        MergeResult r = resolver.resolve(CausalDelta.builder("e1","PS5","BLR",-3).timestamp(Instant.now())
                .causalContext(Map.of("BLR",5L)).build(), product);
        assertTrue(r.isApplied());
    }
}
