# Changelog

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
