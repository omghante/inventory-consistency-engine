package com.cascade.demo;

import com.cascade.engine.CASCADEEngine;
import com.cascade.metrics.MetricsDashboard;
import com.cascade.delta.CausalDelta;
import com.cascade.delta.Preconditions;
import com.cascade.delta.SourceType;
import com.cascade.queue.EventQueue;
import com.cascade.worker.WorkerCluster;
import com.cascade.recovery.EventStore;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Full System Simulation — End-to-end distributed inventory pipeline.
 */
public class FullSystemSimulation {
    private static final String CYAN="\u001B[36m",GREEN="\u001B[32m",RED="\u001B[31m",BOLD="\u001B[1m",RESET="\u001B[0m";

    public static void main(String[] args) throws Exception {
        System.out.println(CYAN+BOLD+"\n  CASCADE — Full System Simulation\n  Queue → Workers → CASCADE Engine → Event Store\n"+RESET);
        printPhase("SETUP","Initializing distributed system components");
        CASCADEEngine engine=new CASCADEEngine();
        EventQueue queue=new EventQueue(50_000,3,java.time.Duration.ofMillis(50));
        EventStore store=new EventStore();
        WorkerCluster cluster=new WorkerCluster(queue,engine,store,4);
        MetricsDashboard dashboard=new MetricsDashboard(engine,queue,cluster,store);
        engine.registerProduct("PS5",1000); engine.registerProduct("IPHONE16",2000); engine.registerProduct("MACBOOK",500);
        System.out.println("  All components initialized");

        // Phase 1: Normal traffic
        printPhase("PHASE 1","Normal operations — 4 workers, 500 events");
        cluster.startAll();
        String[] products={"PS5","IPHONE16","MACBOOK"};
        String[] warehouses={"MUM-01","BLR-01","DEL-01","HYD-01","CHN-01"};
        for(int i=0;i<500;i++) queue.enqueue(CausalDelta.builder("normal_"+i,products[i%3],warehouses[i%5],-1)
                .timestamp(Instant.now()).causalContext(Map.of(warehouses[i%5],(long)(i+1)))
                .source(SourceType.POS_AUTOMATED).sourceReliability(0.985).build());
        Thread.sleep(1000);
        System.out.printf("  PS5: %d | IPHONE16: %d | MACBOOK: %d%n",engine.getStock("PS5"),engine.getStock("IPHONE16"),engine.getStock("MACBOOK"));

        // Phase 2: Flash sale
        printPhase("PHASE 2","Flash sale — 5000 orders, scaling to 8 workers");
        for(int i=0;i<4;i++) System.out.println("  "+cluster.scaleUp());
        for(int i=0;i<5000;i++) queue.enqueue(CausalDelta.builder("flash_"+i,"PS5",
                warehouses[ThreadLocalRandom.current().nextInt(5)],-1).timestamp(Instant.now())
                .source(SourceType.POS_AUTOMATED).sourceReliability(0.985).precondition(new Preconditions(1)).build());
        Thread.sleep(2000);
        System.out.println("  PS5 stock: "+engine.getStock("PS5"));
        System.out.println(engine.getStock("PS5")>=0?GREEN+"  NO OVERSELLING"+RESET:RED+"  OVERSOLD"+RESET);

        // Phase 3: Duplicate storm
        printPhase("PHASE 3","Duplicate storm — 100 unique × 5 retries");
        for(int i=0;i<100;i++) for(int r=0;r<5;r++) queue.enqueue(CausalDelta.builder("dup_"+i,"IPHONE16","MUM-01",-1)
                .timestamp(Instant.now()).source(SourceType.POS_AUTOMATED).sourceReliability(0.985).build());
        Thread.sleep(1000);
        System.out.printf("  Duplicates blocked: %d | IPHONE16: %d%n",engine.getDuplicatesBlocked(),engine.getStock("IPHONE16"));

        // Phase 4: Recovery
        printPhase("PHASE 4","State reconstruction from event log");
        System.out.printf("  PS5:      %d (reconstructed: %d)%n",engine.getStock("PS5"),store.reconstructState("PS5",1000));
        System.out.printf("  IPHONE16: %d (reconstructed: %d)%n",engine.getStock("IPHONE16"),store.reconstructState("IPHONE16",2000));
        System.out.printf("  MACBOOK:  %d (reconstructed: %d)%n",engine.getStock("MACBOOK"),store.reconstructState("MACBOOK",500));

        // Phase 5: Dashboard
        printPhase("PHASE 5","System Dashboard");
        System.out.println(dashboard.render());
        System.out.println(cluster.getStats());

        cluster.shutdownAll();
        System.out.println("  Simulation complete");
    }

    static void printPhase(String l,String d){System.out.println("\n"+BOLD+CYAN+"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"+RESET);
        System.out.println(BOLD+"  "+l+": "+d+RESET);System.out.println(CYAN+"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"+RESET);}
}
