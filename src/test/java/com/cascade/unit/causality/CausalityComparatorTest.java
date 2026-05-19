package com.cascade.unit.causality;

import com.cascade.causality.*;
import org.junit.jupiter.api.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CausalityComparator Tests")
class CausalityComparatorTest {
    @Test void newerDetection() { assertTrue(CausalityComparator.isNewer(new VectorClock(Map.of("A",5L)),new VectorClock(Map.of("A",3L)))); }
    @Test void staleDetection() { assertTrue(CausalityComparator.isStale(new VectorClock(Map.of("A",2L)),new VectorClock(Map.of("A",5L)))); }
    @Test void concurrentDetection() { assertTrue(CausalityComparator.isConcurrent(new VectorClock(Map.of("A",5L)),new VectorClock(Map.of("B",5L)))); }
    @Test void notStaleWhenNewer() { assertFalse(CausalityComparator.isStale(new VectorClock(Map.of("A",5L)),new VectorClock(Map.of("A",3L)))); }
}
