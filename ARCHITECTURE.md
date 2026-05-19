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

1. **Per-product locking** — Different products process in parallel
2. **Metadata-driven degradation** — Same merge() call, different behavior
3. **Append-only event log** — Enables replay without data loss
4. **Snapshot + incremental replay** — Fast recovery
5. **Zero external dependencies** — Pure Java, self-contained
