package com.cascade.integration;

import com.cascade.engine.CASCADEEngine; import com.cascade.delta.*;
import org.junit.jupiter.api.*; import java.time.Instant; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Multi-Warehouse Consistency")
class MultiWarehouseConsistencyTest {
    @Test void fiveWarehousesConverge() {
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",1000);
        String[] whs={"MUM","BLR","DEL","HYD","CHN"};
        for(int r=1;r<=10;r++) for(String wh:whs)
            engine.merge(CausalDelta.builder("e_"+r+"_"+wh,"PS5",wh,-r).timestamp(Instant.now())
                    .causalContext(Map.of(wh,(long)r)).source(SourceType.POS_AUTOMATED).sourceReliability(0.985).build());
        assertTrue(engine.getStock("PS5") < 1000); assertTrue(engine.getAppliedCount() > 0);
    }
}
