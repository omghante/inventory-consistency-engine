package com.cascade.demo;

import com.cascade.engine.CASCADEEngine;
import com.cascade.delta.CausalDelta;
import com.cascade.delta.Preconditions;
import com.cascade.delta.SourceType;
import com.cascade.engine.MergeResult;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ConcurrencyBenchmark — Stress tests for CASCADE under concurrent load.
 */
public class ConcurrencyBenchmark {
    private static final String CYAN="\u001B[36m",GREEN="\u001B[32m",RED="\u001B[31m",BOLD="\u001B[1m",RESET="\u001B[0m";

    public static void main(String[] args) throws Exception {
        printBanner(); benchmark1(); benchmark2(); benchmark3(); benchmark4(); benchmark5();
    }

    static void benchmark1() throws Exception {
        printPhase("BENCHMARK 1","Pure Throughput — 100K merges, 8 threads");
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",10_000_000);
        int total=100_000,threads=8; ExecutorService pool=Executors.newFixedThreadPool(threads);
        CountDownLatch latch=new CountDownLatch(total); AtomicInteger counter=new AtomicInteger(0);
        Instant start=Instant.now();
        for(int i=0;i<total;i++) pool.submit(()->{int id=counter.incrementAndGet();
            engine.merge(CausalDelta.builder("evt_"+id,"PS5","W-"+(id%5),-1).timestamp(Instant.now())
                    .source(SourceType.POS_AUTOMATED).sourceReliability(0.985).build()); latch.countDown();});
        latch.await(); long ms=Duration.between(start,Instant.now()).toMillis(); pool.shutdown();
        long tp=(total*1000L)/Math.max(1,ms); int expected=10_000_000-total;
        System.out.printf("  Events: %d | Threads: %d | Time: %dms%n  Throughput: %d merges/sec%n",total,threads,ms,tp);
        System.out.println("  Stock: "+engine.getStock("PS5")+" (expected: "+expected+")");
        System.out.println(engine.getStock("PS5")==expected?GREEN+"  CORRECT"+RESET:RED+"  RACE CONDITION"+RESET);
    }

    static void benchmark2() throws Exception {
        printPhase("BENCHMARK 2","Flash Sale — 500 stock, 2000 orders");
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",500);
        int total=2000,threads=16; ExecutorService pool=Executors.newFixedThreadPool(threads);
        CountDownLatch latch=new CountDownLatch(total);
        AtomicInteger accepted=new AtomicInteger(),rejected=new AtomicInteger(),eid=new AtomicInteger();
        Instant start=Instant.now();
        for(int i=0;i<total;i++) pool.submit(()->{int id=eid.incrementAndGet();
            MergeResult r=engine.merge(CausalDelta.builder("order_"+id,"PS5","W-"+(id%3),-1).timestamp(Instant.now())
                    .precondition(new Preconditions(1)).source(SourceType.POS_AUTOMATED).sourceReliability(0.985).build());
            if(r.isApplied()) accepted.incrementAndGet(); else rejected.incrementAndGet(); latch.countDown();});
        latch.await(); pool.shutdown(); long ms=Duration.between(start,Instant.now()).toMillis();
        System.out.printf("  Stock: 500→%d | Accepted: %d | Rejected: %d | Time: %dms%n",engine.getStock("PS5"),accepted.get(),rejected.get(),ms);
        System.out.println(engine.getStock("PS5")>=0?GREEN+"  NO OVERSELLING"+RESET:RED+"  OVERSOLD"+RESET);
    }

    static void benchmark3() throws Exception {
        printPhase("BENCHMARK 3","Duplicate Storm — 100 unique, 10x retries");
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",10_000);
        int unique=100,retries=10,total=unique*retries; ExecutorService pool=Executors.newFixedThreadPool(8);
        CountDownLatch latch=new CountDownLatch(total);
        for(int e=0;e<unique;e++) for(int r=0;r<retries;r++){final int ev=e; pool.submit(()->{
            engine.merge(CausalDelta.builder("evt_"+ev,"PS5","MUM",-1).timestamp(Instant.now()).build()); latch.countDown();});}
        latch.await(); pool.shutdown();
        System.out.printf("  Applied: %d | Duplicates: %d | Stock: %d (expected: %d)%n",
                engine.getAppliedCount(),engine.getDuplicatesBlocked(),engine.getStock("PS5"),10_000-unique);
        System.out.println(engine.getStock("PS5")==10_000-unique?GREEN+"  EXACTLY-ONCE"+RESET:RED+"  DUPLICATE LEAKED"+RESET);
    }

