package com.cascade.unit.recovery;

import com.cascade.engine.CASCADEEngine;
import com.cascade.delta.CausalDelta;
import com.cascade.recovery.*;
import org.junit.jupiter.api.*;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReplayEngine Tests")
class ReplayEngineTest {
    @Test void fullReplay() {
        CASCADEEngine engine = new CASCADEEngine(); engine.registerProduct("PS5",100);
        EventStore store = new EventStore();
        for(int i=0;i<10;i++){CausalDelta d=CausalDelta.builder("e"+i,"PS5","MUM",-1).timestamp(Instant.now()).build();
            store.record(d,engine.merge(d),"w1");}
        CASCADEEngine recovered = new CASCADEEngine();
        ReplayEngine re = new ReplayEngine(store);
        var result = re.replayAll(recovered,"PS5",100);
        assertEquals(10, result.applied()); assertEquals(90, result.finalStock()); assertEquals(engine.getStock("PS5"), result.finalStock()); }

    @Test void incrementalReplay() {
        CASCADEEngine engine = new CASCADEEngine(); engine.registerProduct("PS5",100);
        EventStore store = new EventStore();
        for(int i=0;i<5;i++){CausalDelta d=CausalDelta.builder("e"+i,"PS5","MUM",-1).timestamp(Instant.now()).build();
            store.record(d,engine.merge(d),"w1");}
        long checkpoint = store.getLatestSequence();
        for(int i=5;i<10;i++){CausalDelta d=CausalDelta.builder("e"+i,"PS5","MUM",-1).timestamp(Instant.now()).build();
            store.record(d,engine.merge(d),"w1");}
        // Replay the incremental events (after checkpoint) onto a FRESH engine (simulating snapshot recovery)
        CASCADEEngine recovered = new CASCADEEngine(); recovered.registerProduct("PS5",95); // snapshot: 100-5=95
        var result = new ReplayEngine(store).replayAfter(recovered, checkpoint);
        assertEquals(5, result.applied()); assertEquals(90, recovered.getStock("PS5")); }
}
