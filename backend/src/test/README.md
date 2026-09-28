# Backend tests

Two suites live here, split by a **JUnit tag rather than by location**: the stack
tests are annotated `@Tag("integration")` and surefire excludes that tag by default.
So a bare `mvn test` runs the offline unit tests and *nothing else* — it needs no
Mongo, no scripts service and no Docker, and finishes in seconds. The stack tests
only run when you clear the exclusion, and they **fail rather than skip** when the
stack is unreachable.

| Where | Kind | What |
|---|---|---|
| `java/.../operation/` | offline | the five edit operations and their undo/redo — has its own `README.md` |
| `java/.../decomposition/` | stack | `DecompositionE2ETest`, the real pipeline per case × strategy |
| `java/.../Mono2microApplicationTests` | stack | context-load smoke test |
| `resources/representations/` | fixtures | committed collector output — has its own `README.md` |

## Running them

`make` targets live in `backend/Makefile` and only wrap the longer `mvn` lines;
`mvn test` is deliberately not wrapped, since it is already the short one.

```bash
mvn test                              # offline unit tests only, ~2s

docker compose up -d mongo scripts    # the stack tests need these
make test-all                         # both suites
```

A case whose fixture files are absent **skips**, and the skipped combinations are
printed after the run — read that list before trusting a pass, because a run where
everything skipped is green but vacuous.

## make commands

All five live in `backend/Makefile` and run from `backend/`. The two that run tests
need the stack up; the two summaries just read the last run's reports.

| Target | Does |
|---|---|
| `make test-all` | both suites, no coverage gate |
| `make coverage` | both suites **plus** the coverage gate — what CI runs; fails until coverage rises |
| `make test-summary` | per-case pass/skip/fail with reasons |
| `make coverage-summary` | coverage percentages and the worst packages |
| `make coverage-report` | `coverage`, then both summaries — printed even when the gate fails |

The Mongo and scripts addresses default to the local stack and are overridable:
`make test-all MONGO_DB=mongodb://otherhost:27017`.

See `AGENT.md`'s Testing section for the tag mechanics and the coverage gate.
