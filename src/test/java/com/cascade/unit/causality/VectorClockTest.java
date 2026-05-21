package com.cascade.unit.causality;

import com.cascade.causality.VectorClock;
import com.cascade.causality.CausalRelation;
import org.junit.jupiter.api.*;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VectorClock Tests")
class VectorClockTest {
    @Test void emptyEqual() { assertEquals(CausalRelation.EQUAL, new VectorClock().compare(new VectorClock())); }
    @Test void identicalEqual() {
        assertEquals(CausalRelation.EQUAL, new VectorClock(Map.of("MUM",5L,"BLR",3L)).compare(new VectorClock(Map.of("MUM",5L,"BLR",3L)))); }
    @Test void strictlyAfter() {
        assertEquals(CausalRelation.AFTER, new VectorClock(Map.of("MUM",5L,"BLR",4L)).compare(new VectorClock(Map.of("MUM",3L,"BLR",2L)))); }
    @Test void strictlyBefore() {
        assertEquals(CausalRelation.BEFORE, new VectorClock(Map.of("MUM",2L,"BLR",1L)).compare(new VectorClock(Map.of("MUM",5L,"BLR",3L)))); }
    @Test void concurrent() {
        assertEquals(CausalRelation.CONCURRENT, new VectorClock(Map.of("MUM",5L,"BLR",2L)).compare(new VectorClock(Map.of("MUM",3L,"BLR",4L)))); }
    @Test void extraNodeAfter() { assertEquals(CausalRelation.AFTER, new VectorClock(Map.of("MUM",1L)).compare(new VectorClock())); }
    @Test void emptyBefore() { assertEquals(CausalRelation.BEFORE, new VectorClock().compare(new VectorClock(Map.of("MUM",1L)))); }
    @Test void differentNodesConcurrent() {
        assertEquals(CausalRelation.CONCURRENT, new VectorClock(Map.of("MUM",3L)).compare(new VectorClock(Map.of("BLR",3L)))); }
    @Test void increment() {
        VectorClock vc = new VectorClock(); vc.increment("MUM").increment("MUM").increment("BLR");
        assertEquals(Map.of("MUM",2L,"BLR",1L), vc.getClockState()); }
    @Test void merge() {
        VectorClock merged = new VectorClock(Map.of("MUM",5L,"BLR",2L)).merge(new VectorClock(Map.of("MUM",3L,"BLR",4L,"DEL",1L)));
        assertEquals(Map.of("MUM",5L,"BLR",4L,"DEL",1L), merged.getClockState()); }
    @Test void mergeCommutative() {
        var a=new VectorClock(Map.of("MUM",5L,"BLR",2L)); var b=new VectorClock(Map.of("MUM",3L,"BLR",4L));
        assertEquals(a.merge(b).getClockState(), b.merge(a).getClockState()); }
    @Test void mergeIdempotent() {
        var a=new VectorClock(Map.of("MUM",5L,"BLR",2L)); assertEquals(a.getClockState(), a.merge(a).getClockState()); }
    @Test void mergeAssociative() {
        var a=new VectorClock(Map.of("MUM",5L)); var b=new VectorClock(Map.of("BLR",3L)); var c=new VectorClock(Map.of("MUM",2L,"DEL",7L));
        assertEquals(a.merge(b).merge(c).getClockState(), a.merge(b.merge(c)).getClockState()); }
    @Test void cloneIndependent() {
        VectorClock o=new VectorClock(Map.of("MUM",5L)); VectorClock cl=o.copy(); cl.increment("MUM");
        assertEquals(5L,o.getClockState().get("MUM")); assertEquals(6L,cl.getClockState().get("MUM")); }
    @Test void threeWarehouseScenario() {
        var mum=new VectorClock(Map.of("MUM",5L,"BLR",3L,"DEL",1L));
        var blr=new VectorClock(Map.of("MUM",3L,"BLR",4L,"DEL",1L));
        var del=new VectorClock(Map.of("MUM",5L,"BLR",4L,"DEL",2L));
        assertEquals(CausalRelation.CONCURRENT, mum.compare(blr));
        assertEquals(CausalRelation.AFTER, del.compare(mum));
        assertEquals(CausalRelation.AFTER, del.compare(blr)); }

    @Nested @DisplayName("Bounded Clock Pruning")
    class BoundedClockTests {

        @Test @DisplayName("Bounded clock prunes to maxEntries on creation")
        void prunesOnCreation() {
            Map<String, Long> initial = new HashMap<>();
            initial.put("MUM", 10L);
            initial.put("BLR", 5L);
            initial.put("DEL", 20L);
            initial.put("HYD", 1L);
            VectorClock vc = VectorClock.bounded(initial, 2);
            assertEquals(2, vc.size());
            // Should retain the two highest: DEL(20) and MUM(10)
            assertTrue(vc.getClockState().containsKey("DEL"));
            assertTrue(vc.getClockState().containsKey("MUM"));
            assertFalse(vc.getClockState().containsKey("HYD"));
        }

        @Test @DisplayName("Bounded clock prunes after merge")
        void prunesAfterMerge() {
            VectorClock a = VectorClock.bounded(Map.of("MUM", 10L, "BLR", 5L), 2);
            VectorClock b = new VectorClock(Map.of("DEL", 20L, "HYD", 1L));
            VectorClock merged = a.merge(b);
            assertEquals(2, merged.size());
            // Should retain: DEL(20) and MUM(10)
            assertTrue(merged.getClockState().containsKey("DEL"));
            assertTrue(merged.getClockState().containsKey("MUM"));
        }

        @Test @DisplayName("Unbounded clock does not prune")
        void unboundedNoPrune() {
            VectorClock vc = new VectorClock(Map.of("A", 1L, "B", 2L, "C", 3L, "D", 4L));
            assertEquals(4, vc.size());
            assertEquals(VectorClock.UNBOUNDED, vc.getMaxEntries());
        }

        @Test @DisplayName("Bounded clock within limit is unchanged")
        void withinLimitUnchanged() {
            VectorClock vc = VectorClock.bounded(Map.of("MUM", 5L, "BLR", 3L), 5);
            assertEquals(2, vc.size());
            assertEquals(Map.of("MUM", 5L, "BLR", 3L), vc.getClockState());
        }

        @Test @DisplayName("Bounded empty clock is valid")
        void boundedEmpty() {
            VectorClock vc = VectorClock.bounded(3);
            assertEquals(0, vc.size());
            assertTrue(vc.isEmpty());
            assertEquals(3, vc.getMaxEntries());
        }

        @Test @DisplayName("Copy preserves bound")
        void copyPreservesBound() {
            VectorClock vc = VectorClock.bounded(Map.of("MUM", 10L), 3);
            VectorClock copied = vc.copy();
            assertEquals(3, copied.getMaxEntries());
            assertEquals(vc.getClockState(), copied.getClockState());
        }

        @Test @DisplayName("Invalid maxEntries throws")
        void invalidMaxEntries() {
            assertThrows(IllegalArgumentException.class, () -> VectorClock.bounded(0));
            assertThrows(IllegalArgumentException.class, () -> VectorClock.bounded(-5));
        }

        @Test @DisplayName("Size method returns node count")
        void sizeMethod() {
            assertEquals(0, new VectorClock().size());
            assertEquals(3, new VectorClock(Map.of("A", 1L, "B", 2L, "C", 3L)).size());
        }
    }
}

