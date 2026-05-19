package com.cascade.unit.scoring;

import com.cascade.scoring.TrustScorer;
import com.cascade.delta.*;
import org.junit.jupiter.api.*;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TrustScorer Tests")
class TrustScorerTest {
    private TrustScorer scorer;
    @BeforeEach void setUp() { scorer = new TrustScorer(); }

    @Test void highTrustPOS() {
        Double score = scorer.computeScore(CausalDelta.builder("e1","PS5","MUM",-1).timestamp(Instant.now())
                .source(SourceType.POS_AUTOMATED).sourceReliability(0.99).build());
        assertNotNull(score); assertTrue(score > 0.8); }

    @Test void lowTrustManual() {
        Double score = scorer.computeScore(CausalDelta.builder("e1","PS5","MUM",-1)
                .timestamp(Instant.parse("2026-05-18T06:00:00Z")).source(SourceType.MANUAL_ENTRY).sourceReliability(0.1).build());
        assertNotNull(score); assertTrue(score < 0.5); }

    @Test void noMetadataReturnsNull() {
        assertNull(scorer.computeScore(CausalDelta.builder("e1","PS5","MUM",-1).timestamp(Instant.now()).build())); }

    @Test void freshnessDecay() {
        double fresh = scorer.computeFreshness(Instant.now());
        double old = scorer.computeFreshness(Instant.parse("2026-05-18T06:00:00Z"));
        assertTrue(fresh > old); }
}
