# Contributing to CASCADE

## Development Setup
1. Java 17+ and Maven 3.9+
2. `mvn compile` to build
3. `mvn test` to run all tests

## Code Structure
- `engine/` — Core merge pipeline (modify with extreme care)
- `causality/` — Vector clock implementation
- `delta/` — Event model and metadata
- `state/` — Product state management
- `scoring/` — Trust-based conflict resolution
- `queue/` — Async event processing
- `worker/` — Distributed worker management
- `recovery/` — Event sourcing and replay

## Testing Requirements
- All PRs must pass existing tests
- New features require unit tests
- Concurrency changes require stress tests
- Performance-sensitive changes require benchmarks

## Commit Convention
- `feat:` New feature
- `fix:` Bug fix
- `perf:` Performance improvement
- `test:` Test additions
- `docs:` Documentation
- `refactor:` Code restructuring
