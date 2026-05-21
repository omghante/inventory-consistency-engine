# CASCADE Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Order Service                         │
└──────────────────────┬──────────────────────────────────┘
                       ↓
┌──────────────────────┴──────────────────────────────────┐
│                   Event Queue                            │
│  ┌──────────┐  ┌──────────┐  ┌─────────────────────┐   │
│  │ Producer │→ │  Queue   │→ │ Consumer (Workers)  │   │
│  └──────────┘  │ (Bounded)│  └──────────┬──────────┘   │
│                │          │    ↓ fail     │              │
│                │  Retry   │←───          │              │
│                │  Queue   │    ↓ max     │              │
│                └──────────┘   DLQ        │              │
└──────────────────────────────────────────┼──────────────┘
                                           ↓
┌──────────────────────────────────────────┴──────────────┐
│                  CASCADE Engine                          │
│                                                          │
│  Step 1: Idempotency    → eventId lookup                │
│  Step 2: Precondition   → minStock check                │
│  Step 3: Causality      → vector clock comparison       │
│  Step 4: Apply          → stock += delta                │
│                                                          │
│  ┌────────────────┐  ┌──────────────────┐               │
│  │ MergeProcessor │  │ ConflictResolver │               │
│  └────────────────┘  └──────────────────┘               │
│  ┌────────────────────┐  ┌─────────────────┐            │
│  │ DegradationManager │  │   TrustScorer   │            │
│  └────────────────────┘  └─────────────────┘            │
└──────────────────────────────────┬──────────────────────┘
                                   ↓
┌──────────────────────────────────┴──────────────────────┐
│                    Event Store                           │
│  Append-only log → Replay → State Reconstruction        │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐ │
│  │ ReplayEngine│  │SnapshotMgr   │  │RecoveryCoord   │ │
│  └─────────────┘  └──────────────┘  └────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

## Key Design Decisions

1. **Per-product StampedLock** — Different products process in parallel with zero cross-product contention. Write operations (merge) acquire write locks. Read operations (getStock) use optimistic reads for zero-contention queries under flash sale conditions.

2. **Lock-free statistics (AtomicLong)** — Engine-level counters (totalMerges, appliedCount, rejectedCount, etc.) use AtomicLong for lock-free thread-safe accumulation. No counter increments are lost under concurrent load.

3. **Metadata-driven degradation** — Same merge() call, different behavior based on available metadata. System never crashes when upstream services are unavailable.

4. **Bounded vector clocks** — Supports configurable max-entries pruning to prevent unbounded memory growth in systems with many warehouse nodes. Retains the most causally significant entries (highest counters). Follows Amazon Dynamo's clock pruning strategy.

5. **Append-only event log** — Enables replay without data loss. Combined with snapshot-based fast recovery.

6. **Zero external dependencies** — Pure Java, self-contained.

## Concurrency Model

```
Thread 1 (PS5 order)  ──→ StampedLock[PS5].writeLock()  ──→ merge pipeline
Thread 2 (PS5 order)  ──→ StampedLock[PS5].writeLock()  ──→ waits (same product)
Thread 3 (XBOX order) ──→ StampedLock[XBOX].writeLock() ──→ merge pipeline (parallel)
Thread 4 (PS5 query)  ──→ StampedLock[PS5].tryOptimisticRead() ──→ returns immediately
```

- **Writes** to the same product are serialized (required for correctness)
- **Writes** to different products run in parallel (no shared state)
- **Reads** use optimistic locking — zero contention unless a concurrent write invalidates the stamp, in which case it falls back to a read lock
- **Statistics** are lock-free AtomicLong — no mutex required for counters

