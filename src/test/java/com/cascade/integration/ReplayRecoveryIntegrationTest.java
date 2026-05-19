package com.cascade.integration;

import com.cascade.engine.CASCADEEngine; import com.cascade.delta.*;
import com.cascade.recovery.*; import com.cascade.state.ProductState;
import org.junit.jupiter.api.*; import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Replay Recovery Integration")
class ReplayRecoveryIntegrationTest {
    @Test void snapshotThenReplay() {
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",500);
        EventStore store=new EventStore(); SnapshotManager sm=new SnapshotManager();
        for(int i=0;i<20;i++){CausalDelta d=CausalDelta.builder("e"+i,"PS5","MUM",-1).timestamp(Instant.now()).build();
            store.record(d,engine.merge(d),"w1");}
        sm.takeSnapshot(engine.getProductState("PS5"), store.getLatestSequence());
        for(int i=20;i<30;i++){CausalDelta d=CausalDelta.builder("e"+i,"PS5","MUM",-1).timestamp(Instant.now()).build();
            store.record(d,engine.merge(d),"w1");}
        CASCADEEngine recovered=new CASCADEEngine();
        RecoveryCoordinator rc=new RecoveryCoordinator(sm,new ReplayEngine(store));
        var result=rc.recover(recovered,"PS5",500);
        assertTrue(result.fromSnapshot()); assertEquals(engine.getStock("PS5"), result.finalStock());
    }
}
