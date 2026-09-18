# AGENT.md

Guidance for coding agents working in this repository. `CLAUDE.md` is a local, gitignored copy of this file.

## What this repository is

A fork of [socialsoftware/mono2micro](https://github.com/socialsoftware/mono2micro),
and **the implementation target of Ana Almeida's MSc thesis**.

Mono2Micro migrates Spring-Boot monoliths (FenixFramework and Spring Data ORMs) to
microservices by identifying candidate services that **minimize the number of system
transactions per business transaction** — minimizing the relaxed consistency that
splitting a monolith introduces.

> **Thesis context lives in `../implementation/`**, not here: the feature model this
> tool is being refactored to instantiate, the gap analysis, and the decision log.
> Read `../implementation/docs/gap-analysis.md` before changing an extension point —
> it records which axes are deliberately single-valued today and why that matters.

**Issues and specs also live in `../implementation/`, not here.** This repo is public
and holds code only; the thesis tracker is private. One feature per directory at
`../implementation/.scratch/<feature-slug>/`, with the spec at `spec.md` and one file
per ticket under `issues/`. Conventions: `../implementation/docs/agents/issue-tracker.md`.

When implementing a ticket, read it from that path and write code here. Record the
resulting decision in `../implementation/docs/decisions.md`.

## Architecture — six services

Defined in `docker-compose.yml`:

| Service | Port | Stack | Role |
|---|---|---|---|
| `mongo` | 27017 | MongoDB | All persisted state; GridFS for large artifacts |
| `mongo-express` | 8081 | — | Database admin UI |
| `backend` | 8080 | Spring Boot 2.1.2, Java 8 | Orchestration, metrics, operations. Context path `/mono2micro`. **Does not cluster.** |
| `scripts` | 5002 | FastAPI / Python | The actual clustering (SciPy) + code2vec |
| `functionality_refactor` | 5001 | Go 1.14 | Heuristic functionality refactoring |
| `frontend` | 3000 | React 17 + TypeScript | UI |

The **8 collectors are not services** — run-once programs that emit JSON, uploaded
through the frontend.

Note that **clustering already crosses a process boundary**: the backend calls the
Python service over HTTP via `WebClient`. Any mental model of "one backend process"
is wrong.

## Running

```bash
docker-compose build && docker-compose up
docker-compose up --no-deps -d --build backend     # rebuild backend only
docker-compose build --no-cache && docker-compose up --build   # clean rebuild
```

UI at http://localhost:3000, mongo-express at http://localhost:8081.

### Per component

```bash
# Backend (Spring Boot)
cd backend && mvn clean install -DskipTests
java -Djava.security.egd=file:/dev/./urandom -jar ./target/mono2micro-0.0.1-SNAPSHOT.jar

# Scripts (FastAPI)
cd scripts && pip install -r requirements.txt && python main.py     # 0.0.0.0:5002

# Frontend (React)
cd frontend && npm install --legacy-peer-deps && npm start          # :3000

# functionality_refactor (Go)
cd tools/functionality_refactor/src
make test          # unit + integration
make unit-test     # -tags=unit
make lint          # go fmt, go vet, golangci-lint
make build
```

### Setup gotchas

1. **`specific.properties` must exist** in `backend/src/main/resources/`, created from
   `specific.properties.example`. It is gitignored. Its baked-in default points at an
   authenticated Mongo URI, which is **stale** — the last upstream commit moved
   docker-compose to `--noauth`. It works because `MONGO_DB` overrides it, so running
   the backend outside Docker requires exporting `MONGO_DB`.
2. **`codebases/` must be empty before packaging.** Spring Boot cannot build a jar with
   more than 65535 files ([spring-boot#2895](https://github.com/spring-projects/spring-boot/issues/2895)).
   `codebases/` is a runtime volume mounted into three containers.
3. **code2vec model** must be downloaded into `scripts/models/` for code-embedding
   features (see `README.md`).
4. Env vars override the properties file: `SCRIPTS_ADDRESS`, `MONGO_DB`,
   `MONGO_DB_NAME`.
5. Python pins (`tensorflow`, `numpy`, `scipy`) are tight and fragile — changing them
   tends to break code2vec.

## Testing

- **Backend:** `mvn test`; single test `mvn test -Dtest=ClassName#method`.
  Packaging still uses `-DskipTests` (see `codebases/` below).
  Run `backend/test-summary.py` after `mvn test` for a per-case pass/skip/fail list with
  reasons; surefire's console output names a parameterized case only by index.
  Two kinds of suite, which differ in what they need:
  - **Offline unit tests** — `<pkg>/operation/` (the five decomposition edit operations,
    their undo/redo and history bookkeeping). No Spring context, no Mongo, no Docker:
    `mvn test -Dtest='*PartitionsOperationTest,OperationHistoryBookkeepingTest,OperationDtoTest'`.
    Possible because a hand-built `PartitionsDecomposition` has an empty
    `representationInformations` list, which keeps `AccessesInformation`'s
    `ContextManager` lookup out of reach; `History` is faked for the same reason. See
    `DecompositionFixture` and `InMemoryHistory` in the test tree, and **Gap 7** in
    `../implementation/docs/gap-analysis.md` for the findings they pin.
  - **Stack tests** — `Mono2microApplicationTests` (context-load smoke test) and
    `DecompositionE2ETest`, which drives the real pipeline for every case x strategy
    combination. **Needs the stack up** — `docker compose up -d mongo scripts` — and
    fails, rather than skips, when Mongo or the scripts service is unreachable.
- **Fixtures** are committed under `backend/src/test/resources/representations/`, one
  folder per case. No case holds every fixture, so combinations whose files are absent
  **skip** and are listed after the run; a fixture that is present but broken fails.
  Read that folder's `README.md` before adding a case — `_author.json` carries commit
  authors' email addresses.
- **Go tool:** unit and integration tests are separated by **build tags**
  (`-tags=unit`, `-tags=integration`), not by file location. There are currently no
  `_test.go` files, so `make test` passes vacuously.
- **Frontend:** `npm test` (react-scripts, jsdom). The single CRA default test currently
  fails: Jest cannot parse `vis-network/standalone` (ESM) because `package.json` sets no
  `transformIgnorePatterns`.

## CI

`.github/workflows/ci.yml`, two jobs:

- **`fast`** — every push and PR, no Docker. (A push to a branch that already has an
  open PR skips it, since the `pull_request` run covers the same commit; a small `dedup`
  job decides.) Backend `mvn -DskipTests clean install`,
  frontend install/test/build, Go `go build`. The frontend test and `go vet` run
  **advisory** (`|| true`) because both already fail on pre-existing problems unrelated
  to any change; fix those, then drop the `|| true`.
- **`e2e`** — PRs, nightly (03:00 UTC), and `workflow_dispatch`; not on plain pushes,
  because it builds the ~8GB scripts image. Brings up mongo + scripts via
  `docker compose`, runs `mvn test`, then `test-summary.py`, and uploads the surefire and
  JaCoCo reports as artifacts.

Two things CI must do that a local checkout does not need:

1. `cp specific.properties.example specific.properties` — it is gitignored and read at
   static-init time, so without it every test fails to load the Spring context.
2. `mkdir -p scripts/models/java-large-release` — `code2vec/controller.py` runs
   `Config(verify=True)` at import, which raises `"Model load dir ... does not exist"` and
   crash-loops the scripts container. The directory only has to exist: the model is lazily
   loaded and the backend reads precomputed `codeVector` arrays from the fixture JSON
   instead of calling `/code2vec/predict`.

## Backend layout

Package root: `backend/src/main/java/pt/ist/socialsoftware/mono2micro/` — referred to
below as `<pkg>/`. Organized **by domain concept**, each repeating the same Spring
layering:

```
HTTP → controller/ → service/ → repository/ → MongoDB
                        ↓
                    domain/  (the real objects)     dto/  (API-shaped)
```

Conventions: `*Controller` routes with no logic · `*Service` does the work ·
`*Repository` is usually a bodyless Spring interface · `*Factory` picks an
implementation by type string · `domain/` holds persisted objects · `dto/` holds their
API shapes.

Large artifacts (similarity matrices, dendrogram PNGs, recommendation results) go to
**GridFS** via `<pkg>/fileManager/GridFsService.java`. Domain objects are not Spring
beans and reach services through the static `<pkg>/fileManager/ContextManager.java`.

### Domain model

```
Codebase
├── Representation*        (imported collector files)
└── Strategy*              (a decomposition approach)
    ├── Similarity*  ──►  Dendrogram (built by SciPy)
    ├── Recommendation*
    └── Decomposition*
        ├── Cluster / Partition ──► Element (DomainEntity)
        ├── RepresentationInformation*
        ├── Operation*  (undo/redo via History)
        └── metrics: Map<String, Object>
```

| Concept | Location | Notes |
|---|---|---|
| **Codebase** | `<pkg>/codebase/` | Top-level container; id is its name |
| **Representation** | `<pkg>/representation/domain/` | Abstract + factory; 7 types. Four *groups* at `Representation.java:25-28`: `Accesses Based`, `Repository Based`, `Code Embeddings Based`, `Structure Based` |
| **Strategy** | `<pkg>/strategy/domain/` | **One concrete class**, 7 type strings (`Strategy.java:35-41`). No `StrategyFactory`. Two static maps bind strategies to required representations |
| **Similarity** | `<pkg>/similarity/domain/` | Abstract; `SimilarityScipy` mid-layer; 6 concrete leaves |
| **Weights** | `<pkg>/similarity/domain/similarityMatrix/weights/` | Abstract + factory, 5 subclasses. Each fills a slice of a 3-D matrix, flattened as `metric += matrix[i][j][k] * weights[k] / 100` |
| **Clustering** | `<pkg>/clusteringAlgorithm/` | Abstract, 3 methods. Only `SciPyClustering` registered. `Expert.java` handles expert-supplied decompositions |
| **Decomposition** | `<pkg>/decomposition/domain/` | Abstract; one subclass `PartitionsDecomposition`. `metrics` is a `Map<String,Object>` keyed by metric type |
| **Cluster / Element** | `<pkg>/cluster/`, `<pkg>/element/` | `Partition` adds `couplingDependencies` — the cross-cluster reaches that become distributed transactions. `DomainEntity` is the only `Element` |
| **RepresentationInformation** | `<pkg>/decomposition/domain/representationInformation/` | Per-group decomposition state **and the metric registry** |
| **Metrics** | `<pkg>/metrics/` | `decompositionMetrics/` (Cohesion, Complexity, Coupling, Performance, TSR), `functionalityMetrics/`, `functionalityRedesignMetrics/` |
| **Operation** | `<pkg>/operation/` | merge, split, rename, transfer, formCluster; undo/redo via `<pkg>/history/` |
| **comparisonTool** | `<pkg>/comparisonTool/` | MoJo (`<pkg>/utils/mojoCalculator/`) and Purity |

## Extension points

Every extensible axis is a `*Factory` dispatching on a type string:
`RepresentationFactory`, `SimilarityFactory` / `SimilarityScipyFactory`,
`ClusteringFactory`, `RecommendationFactory`, `WeightsFactory`,
`RepresentationInformationFactory`.

The Python side documents its own seam in `scripts/main.py:20`:

```python
# To add different algorithms, create a new router and include it here
```

### New clustering algorithm

1. Subclass `<pkg>/clusteringAlgorithm/Clustering.java` — implement
   `generateDecomposition`, `getType`, `getAlgorithmSupportedStrategyTypes`.
2. Register in `ClusteringFactory` — **both** the `switch` **and** the
   `algorithmTypes` list. The frontend reads that list via
   `GET /mono2micro/clustering/{algorithmType}/getAlgorithmSupportedStrategyTypes`.
3. New numerics: add a router in `scripts/`, `include_router` it in `scripts/main.py`,
   call it over `WebClient` the way `SciPyClustering` does.

`getAlgorithmSupportedStrategyTypes()` is how "not every algorithm supports every
strategy" is expressed — the code-level counterpart of the feature model's cross-tree
constraints.

### New metric

1. Subclass the appropriate `*MetricCalculator`.
2. Add it to the list returned by the relevant
   `RepresentationInformation.getDecompositionMetrics()` — `AccessesInformation`,
   `RepositoryInformation`, or `StructureInformation`.

`PartitionsDecomposition.calculateMetrics()` then collects it automatically. Because
metrics live in a `Map<String,Object>`, no schema change is needed — but the frontend
must know the key to display it.

### New representation

New subclass in `<pkg>/representation/domain/` with a type constant → case in
`RepresentationFactory` → entry in `Representation.representationGroupToRepresentations`
→ matching `RepresentationInformation` subclass + factory case → frontend
`models/representation/files/` + `RepresentationFactory.ts`.

### New similarity generator — budget for ~9 files

The type string is duplicated across five registries and mirrored in TypeScript:

1. `<pkg>/similarity/domain/SimilarityScipyXxx.java` — subclass `SimilarityScipy`
2. `<pkg>/similarity/domain/SimilarityFactory.java` — case
3. `<pkg>/similarity/domain/SimilarityScipyFactory.java` — case
4. `<pkg>/similarity/dto/SimilarityScipyXxxDto.java` + `@JsonSubTypes` in `SimilarityDto.java`
5. `<pkg>/recommendation/domain/RecommendationsType.java` + `RecommendationFactory`
6. `<pkg>/strategy/domain/Strategy.java` — constant + entries in **both** static maps
7. `<pkg>/clusteringAlgorithm/SciPyClustering.java` — add to `getAlgorithmSupportedStrategyTypes()`
8. If new weight dimensions: `.../weights/XxxWeights.java` + `@JsonSubTypes` in
   `Weights.java` + `WeightsFactory` case
9. Frontend: `models/similarity/` + `SimilarityFactory.ts`, `StrategyTypes.ts`,
   `RecommendationTypes.ts` + `RecommendationFactory.ts`, `models/weights/` +
   `WeightsFactory.ts`, a form under `components/similarity/forms/`

Miss one and the variant fails at runtime rather than compile time. This fan-out is a
known finding, not an accident of your change — see
`../implementation/docs/gap-analysis.md`.

### New view

New folder under `frontend/src/components/view/` alongside `accessesViews/`,
`repositoryView/`, `structureView/`, backed by the `getEdgeWeights` / `getSearchItems`
endpoints that delegate to the matching `RepresentationInformation`.

## Frontend

React 17 + TypeScript, `vis-network` for graphs, MUI + react-bootstrap. `src/models/`
mirrors the backend domain with parallel TS factories, which is why backend changes
propagate here. API layer in `src/services/APIService.ts`.

Backend URLs are **hardcoded** in `src/constants/constants.js`:

```js
export const URL = "http://localhost:8080/mono2micro/";
export const REFACTORIZATION_TOOL_URL = "http://localhost:5001/api/v1/";
```

## Collectors

Eight independent projects under `collectors/`, each with its own build — no shared
build system. Read the collector's own README before building it.

| Collector | Build |
|---|---|
| `spoon-callgraph`, `structure-collector`, `code2vec-callgraph`, `codeql-collector` | Maven (`mvn compile exec:java`, GUI/prompt driven) |
| `commit-collection` | Python (poetry + requirements.txt) |
| `dynamic-collection` | AspectJ |
| `java-callgraph` | Python scripts (ECSA2019 artifact) |
| `eclipse-plugin-callgraph` | Eclipse plugin |

`codeql-collector` is the newest and most active — Django, Rails, SpringDataJPA,
FenixFramework.

## Conventions

- Default branch is `master`. The fork tracks
  `git@github.com:ana-mc-almeida/mono2micro.git`.
- `codebases/` is a runtime volume — keep it empty for backend packaging.
- Adding a variant means registering it in **every** factory that knows the concept;
  grep for an existing type string to find them all.
- When a change relates to a feature-model axis, use the model's vocabulary in
  commits and comments, and note the outcome in `../implementation/docs/decisions.md`.
