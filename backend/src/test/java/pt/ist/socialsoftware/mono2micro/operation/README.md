# `operation/` tests

Offline unit tests for the five decomposition edit operations, and for undo and redo.
No Spring context, no Mongo, no Docker — a bare `mvn test` runs these and only these,
because the stack-dependent classes elsewhere are tagged `@Tag("integration")` and
excluded by default. See `AGENT.md`'s Testing section.

## Layout

```
operation/                               shared helpers, public
├── DecompositionFixture
├── DecompositionSnapshot
├── InMemoryHistory
├── unit/                                one production class at a time
│   ├── <Op>PartitionsOperationTest      × 5
│   ├── OperationDtoTest
│   ├── OperationHistoryBookkeepingTest
│   └── RecordingRepresentationInformation
└── characterization/                    the manual operations end to end, through the services
    ├── ManualOperationCharacterizationTest
    ├── ManualEditing
    ├── StubContextManager
    └── InMemoryFunctionalityService
```

**Tests** end in `Test`. Maven's surefire plugin runs exactly these, by name: a class
without the suffix is never executed as a test, whatever it contains.

**Helpers** have no suffix. They build inputs, compare outputs, or stand in for something
the real code would reach for. Nothing runs them directly; the tests use them.

**Where a helper goes.** In Java the folder path is the package, and a class without
`public` is visible only inside its own package; a subfolder is a *different* package, with
no special access to its parent. So:

- a helper used by one folder lives in that folder and stays package-private;
- a helper used by both `unit/` and `characterization/` lives here in `operation/` and has
  to be `public`. That is `DecompositionFixture` and `DecompositionSnapshot`, plus
  `InMemoryHistory`, which only `unit/` names but which `DecompositionFixture.build()`
  creates, so it must sit where the fixture does.

<!-- prettier-ignore-start -->
| File | Kind | Role |
|---|---|---|
| `unit/<Op>PartitionsOperationTest` | test | one class per operation: its cluster arithmetic, coupling dependencies and guards |
| `unit/OperationHistoryBookkeepingTest` | test | depth/undo/redo bookkeeping across operations |
| `unit/OperationDtoTest` | test | type strings, and the copy constructors that make a request executable |
| `unit/RecordingRepresentationInformation` | helper | records which hooks an operation calls |
| `characterization/ManualOperationCharacterizationTest` | test | each operation end to end, as the architect sees it: clusters, functionalities and redesigns, undo/redo, history position. Undo losing functionalities is a known defect: the "today" tests pin it, and `@Disabled` twins assert the fix ticket 12 brings |
| `characterization/ManualEditing` | helper | issues edits, undo and redo through the services the controllers call |
| `characterization/InMemoryFunctionalityService` | helper | fake `FunctionalityService`; keeps redesigns in memory instead of GridFS |
| `characterization/StubContextManager` | helper | fills `ContextManager`'s cache with a stub serving only that fake |
| `DecompositionFixture` | helper | builder DSL — makes a `PartitionsDecomposition` as data |
| `DecompositionSnapshot` | helper | reduces a decomposition to comparable maps, for round-trips |
| `InMemoryHistory` | helper | fake `History`; keeps `ContextManager` out of reach |
<!-- prettier-ignore-end -->

Each helper's own javadoc explains *why* it exists and what it deliberately does not
cover. Read `InMemoryHistory` before writing a test that calls `execute()`, and
`DecompositionFixture` before writing any test here.

## Adding a test

1. Name it `SomethingTest` or surefire will skip it silently.
2. Build inputs with `DecompositionFixture.with()`, not by hand.
3. Compare decompositions with `DecompositionSnapshot`, not `assertEquals` — `Cluster`
   declares no `equals`, so direct comparison compares references and passes wrongly.
4. Never let a test reach the real `ContextManager`. It caches a whole application context
   in a static field, which leaks into every later test in the same JVM fork. A test that
   needs `AccessesInformation`'s hooks installs `StubContextManager` in `@BeforeEach` and
   uninstalls it in `@AfterEach`.
5. A test that must survive tickets 05, 11 and 12 unchanged drives edits through
   `ManualEditing`, never through an operation class.

Findings these tests pin are written up as **Gap 7** in
`../implementation/docs/gap-analysis.md` (outside this repo).