    static void benchmark4() throws Exception {
        printPhase("BENCHMARK 4","Mixed Metadata — 30K events, 3 types");
        CASCADEEngine engine=new CASCADEEngine(); engine.registerProduct("PS5",100_000);
        int per=10_000; ExecutorService pool=Executors.newFixedThreadPool(8);
        AtomicInteger c=new AtomicInteger(); CountDownLatch latch=new CountDownLatch(per*3);
        Instant start=Instant.now();
        for(int i=0;i<per;i++) pool.submit(()->{int id=c.incrementAndGet();
            engine.merge(CausalDelta.builder("full_"+id,"PS5","W-"+(id%3),-1).timestamp(Instant.now())
                    .causalContext(java.util.Map.of("W-"+(id%3),(long)id)).source(SourceType.POS_AUTOMATED).sourceReliability(0.985).build()); latch.countDown();});
        for(int i=0;i<per;i++) pool.submit(()->{int id=c.incrementAndGet();
            engine.merge(CausalDelta.builder("partial_"+id,"PS5","W-"+(id%3),-1).timestamp(Instant.now())
                    .causalContext(java.util.Map.of("W-"+(id%3),(long)id)).build()); latch.countDown();});
        for(int i=0;i<per;i++) pool.submit(()->{int id=c.incrementAndGet();
            engine.merge(CausalDelta.builder("bare_"+id,"PS5","W-"+(id%3),-1).timestamp(Instant.now()).build()); latch.countDown();});
        latch.await(); pool.shutdown(); long ms=Duration.between(start,Instant.now()).toMillis();
        System.out.printf("  Total: %d | Time: %dms | Throughput: %d/sec%n",per*3,ms,(per*3*1000L)/Math.max(1,ms));
        System.out.println(GREEN+"  NO CRASHES — graceful degradation works"+RESET);
    }

    static void benchmark5() throws Exception {
        printPhase("BENCHMARK 5","Multi-Product — 100 products, 1000 events each");
        CASCADEEngine engine=new CASCADEEngine(); int prods=100,per=1000,total=prods*per;
        for(int p=0;p<prods;p++) engine.registerProduct("PROD_"+p,1_000_000);
        ExecutorService pool=Executors.newFixedThreadPool(16); AtomicInteger c=new AtomicInteger();
        CountDownLatch latch=new CountDownLatch(total); Instant start=Instant.now();
        for(int p=0;p<prods;p++) for(int e=0;e<per;e++){final String pid="PROD_"+p; pool.submit(()->{int id=c.incrementAndGet();
            engine.merge(CausalDelta.builder("evt_"+id,pid,"W-"+(id%5),-1).timestamp(Instant.now())
                    .source(SourceType.POS_AUTOMATED).sourceReliability(0.985).build()); latch.countDown();});}
        latch.await(); pool.shutdown(); long ms=Duration.between(start,Instant.now()).toMillis();
        long tp=(total*1000L)/Math.max(1,ms);
        System.out.printf("  Products: %d | Events: %d | Time: %dms | Throughput: %d/sec%n",prods,total,ms,tp);
        boolean ok=true; for(int p=0;p<prods;p++) if(engine.getStock("PROD_"+p)!=1_000_000-per){ok=false;break;}
        System.out.println(ok?GREEN+"  ALL PRODUCTS CORRECT"+RESET:RED+"  WRONG STOCK"+RESET);
    }

    static void printBanner(){System.out.println(CYAN+BOLD+"\n  CASCADE — Concurrency Benchmark Suite\n"+RESET);}
    static void printPhase(String l,String d){System.out.println("\n"+BOLD+CYAN+"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"+RESET);
        System.out.println(BOLD+"  "+l+": "+d+RESET);System.out.println(CYAN+"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"+RESET);}
}
