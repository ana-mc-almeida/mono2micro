# `operation/` tests

Offline unit tests for the five decomposition edit operations. No Spring context, no
Mongo, no Docker — a bare `mvn test` runs these and only these, because the
stack-dependent classes elsewhere are tagged `@Tag("integration")` and excluded by
default. See `AGENT.md`'s Testing section.

## Two kinds of file here

**Tests** end in `Test`. Maven's surefire plugin runs exactly these, by name — a class
without the suffix is never executed as a test, whatever it contains.

**Helpers** have no suffix, and are `final class` or `class`, never `public`. They build
inputs, compare outputs, or stand in for something the real code would reach for. Nothing
runs them directly; the tests use them.

| File | Kind | Role |
|---|---|---|
| `MergePartitionsOperationTest` | test | one class per operation… |
| `SplitPartitionsOperationTest` | test | |
| `TransferPartitionsOperationTest` | test | |
| `RenamePartitionsOperationTest` | test | |
| `FormClusterPartitionsOperationTest` | test | |
| `OperationHistoryBookkeepingTest` | test | depth/undo/redo bookkeeping across operations |
| `OperationDtoTest` | test | type strings, and the copy constructors that make a request executable |
| `DecompositionFixture` | helper | builder DSL — makes a `PartitionsDecomposition` as data |
| `DecompositionSnapshot` | helper | reduces a decomposition to comparable maps, for round-trips |
| `InMemoryHistory` | helper | fake `History`; keeps `ContextManager` out of reach |
| `RecordingRepresentationInformation` | helper | records which hooks an operation calls |

Each helper's own javadoc explains *why* it exists and what it deliberately does not
cover. Read `InMemoryHistory` before writing a test that calls `execute()`, and
`DecompositionFixture` before writing any test here.

## Why it is one flat folder

The folder path **is** the package name in Java, and the helpers are *package-private* on
purpose: they are internal scaffolding, not API, so nothing outside this package should
see them. Tests import production classes (`Partition`, `MergePartitionsOperation`) but
never import the helpers — same package, so they are simply in scope.

Moving the helpers into a `utils/` subfolder would make them a *different* package, which
in Java gets no privileged access to this one. They would all have to become `public`,
and the two that subclass production types to reach protected members might not compile
at all. The flat layout also mirrors `src/main/java/.../operation/`, which is Maven's
standard convention.

## Adding a test

1. Name it `SomethingTest` or surefire will skip it silently.
2. Build inputs with `DecompositionFixture.with()`, not by hand.
3. Compare decompositions with `DecompositionSnapshot`, not `assertEquals` — `Cluster`
   declares no `equals`, so direct comparison compares references and passes wrongly.
4. Never let a test reach `ContextManager`. It caches a whole application context in a
   static field, which leaks into every later test in the same JVM fork.

Findings these tests pin are written up as **Gap 7** in
`../implementation/docs/gap-analysis.md` (outside this repo).
