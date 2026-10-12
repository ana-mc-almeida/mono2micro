# Characterization files — recorded REST answers

These files hold what today's tool answers over the REST API: the clusters an algorithm run produces, the metrics, and the MoJo/Purity comparison. They are the **characterization the restructure must keep**. A later ticket that ports an endpoint changes how the JSON is fetched (in `HttpDecompositionPipeline`), never these files, so a mismatch after a port is a regression, not a reason to re-record.

They hold only what the API returns: paths, JSON and type strings, never a Java class name. Nothing here depends on the tool's domain classes, so the files survive an internal refactor of the backend.

`Golden.java` compares them. The inputs that produce them are the collector files under `../representations/`.

## Expected layout

```
backend/src/test/resources/characterization/
├── clusters/<case-name>/<strategy-label>.json          written by DecompositionE2ETest
├── metrics/<case-name>/<strategy-label>/<what>.json    written by PipelineCharacterizationE2ETest
└── comparison/<case-name>/<a>-vs-<b>.json              written by PipelineCharacterizationE2ETest
```

`<strategy-label>` is `StrategyCase.label()`: `accesses`, `repository`, `class-vectorization`, `entity-vectorization`, `fv-callgraph`, `fv-sequence-accesses`, `structure`.

Every algorithm run uses the cut `N = 3` (`HttpDecompositionPipeline.java:143`). The other strategy parameters are in `StrategyCase`.

## What each file pins

<!-- prettier-ignore-start -->
| File | Content| Test |
| ---- | ------ | ---- |
| `clusters/<case>/<strategy>.json` | Cluster name → sorted entity names, from `getClusters` | `DecompositionE2ETest`, one per case × strategy |
| `metrics/quizzes-tutor/accesses/decomposition.json` | Decomposition and cluster metrics of the algorithm run, Silhouette Score included | `metricsOfTheAlgorithmRun` |
| `metrics/quizzes-tutor/accesses/functionalities.json` | Each functionality's metrics, and those of each of its redesigns | `metricsOfTheAlgorithmRun` |
| `metrics/quizzes-tutor/accesses/decomposition-after-merge.json` | The metrics after a merge of `Cluster0` and `Cluster2`, and an `/update` | `metricsAfterAnEdit` |
| `metrics/quizzes-tutor/accesses/expert-decomposition.json` | The metrics of the imported expert clustering (no Silhouette Score, since no algorithm ran) | `expertImport` |
| `metrics/quizzes-tutor/repository/decomposition.json` | Decomposition and cluster metrics of the Repository run, `TSR` included | `metricsOfTheRepositoryRun` |
| `metrics/quizzes-tutor-structure/structure/decomposition.json` | Decomposition and cluster metrics of the Structure run | `metricsOfTheStructureRun` |
| `comparison/quizzes-tutor/accesses-vs-expert.json` | MoJo and Purity, algorithm run vs. expert clustering, keyed by analysis `type` | `comparison` |
<!-- prettier-ignore-end -->

The edit tests (merge, split, transfer, rename, form cluster, undo/redo) assert their clusters inline in `PipelineCharacterizationE2ETest`, so they have no file here.

## Coverage

`clusters/` covers every case × strategy whose fixtures exist (see `../representations/README.md`, "A case need not cover every strategy"):

| Case                      | Strategies with a cluster file                                                                      |
| ------------------------- | --------------------------------------------------------------------------------------------------- |
| `quizzes-tutor`           | accesses, repository, class-vectorization, entity-vectorization, fv-callgraph, fv-sequence-accesses |
| `quizzes-tutor-structure` | accesses, structure                                                                                 |
| `spring-petclinic`        | accesses, repository                                                                                |

`PipelineCharacterizationE2ETest` builds the edits, the comparison and most metrics from one starting decomposition: `quizzes-tutor` with the Accesses strategy. That gives a known three-cluster answer to start from. MoJo and Purity are computed from the clusters alone, so a single case is enough for them.

Metrics are the exception. **The metric set depends on the strategy**: each representation contributes its own decomposition metrics (`RepresentationInformation.getDecompositionMetrics()`). So `metrics/` also pins the two strategies that bring metrics of their own:

| Strategy   | Case                      | Why it is pinned separately                                                 |
| ---------- | ------------------------- | --------------------------------------------------------------------------- |
| Accesses   | `quizzes-tutor`           | The base set: Cohesion, Complexity, Coupling, Performance                   |
| Repository | `quizzes-tutor`           | Adds `TSR` (`RepositoryInformation.java:63`)                                |
| Structure  | `quizzes-tutor-structure` | Computes the base set through `StructureInformation`, a different code path |

The code embeddings strategies use the Accesses metrics, so they need no file of their own.

## Known oddities, pinned on purpose

On `quizzes-tutor` at `N = 3`, `class-vectorization`, `fv-callgraph` and `fv-sequence-accesses` put every entity in `Cluster0` and leave `Cluster1` and `Cluster2` empty. That is today's behavior, so it is what the files record.

In `metrics/quizzes-tutor-structure/structure/decomposition.json`, the decomposition's Complexity (`4.509`) equals `Cluster0`'s, while the other two clusters are at `52.714` and `44.273`. That is also today's behavior. If the restructure changes either, the test goes red, and the change is either a regression or a fix that needs a decision.

## How the comparison works

- Object fields are compared by name. A field missing from either side is a difference.
- Arrays are compared in order. `getClusters` sorts cluster names and entity names, so order is stable. The comparison drops MoJo's `falsePairs`, whose order nothing promises; `falsePositive` and `falseNegative` already pin their count.
- Numbers match within `1e-6`, so a last-bit difference from another machine's floating point is not a failure. **An integer never matches a decimal**, however close: `80` where `80.0` was recorded is a JSON contract change.
- At most 20 differences are listed in the failure message.

## Recording a value

On a mismatch, or when no file exists yet, the actual JSON is written to `backend/target/characterization-actual/<same path>`, and the assertion names both paths. To record:

1. Read the actual file and the diff. Re-record **only** when the change is intended.
2. Copy it over the file here.
3. Say why in the commit message. A re-recording during a port needs a decision, not just a copy.

The written files have their object fields sorted, so two recordings diff cleanly.

## Adding a case or a strategy

- **Clusters:** add the fixture folder under `../representations/` and the case name to `DecompositionE2ETest.CASES`. The first run fails with "Nothing is recorded for…" and writes the actual JSON. Read it, then copy it here.
- **Metrics or comparison for another case or strategy:** add a test to `PipelineCharacterizationE2ETest` that calls `Golden.assertMatches` with a new path under `metrics/` or `comparison/`. Record the files the same way.

## Running

The tests need the stack:

```bash
docker compose up -d mongo scripts functionality_refactor
```

Locally, a test whose stack or fixture is missing **skips** and gives the reason. In CI, `REQUIRE_STACK=true` makes it fail instead. `functionality_refactor` is needed only by the export test, which skips on its own when only that service is down.
