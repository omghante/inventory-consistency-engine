# Changelog

## [1.1.0] - 2026-05-21

### Fixed
- Engine-level statistics (totalMerges, appliedCount, rejectedCount, conflictsDetected, duplicatesBlocked, oversellsPrevented) were using bare `long` increments — a data race under concurrent access. Migrated all counters to AtomicLong for lock-free thread-safe accumulation.

### Changed
- Replaced `synchronized(product)` locking with per-product StampedLock. Write operations (merge) use write locks. Read operations (getStock) use optimistic reads for zero-contention queries during flash sale conditions.
- StateRepository now manages per-product StampedLock instances alongside product state.

### Added
- Bounded vector clock support via `VectorClock.bounded(maxEntries)` factory. Prevents unbounded memory growth in systems with many warehouse nodes by pruning entries with the lowest counters after merge operations. Default behavior (unbounded) is unchanged.
- `VectorClock.size()` method for monitoring clock growth in production.
- `VectorClock.getMaxEntries()` method for configuration introspection.
- `StateRepository.getLock()` method for fine-grained concurrency control.
- `EngineStatisticsThreadSafetyTest` — validates counter accuracy under 10K+ concurrent merges, mixed accept/reject workloads, and duplicate storm scenarios.
- 8 new VectorClock pruning tests covering bounded creation, merge pruning, copy preservation, boundary conditions, and invalid input validation.

## [1.0.0] - 2026-05-19

### Added
- CASCADE engine with 4-step merge pipeline
- Vector clock-based causal ordering
- Trust-weighted conflict resolution
- Precondition-based oversell prevention
- Idempotency via event ID tracking
- Graceful degradation (full → partial → bare delta)
- Async event queue with retry and DLQ
- Worker cluster with horizontal scaling
- Event store with replay and state reconstruction
- Snapshot-based recovery coordination
- Metrics dashboard with system-wide observability
- 6 runnable demos (flash sale, benchmarks, full system, failure recovery, multi-warehouse, Redis outage)
- 80+ unit, integration, concurrency, and performance tests
- CI/CD with GitHub Actions (tests, benchmarks, CodeQL)
- Docker and docker-compose setup
- Prometheus + Grafana observability stack
