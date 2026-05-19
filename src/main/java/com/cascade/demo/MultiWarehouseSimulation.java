package com.cascade.demo;

import com.cascade.engine.CASCADEEngine;
import com.cascade.delta.CausalDelta;
import com.cascade.delta.SourceType;
import java.time.Instant;
import java.util.Map;

/**
 * MultiWarehouseSimulation — Simulates 5 warehouses with concurrent stock updates.
 */
public class MultiWarehouseSimulation {
    public static void main(String[] args) {
        System.out.println("\n  CASCADE — Multi-Warehouse Simulation\n");
        CASCADEEngine engine = new CASCADEEngine();
        engine.registerProduct("PS5", 500);
        String[] warehouses = {"MUM-01", "BLR-01", "DEL-01", "HYD-01", "CHN-01"};

        for (int round = 1; round <= 5; round++) {
            System.out.printf("  Round %d:%n", round);
            for (String wh : warehouses) {
                engine.merge(CausalDelta.builder("mw_" + round + "_" + wh, "PS5", wh, -round)
                        .timestamp(Instant.now()).causalContext(Map.of(wh, (long) round))
                        .source(SourceType.WAREHOUSE_SCANNER).sourceReliability(0.92).build());
            }
            System.out.printf("    Stock: %d | Conflicts: %d%n", engine.getStock("PS5"), engine.getConflictsDetected());
        }
        System.out.println("\n  " + engine.getStats());
    }
}
