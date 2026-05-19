package com.cascade.demo;

import com.cascade.engine.CASCADEEngine;
import com.cascade.engine.MergeResult;
import com.cascade.delta.CausalDelta;
import com.cascade.delta.Preconditions;
import com.cascade.delta.SourceType;

import java.time.Instant;
import java.util.Map;

/**
 * PS5 Flash Sale Demo — Demonstrates all 6 CASCADE behaviors.
 */
public class PS5FlashSaleDemo {

    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String BOLD = "\u001B[1m";
    private static final String RESET = "\u001B[0m";

    public static void main(String[] args) {
        printBanner();
        CASCADEEngine engine = new CASCADEEngine();
        engine.registerProduct("PS5", 100);

        // 1. Normal Operation
        printPhase("1", "Normal Operation — Direct delta merge");
        MergeResult r1 = engine.merge(
                CausalDelta.builder("evt_001", "PS5", "MUM-01", -5)
                        .timestamp(Instant.now())
                        .causalContext(Map.of("MUM-01", 1L))
                        .source(SourceType.POS_AUTOMATED)
                        .sourceReliability(0.985)
                        .build()
        );
        System.out.println("  " + r1);
        System.out.println("  Stock: " + engine.getStock("PS5"));

        // 2. Stale Detection
        printPhase("2", "Stale Event Rejection — Old vector clock");
        MergeResult r2 = engine.merge(
                CausalDelta.builder("evt_002", "PS5", "MUM-01", +10)
                        .timestamp(Instant.parse("2026-05-19T06:00:00Z"))
                        .causalContext(Map.of("MUM-01", 1L))
                        .source(SourceType.WAREHOUSE_SCANNER)
                        .sourceReliability(0.90)
                        .build()
        );
        System.out.println("  " + r2);

        // 3. Concurrent Conflict
        printPhase("3", "Concurrent Conflict Resolution — Trust scoring");
        MergeResult r3 = engine.merge(
                CausalDelta.builder("evt_003", "PS5", "BLR-01", -3)
                        .timestamp(Instant.now())
                        .causalContext(Map.of("BLR-01", 5L))
                        .source(SourceType.POS_AUTOMATED)
                        .sourceReliability(0.99)
                        .build()
        );
        System.out.println("  " + r3);
        System.out.println("  Stock: " + engine.getStock("PS5"));

        // 4. Oversell Prevention
        printPhase("4", "Oversell Prevention — Precondition check");
        MergeResult r4 = engine.merge(
                CausalDelta.builder("evt_004", "PS5", "MUM-01", -500)
                        .timestamp(Instant.now())
                        .precondition(new Preconditions(500))
                        .build()
        );
        System.out.println("  " + r4);
        System.out.println("  Stock: " + engine.getStock("PS5") + " (unchanged)");

        // 5. Idempotency
        printPhase("5", "Idempotency — Duplicate rejection");
        MergeResult r5 = engine.merge(
                CausalDelta.builder("evt_001", "PS5", "MUM-01", -5)
                        .timestamp(Instant.now()).build()
        );
        System.out.println("  " + r5);

        // 6. Graceful Degradation
        printPhase("6", "Graceful Degradation — Bare delta (no metadata)");
        MergeResult r6 = engine.merge(
                CausalDelta.builder("evt_005", "PS5", "DEL-01", -2)
                        .timestamp(Instant.now()).build()
        );
        System.out.println("  " + r6);
        System.out.println("  Stock: " + engine.getStock("PS5"));

        System.out.println("\n" + engine.getStats());
    }

    static void printBanner() {
        System.out.println(CYAN + BOLD + """
                ╔══════════════════════════════════════════════════════════╗
                ║   CASCADE — PS5 Flash Sale Demo                         ║
                ╚══════════════════════════════════════════════════════════╝
                """ + RESET);
    }
    static void printPhase(String num, String desc) {
        System.out.println("\n" + BOLD + CYAN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);
        System.out.println(BOLD + "  STEP " + num + ": " + desc + RESET);
        System.out.println(CYAN + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" + RESET);
    }
}
