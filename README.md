<div align="center">

# CASCADE

### Causal Adaptive Scored Conflict-free Algorithm for Distributed Events

A distributed inventory reconciliation engine built for Amazon-scale e-commerce.

[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Tests](https://img.shields.io/badge/Tests-76_Passing-4CAF50?style=for-the-badge)](.)
[![License](https://img.shields.io/badge/License-MIT-2196F3?style=for-the-badge)](LICENSE)
[![Build](https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apachemaven)](pom.xml)

</div>

---

## The Problem

Amazon sells **~4,000 items per minute**. Each sale touches inventory data stored across dozens of warehouses, fulfillment centers, and regional databases. When two customers in Mumbai and Bangalore buy the last PS5 at the exact same millisecond — which order wins? What if a warehouse scanner sends an update, but the network delays it by 30 seconds, and by the time it arrives, 50 newer updates have already been processed? What if Redis goes down and your metadata service stops responding — does the entire checkout pipeline crash?

These aren't theoretical problems. They are the daily reality of distributed inventory systems.

**CASCADE solves all of them with a single function call.**

---

## The Core Idea

> ***"The algorithm doesn't change. The data does."***

CASCADE is **one merge function** — `engine.merge(delta)` — that automatically adapts its conflict resolution strategy based on what metadata is available:

```
Full metadata   →  Vector Clocks + Trust Scoring + Preconditions   (smartest)
Partial         →  Vector Clocks + Timestamp Fallback              (still good)
Bare delta      →  Direct application with idempotency             (survival mode)
```

When your metadata services are healthy, CASCADE uses vector clocks and trust scoring for precise conflict resolution. When services go down, it **gracefully degrades** instead of crashing. Your checkout never stops.

**Same engine. Same function. Same code path. Different data → different behavior.**

---

## Why This Exists

Amazon manages inventory across a distributed system that prioritizes **Availability** and **Partition Tolerance** over instant consistency (AP in the CAP theorem). This means:

- Multiple warehouses can process orders **simultaneously** without waiting for each other
- Data becomes consistent **eventually** (within milliseconds), not instantly
- The system **never stops accepting orders**, even during partial failures

CASCADE implements this philosophy using battle-tested distributed systems concepts:

| Real-World System | Concept Borrowed | What It Does in CASCADE |
|:--|:--|:--|
| **Amazon Dynamo** | Vector Clocks | Detects stale and concurrent updates |
| **DynamoDB** | Conditional Writes | Prevents overselling (`minStock` check) |
| **CRDTs** | Conflict-free Merge | Applies deltas additively, not overwriting |
| **Amazon SQS** | Async Queue + DLQ | Buffers events with retry and dead letter |
| **Apache Kafka** | Append-only Log | Enables replay and crash recovery |

CASCADE's innovation is combining all of these into **one adaptive merge function** with trust-weighted conflict resolution.

---

## Architecture

```
                        ┌──────────────────────┐
                        │    Order Services     │
                        │  (Mumbai, Bangalore,  │
                        │   Delhi, Hyderabad)   │
                        └──────────┬───────────┘
                                   ↓
              ┌────────────────────────────────────────┐
              │             Event Queue                 │
              │                                        │
              │  ┌──────────┐     ┌────────────────┐   │
              │  │ Producer │ ──→ │ Bounded Queue  │   │
              │  └──────────┘     └───────┬────────┘   │
              │                     ↓ fail │           │
              │              ┌──────┘      ↓           │
              │              │     ┌───────────────┐   │
              │         Retry with │  Dead Letter   │   │
              │         Backoff    │  Queue (DLQ)   │   │
              │              │     └───────────────┘   │
              └──────────────┼─────────────────────────┘
                             ↓
              ┌────────────────────────────────────────┐
              │          Worker Cluster                 │
              │                                        │
              │    ┌────────┐ ┌────────┐ ┌────────┐   │
              │    │Worker 1│ │Worker 2│ │Worker N│   │
              │    └───┬────┘ └───┬────┘ └───┬────┘   │
              │        └──────────┼──────────┘        │
              └───────────────────┼────────────────────┘
                                  ↓
              ┌────────────────────────────────────────┐
              │           CASCADE Engine                │
              │                                        │
              │   Step 1: Idempotency Check            │
              │   Step 2: Precondition Check            │
              │   Step 3: Causal Ordering               │
              │   Step 4: Delta Application             │
              │                                        │
              │   ┌────────────────┐ ┌──────────────┐  │
              │   │ MergeProcessor │ │ConflictResolver│ │
              │   └────────────────┘ └──────────────┘  │
              │   ┌──────────────────┐ ┌────────────┐  │
              │   │DegradationManager│ │ TrustScorer │  │
              │   └──────────────────┘ └────────────┘  │
              └───────────────────┬────────────────────┘
                                  ↓
              ┌────────────────────────────────────────┐
              │           Event Store                   │
              │   Append-only commit log                │
              │   State reconstruction via replay       │
              │   Snapshot-based fast recovery          │
              └────────────────────────────────────────┘
```

---

## The 4-Step Merge Pipeline

Every inventory event passes through exactly 4 steps. Each step is **optional** — if the required metadata is missing, the step is skipped gracefully (not crashed).

```
┌─────────────────────────────────────────────────────────────────┐
│                     CausalDelta arrives                         │
└──────────────────────────┬──────────────────────────────────────┘
                           ↓
┌──────────────────────────────────────────────────────────────────┐
│  STEP 1: IDEMPOTENCY                                            │
│  Has this eventId been seen before?                              │
│  YES → DUPLICATE_REJECTED (safe to ignore)                      │
│  NO  → continue                                                 │
└──────────────────────────┬───────────────────────────────────────┘
                           ↓
┌──────────────────────────────────────────────────────────────────┐
│  STEP 2: PRECONDITION                                           │
│  Does current stock satisfy minStock requirement?                │
│  NO  → CONDITION_FAILED (oversell prevented)                    │
│  YES → continue                                                 │
│  (skipped if no precondition attached)                           │
└──────────────────────────┬───────────────────────────────────────┘
                           ↓
┌──────────────────────────────────────────────────────────────────┐
│  STEP 3: CAUSAL ORDERING                                        │
│  Compare vector clocks:                                          │
│  BEFORE     → STALE_REJECTED (outdated event)                   │
│  CONCURRENT → Trust Scoring (resolve conflict)                   │
│  AFTER      → continue to step 4                                │
│  (skipped if no vector clock attached)                           │
└──────────────────────────┬───────────────────────────────────────┘
                           ↓
┌──────────────────────────────────────────────────────────────────┐
│  STEP 4: APPLY DELTA                                            │
│  stock += delta                                                  │
│  Merge vector clocks (CRDT)                                      │
│  Record in event history                                         │
│  → APPLIED                                                    │
└──────────────────────────────────────────────────────────────────┘
```

---

## Quick Start

### Prerequisites

- **Java 17** or higher
- **Maven 3.9+**

### Build & Test

```bash
# Compile
mvn compile

# Run all 76 tests
mvn test

# Or use the Makefile
make build
make test
```

### Your First Merge (3 Lines of Code)

```java
import com.cascade.engine.CASCADEEngine;
import com.cascade.delta.CausalDelta;
import com.cascade.engine.MergeResult;

// Create engine and register a product
CASCADEEngine engine = new CASCADEEngine();
engine.registerProduct("PS5", 100);

// Someone bought 1 PS5
MergeResult result = engine.merge(
    CausalDelta.builder("order-12345", "PS5", "MUM-01", -1)
        .timestamp(Instant.now())
        .build()
);

System.out.println(result);
// APPLIED [order-12345] stock: 100 → 99
```

That's it. **One function. Four arguments.** The builder lets you attach optional metadata for smarter resolution.

---

## How to Use CASCADE

### The Builder Pattern

Every inventory update is a `CausalDelta` built with a fluent API:

```java
CausalDelta delta = CausalDelta.builder(
        "order-12345",     // Unique event ID (for deduplication)
        "PS5",             // Product ID
        "MUM-01",          // Warehouse / source node
        -1                 // Delta (negative = sold, positive = restocked)
    )
    .timestamp(Instant.now())                          // When this happened
    .causalContext(Map.of("MUM-01", 42L))              // Vector clock state
    .source(SourceType.POS_AUTOMATED)                  // Source classification
    .sourceReliability(0.985)                          // How reliable (0.0–1.0)
    .precondition(new Preconditions(1))                // Minimum stock required
    .build();
```

### The Intelligence Ladder

Attach MORE metadata for SMARTER resolution. Attach LESS when services are down — it still works:

| What You Attach | Resolution Mode | When to Use |
|:--|:--|:--|
| Everything (VC + Trust + Precondition) | `CAUSAL` + `TRUST_SCORED` | Normal operations |
| Vector Clock + Trust | `CAUSAL` + `TRUST_SCORED` | Precondition service unavailable |
| Vector Clock only | `CAUSAL` + timestamp fallback | Redis / metadata service is down |
| Just eventId + delta | `DIRECT` | Full network partition (survival mode) |

**The same `merge()` call handles all four levels. Your application code doesn't change.**

### Reading the Result

```java
MergeResult result = engine.merge(delta);

if (result.isApplied()) {
    System.out.println("Stock: " + result.getOldQuantity() + " → " + result.getNewQuantity());
    System.out.println("Mode: " + result.getResolutionMode());
} else {
    System.out.println("Rejected: " + result.getReason());
}
```

### All Possible Outcomes

| Action | Meaning | What to Do |
|:--|:--|:--|
| `APPLIED` | Stock was updated | Confirm the order |
| `DUPLICATE_REJECTED` | Already processed this eventId | Safe to ignore |
| `CONDITION_FAILED` | Not enough stock | Tell customer "sold out" |
| `STALE_REJECTED` | Event is outdated (old vector clock) | Log and discard |
| `LOW_TRUST_REJECTED` | Concurrent conflict, low trust | Send to manual review |
| `TIMESTAMP_REJECTED` | Older timestamp in LWW fallback | Log and discard |
| `UNKNOWN_PRODUCT` | Product not registered | Fix your code |

---

## Real-World Scenarios

### 1. Flash Sale — Preventing Overselling

```java
engine.registerProduct("PS5", 500);  // Only 500 units

// 2000 customers ordering simultaneously
for (int i = 0; i < 2000; i++) {
    MergeResult result = engine.merge(
        CausalDelta.builder("order-" + i, "PS5", "checkout-" + (i % 5), -1)
            .timestamp(Instant.now())
            .precondition(new Preconditions(1))     // Won't go below 0
            .source(SourceType.POS_AUTOMATED)
            .sourceReliability(0.985)
            .build()
    );

    if (result.getAction() == MergeResult.Action.CONDITION_FAILED) {
        // "Sorry, sold out!"
    }
}

engine.getStock("PS5");  // Exactly 0. Never -1. Never oversold.
```

### 2. Multi-Warehouse Concurrent Updates

```java
// Mumbai sells 5 PS5s
engine.merge(CausalDelta.builder("scan-mum", "PS5", "MUM", -5)
    .timestamp(Instant.now())
    .causalContext(Map.of("MUM", 10L))
    .source(SourceType.WAREHOUSE_SCANNER)
    .sourceReliability(0.92)
    .build());

// Bangalore sells 3 PS5s at the SAME time (concurrent event)
engine.merge(CausalDelta.builder("scan-blr", "PS5", "BLR", -3)
    .timestamp(Instant.now())
    .causalContext(Map.of("BLR", 8L))        // Different warehouse → CONCURRENT
    .source(SourceType.WAREHOUSE_SCANNER)
    .sourceReliability(0.92)
    .build());

// CASCADE detects CONCURRENT → both have high trust → both applied
// Stock: 1000 - 5 - 3 = 992 
```

### 3. Network Retries — Exactly-Once Processing

```java
// Same event delivered 5 times due to network issues
for (int retry = 0; retry < 5; retry++) {
    engine.merge(CausalDelta.builder("order-99999", "PS5", "MUM", -1)
        .timestamp(Instant.now()).build());
}
// Stock changed only ONCE. 4 duplicates silently rejected.
```

### 4. Stale Data Rejection

```java
// Fresh event processed first
engine.merge(CausalDelta.builder("fresh", "PS5", "MUM", -5)
    .causalContext(Map.of("MUM", 50L)).build());

// Hours-old event arrives late (vector clock is behind)
MergeResult result = engine.merge(
    CausalDelta.builder("old-scan", "PS5", "MUM", +100)
        .causalContext(Map.of("MUM", 10L))     // 10 < 50 → STALE
        .build());

result.getAction();  // STALE_REJECTED — old data silently discarded
```

### 5. Full Distributed Pipeline

```java
CASCADEEngine engine = new CASCADEEngine();
engine.registerProduct("PS5", 10_000);

EventQueue queue = new EventQueue();
EventStore store = new EventStore();
WorkerCluster cluster = new WorkerCluster(queue, engine, store, 4);

cluster.startAll();

// Enqueue thousands of events
for (int i = 0; i < 10_000; i++) {
    queue.enqueue(CausalDelta.builder("order-" + i, "PS5", "W-" + (i % 5), -1)
        .timestamp(Instant.now()).precondition(new Preconditions(1)).build());
}

// Scale up during traffic spikes
cluster.scaleUp();
cluster.scaleUp();

// Monitor
MetricsDashboard dashboard = new MetricsDashboard(engine, queue, cluster, store);
System.out.println(dashboard.render());

cluster.shutdownAll();
```

### 6. Crash Recovery

```java
// After a crash — rebuild state from event log
CASCADEEngine recovered = new CASCADEEngine();
ReplayEngine replayer = new ReplayEngine(store);
var result = replayer.replayAll(recovered, "PS5", 10_000);
// State perfectly restored from append-only log
```

---

## Source Types

| Source | Trust Bonus | Example |
|:--|:--|:--|
| `POS_AUTOMATED` | 0.95 | Barcode scanner at checkout counter |
| `WAREHOUSE_SCANNER` | 0.90 | Automated shelf counting robot |
| `ERP_SYSTEM` | 0.85 | SAP / Oracle inventory sync |
| `MANUAL_ENTRY` | 0.60 | Employee typing a number manually |
| `UNKNOWN` | 0.40 | Source not identified |

---

## Benchmark Results

| Benchmark | Events | Threads | Result | Throughput |
|:--|:--|:--|:--|:--|
| Pure Throughput | 100K | 8 | Zero data loss | **510K merges/sec** |
| Flash Sale | 2,000 orders / 500 stock | 16 | Zero overselling | 500 accepted, 1500 rejected |
| Duplicate Storm | 1,000 msgs / 100 unique | 8 | Exactly-once | 900 duplicates blocked |
| Mixed Metadata | 30K events | 8 | No crashes | Graceful degradation |
| Multi-Product | 100K events / 100 products | 16 | All correct | **1M+ merges/sec** |

---

## Run the Demos

```bash
# See all 6 CASCADE behaviors in one scenario
make demo-flash

# Stress test with throughput numbers
make benchmark

# Full distributed pipeline (queue → workers → engine → store)
make demo-system

# 5 failure scenarios (crash, duplicates, stale data, recovery, degradation)
make demo-failure
```

---

## Test Suite

**76 tests. All passing. Zero failures.**

| Category | Tests | What's Covered |
|:--|:--|:--|
| **Unit — Engine** | 26 | Idempotency, preconditions, causality, degradation, edge cases |
| **Unit — MergeProcessor** | 6 | Each pipeline step individually |
| **Unit — ConflictResolver** | 3 | Trust scoring, low-trust rejection, fallback |
| **Unit — VectorClock** | 15 | All causal relations, CRDT properties |
| **Unit — CausalityComparator** | 4 | Newer/stale/concurrent detection |
| **Unit — TrustScorer** | 4 | High trust, low trust, no metadata, freshness |
| **Unit — EventQueue** | 5 | Enqueue, poll, backpressure, DLQ, metrics |
| **Unit — RetryPolicy** | 2 | Retry limits, exponential backoff |
| **Unit — ReplayEngine** | 2 | Full replay, incremental replay |
| **Unit — SnapshotManager** | 2 | Take/retrieve snapshots |
| **Integration** | 4 | Full pipeline, queue→worker, replay recovery, multi-warehouse |
| **Concurrency** | 3 | 10K concurrent merges, flash sale, cluster load |
| **Performance** | 3 | Throughput >100K/sec, latency <1ms, memory <100MB |

---

## Project Structure

```
inventory-consistency-engine/
│
├── .github/workflows/                     # CI/CD
│   ├── unit-tests.yml                     #   Run tests on every push
│   ├── benchmark.yml                      #   Performance benchmarks
│   └── codeql-analysis.yml                #   Security scanning
│
├── docs/                                  # Engineering documentation
│   ├── architecture/                      #   System design docs
│   ├── algorithms/                        #   Algorithm research
│   │   └── cascade/                       #   CASCADE deep dives
│   ├── benchmarks/                        #   Performance analysis
│   └── diagrams/                          #   Mermaid architecture diagrams
│
├── infra/                                 # Infrastructure
│   ├── docker/                            #   Dockerfile + docker-compose
│   ├── prometheus/                        #   Metrics scraping config
│   └── scripts/                           #   Benchmark and load test scripts
│
├── datasets/                              # Simulation datasets (JSON)
│
├── src/main/java/com/cascade/
│   ├── engine/                            #  Core Engine
│   │   ├── CASCADEEngine.java             #    Unified merge() entry point
│   │   ├── MergeProcessor.java            #    4-step pipeline
│   │   ├── ConflictResolver.java          #    Trust-based resolution
│   │   ├── DegradationManager.java        #    Graceful fallback to LWW
│   │   └── MergeResult.java               #    Outcome with observability
│   │
│   ├── causality/                         #  Causal Ordering
│   │   ├── VectorClock.java               #    Dynamo-style vector clocks
│   │   ├── CausalRelation.java            #    BEFORE / AFTER / CONCURRENT
│   │   └── CausalityComparator.java       #    Static comparison utilities
│   │
│   ├── delta/                             # Event Model
│   │   ├── CausalDelta.java               #    Self-describing inventory event
│   │   ├── DeltaMetadata.java             #    Optional metadata container
│   │   ├── Preconditions.java             #    Conditional writes
│   │   └── SourceType.java               #    Source classification enum
│   │
│   ├── state/                             #  Inventory State
│   │   ├── ProductState.java              #    Product stock + history
│   │   ├── StateRepository.java           #    Thread-safe product storage
│   │   ├── AppliedEventRegistry.java      #    Idempotency tracking
│   │   └── InventorySnapshot.java         #    Point-in-time snapshot
│   │
│   ├── scoring/                           #  Trust Resolution
│   │   ├── TrustScorer.java               #    Weighted trust computation
│   │   ├── TrustScore.java                #    Score value object
│   │   └── ReliabilityTracker.java        #    Historical reliability
│   │
│   ├── queue/                             #  Async Processing
│   │   ├── EventQueue.java                #    Bounded queue + retry + DLQ
│   │   ├── RetryPolicy.java               #    Exponential backoff config
│   │   ├── DeadLetterQueue.java           #    Failed event capture
│   │   ├── BackpressureController.java    #    Flow control
│   │   ├── QueueConsumer.java             #    Consumer interface
│   │   └── QueueProducer.java             #    Producer interface
│   │
│   ├── worker/                            #  Distributed Workers
│   │   ├── Worker.java                    #    Event processor thread
│   │   ├── WorkerCluster.java             #    Horizontal scaling manager
│   │   ├── WorkerCoordinator.java         #    Auto-scaling logic
│   │   └── WorkerHeartbeat.java           #    Health monitoring
│   │
│   ├── recovery/                          # Fault Tolerance
│   │   ├── EventStore.java                #    Append-only commit log
│   │   ├── ReplayEngine.java              #    State reconstruction
│   │   ├── SnapshotManager.java           #    Periodic checkpointing
│   │   └── RecoveryCoordinator.java       #    Orchestrated recovery
│   │
│   ├── metrics/                           #  Observability
│   │   ├── MetricsDashboard.java          #    System-wide dashboard
│   │   ├── MetricsCollector.java          #    Low-overhead counters
│   │   ├── ThroughputMetrics.java         #    Sliding-window throughput
│   │   ├── ConflictMetrics.java           #    Per-product conflict tracking
│   │   └── QueueMetrics.java              #    Queue depth monitoring
│   │
│   ├── api/                               #  REST API Facades
│   ├── config/                            #  Configuration Records
│   ├── exceptions/                        # Custom Exceptions
│   ├── utils/                             #  Utilities
│   │
│   └── demo/                              #  Runnable Demos
│       ├── PS5FlashSaleDemo.java          #    All 6 behaviors
│       ├── ConcurrencyBenchmark.java      #    Throughput stress test
│       ├── FullSystemSimulation.java      #    End-to-end pipeline
│       ├── FailureRecoveryDemo.java       #    5 failure scenarios
│       ├── MultiWarehouseSimulation.java  #    5-warehouse sync
│       └── RedisOutageSimulation.java     #    Degradation under outage
│
├── src/test/java/com/cascade/
│   ├── unit/                              # Unit tests by package
│   ├── integration/                       # Cross-component tests
│   ├── concurrency/                       # Thread safety tests
│   └── performance/                       # Benchmark assertions
│
├── ARCHITECTURE.md                        # System design document
├── CONTRIBUTING.md                        # Contribution guidelines
├── CHANGELOG.md                           # Version history
├── Makefile                               # Build shortcuts
└── pom.xml                                # Maven configuration
```

---

## Inspired By

| System | What CASCADE Borrows |
|:--|:--|
| **Amazon Dynamo** | Vector clocks for causal ordering across nodes |
| **DynamoDB** | Conditional writes for oversell prevention |
| **CRDTs** | Conflict-free delta merging (commutative, associative, idempotent) |
| **Amazon SQS** | Async event queue with retry, backoff, and dead letter queue |
| **Apache Kafka** | Append-only event log for replay and crash recovery |
| **Circuit Breakers** | Graceful degradation when metadata services are unavailable |

**CASCADE's contribution**: combining all of these into one adaptive merge abstraction with trust-weighted conflict resolution and automatic graceful degradation — so your application code stays the same regardless of system health.

---

## License

MIT — see [LICENSE](LICENSE)

