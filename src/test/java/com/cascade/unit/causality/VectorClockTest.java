package com.cascade.unit.causality;

import com.cascade.causality.VectorClock;
import com.cascade.causality.CausalRelation;
import org.junit.jupiter.api.*;
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
}
