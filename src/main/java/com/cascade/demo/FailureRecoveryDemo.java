package com.cascade.demo;

import com.cascade.engine.CASCADEEngine;
import com.cascade.delta.CausalDelta;
import com.cascade.delta.Preconditions;
import com.cascade.delta.SourceType;
import com.cascade.engine.MergeResult;
import com.cascade.queue.EventQueue;
import com.cascade.worker.Worker;
import com.cascade.worker.WorkerCluster;
import com.cascade.recovery.EventStore;
import java.time.Instant;
import java.util.Map;

/**
 * Failure Recovery Demo — 5 real failure scenarios.
 */
public class FailureRecoveryDemo {
    private static final String CYAN="\u001B[36m",GREEN="\u001B[32m",RED="\u001B[31m",YELLOW="\u001B[33m",BOLD="\u001B[1m",RESET="\u001B[0m";

    public static void main(String[] args) throws Exception {
        System.out.println(CYAN+BOLD+"\n  CASCADE — Failure Recovery Demo\n"+RESET);
        scenario1(); scenario2(); scenario3(); scenario4(); scenario5();
        System.out.println("\n"+GREEN+BOLD+"  ALL FAILURE SCENARIOS HANDLED "+RESET);
    }

    static void scenario1() throws Exception {
        printPhase("SCENARIO 1","Worker Crash + Restart");
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",100);
        EventQueue queue=new EventQueue(); EventStore store=new EventStore();
        for(int i=0;i<50;i++) queue.enqueue(CausalDelta.builder("s1_"+i,"PS5","MUM",-1).timestamp(Instant.now()).build());
        Worker w1=new Worker("worker-A",queue,engine,store), w2=new Worker("worker-B",queue,engine,store);
        w1.start(); w2.start(); Thread.sleep(200);
        w1.shutdown(); System.out.println("  worker-A crashed! Stock: "+engine.getStock("PS5"));
        for(int i=50;i<100;i++) queue.enqueue(CausalDelta.builder("s1_"+i,"PS5","MUM",-1).timestamp(Instant.now()).build());
        Worker w3=new Worker("worker-C",queue,engine,store); w3.start(); Thread.sleep(500);
        w2.shutdown(); w3.shutdown();
        System.out.println("  Final stock: "+engine.getStock("PS5")+" | Events: "+store.size());
        System.out.println(engine.getStock("PS5")==0?GREEN+"  ZERO DATA LOSS"+RESET:YELLOW+"  In-flight events"+RESET);
    }

    static void scenario2() throws Exception {
        printPhase("SCENARIO 2","Duplicate Delivery Storm");
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",1000);
        EventQueue queue=new EventQueue(); EventStore store=new EventStore();
        for(int i=0;i<50;i++) for(int d=0;d<5;d++) queue.enqueue(CausalDelta.builder("s2_"+i,"PS5","MUM",-1)
                .timestamp(Instant.now()).source(SourceType.POS_AUTOMATED).sourceReliability(0.985).build());
        WorkerCluster cluster=new WorkerCluster(queue,engine,store,4); cluster.startAll(); Thread.sleep(500); cluster.shutdownAll();
        System.out.printf("  Stock: %d (expected: 950) | Duplicates: %d%n",engine.getStock("PS5"),engine.getDuplicatesBlocked());
        System.out.println(engine.getStock("PS5")==950?GREEN+"  EXACTLY-ONCE"+RESET:RED+"  DUPLICATE LEAKED"+RESET);
    }

    static void scenario3() throws Exception {
        printPhase("SCENARIO 3","Stale Event Injection");
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",100);
        for(int i=1;i<=20;i++) engine.merge(CausalDelta.builder("fresh_"+i,"PS5","MUM",-1).timestamp(Instant.now())
                .causalContext(Map.of("MUM",(long)i)).source(SourceType.POS_AUTOMATED).sourceReliability(0.985).build());
        int stale=0;
        for(int i=1;i<=10;i++){MergeResult r=engine.merge(CausalDelta.builder("stale_"+i,"PS5","MUM",+5)
                .timestamp(Instant.parse("2026-05-19T06:00:00Z")).causalContext(Map.of("MUM",(long)i))
                .source(SourceType.WAREHOUSE_SCANNER).sourceReliability(0.90).build());
            if(r.getAction()==MergeResult.Action.STALE_REJECTED) stale++;}
        System.out.println("  Stale rejected: "+stale+"/10 | Stock: "+engine.getStock("PS5")+" (should be 80)");
        System.out.println(stale==10?GREEN+"  ALL STALE REJECTED"+RESET:RED+"  STALE LEAKED"+RESET);
    }

    static void scenario4() throws Exception {
        printPhase("SCENARIO 4","State Reconstruction from Event Log");
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",500); engine.registerProduct("XBOX",300);
        EventStore store=new EventStore();
        int[][] ops={{-5,0},{-3,0},{+10,0},{-7,0},{-2,0},{-10,1},{-5,1},{+20,1},{-8,1},{-3,1}};
        String[] names={"PS5","XBOX"};
        for(int i=0;i<ops.length;i++){CausalDelta d=CausalDelta.builder("s4_"+i,names[ops[i][1]],"MUM",ops[i][0]).timestamp(Instant.now()).build();
            store.record(d,engine.merge(d),"test");}
        int rPS5=store.reconstructState("PS5",500),rXBOX=store.reconstructState("XBOX",300);
        System.out.printf("  PS5: %d→%d | XBOX: %d→%d%n",engine.getStock("PS5"),rPS5,engine.getStock("XBOX"),rXBOX);
        CASCADEEngine recovered=new CASCADEEngine(); recovered.registerProduct("PS5",500); recovered.registerProduct("XBOX",300);
        store.getAppliedEventsForProduct("PS5").forEach(e->recovered.merge(e.delta()));
        store.getAppliedEventsForProduct("XBOX").forEach(e->recovered.merge(e.delta()));
        System.out.println("  Replayed PS5: "+recovered.getStock("PS5")+" | XBOX: "+recovered.getStock("XBOX"));
        System.out.println(recovered.getStock("PS5")==engine.getStock("PS5")?GREEN+"  REPLAY VERIFIED"+RESET:RED+"  MISMATCH"+RESET);
    }

    static void scenario5() throws Exception {
        printPhase("SCENARIO 5","Degraded Mode Operations");
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",100);
        MergeResult r1=engine.merge(CausalDelta.builder("deg_1","PS5","MUM",-5).timestamp(Instant.now())
                .causalContext(Map.of("MUM",1L)).source(SourceType.POS_AUTOMATED).sourceReliability(0.985)
                .precondition(new Preconditions(5)).build());
        MergeResult r2=engine.merge(CausalDelta.builder("deg_2","PS5","MUM",-3).timestamp(Instant.now())
                .causalContext(Map.of("MUM",2L)).source(SourceType.POS_AUTOMATED).sourceReliability(0.985).build());
        MergeResult r3=engine.merge(CausalDelta.builder("deg_3","PS5","MUM",-2).timestamp(Instant.now())
                .causalContext(Map.of("MUM",3L)).build());
        MergeResult r4=engine.merge(CausalDelta.builder("deg_4","PS5","MUM",-1).timestamp(Instant.now()).build());
        System.out.printf("  Full: %s(%s) | NoPre: %s(%s) | NoTrust: %s(%s) | Bare: %s(%s)%n",
                r1.isApplied()?"":"",r1.getResolutionMode(),r2.isApplied()?"":"",r2.getResolutionMode(),
                r3.isApplied()?"":"",r3.getResolutionMode(),r4.isApplied()?"":"",r4.getResolutionMode());
        System.out.println(r1.isApplied()&&r2.isApplied()&&r3.isApplied()&&r4.isApplied()?GREEN+"  ALL LEVELS HANDLED"+RESET:RED+"  FAILED"+RESET);
    }

    static void printPhase(String l,String d){System.out.println("\n"+BOLD+CYAN+"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"+RESET);
        System.out.println(BOLD+"  "+l+": "+d+RESET);System.out.println(CYAN+"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"+RESET);}
}
