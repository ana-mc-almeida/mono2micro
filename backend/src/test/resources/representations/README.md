# Test fixtures — representation files

`DecompositionE2ETest` drives the real decomposition pipeline, so it needs real
collector output. Those files are **not committed to this repository**; download
them and place them here.

## Download

> **TODO:** publish the archive and put the link here, in the style of the
> "Experimentation Data" section of the project `README.md`.

## Expected layout

One folder per case, named after the codebase. Filenames must match exactly —
the test derives them from the folder name. **The casing is inconsistent and
that is deliberate**: `_IDToEntity.json` capitalizes the leading I,
`_entityToID.json` does not. These are collector filenames.

```
backend/src/test/resources/representations/
└── <case-name>/
    ├── <case-name>_IDToEntity.json
    ├── <case-name>_entityToID.json
    ├── <case-name>_accesses.json
    ├── <case-name>_author.json
    ├── <case-name>_commit.json
    └── <case-name>_code_embeddings.json
```

The reference case is `quizzes-tutor`:

```
representations/quizzes-tutor/
├── quizzes-tutor_IDToEntity.json       ~236 B   13 entities
├── quizzes-tutor_entityToID.json       ~210 B   the inverse map
├── quizzes-tutor_accesses.json         ~129 KB  47 functionalities
├── quizzes-tutor_author.json           ~7 KB    contains real email addresses
├── quizzes-tutor_commit.json           ~81 KB   co-change counts
└── quizzes-tutor_code_embeddings.json  ~4.2 MB  856 methods, 384-float vectors
```

`quizzes-tutor_commit_mixed.json` is also in the reference set but maps to no
known representation type, so nothing reads it.

## Which files each strategy needs

A strategy uploads its whole representation *group* in one request — a second
upload of a type the codebase already holds is rejected with "Re-sending
representations is not allowed."

| Strategy | Group | Files |
|---|---|---|
| Accesses | `Accesses Based` | IDToEntity, accesses |
| Repository | `Repository Based` | IDToEntity, accesses, author, commit |
| Class Vectorization | `Code Embeddings Based` | IDToEntity, entityToID, accesses, code_embeddings |
| Entity Vectorization | `Code Embeddings Based` | same as above |
| Functionality Vectorization Call Graph | `Code Embeddings Based` | same as above |
| Functionality Vectorization Sequence Accesses | `Code Embeddings Based` | same as above |
| Structure | `Structure Based` | entityToID, IDToEntity, accesses, **structure** |

**The code embeddings strategies do not need the code2vec model.** The backend
never calls `/code2vec/predict` — it reads the pre-computed `codeVector` arrays
out of the uploaded JSON and does the vectorization in Java. The model is only
needed upstream, to regenerate that file with the code2vec collector.

## A case need not cover every strategy

`DecompositionE2ETest` runs every case against all seven strategies, and **skips**
any combination whose fixtures are absent, naming the missing files. No case
currently holds all of them:

| Case | Covers | Missing |
|---|---|---|
| `quizzes-tutor` | everything but Structure | `_structure.json` |
| `quizzes-tutor-structure` | Accesses, Structure | `_author.json`, `_commit.json`, `_code_embeddings.json` |
| `spring-petclinic` | Accesses, Repository | `_code_embeddings.json`, `_structure.json` |

The skipped combinations are listed on stderr after the run, so a suite that
covered little is not mistaken for one that passed.

The skip is narrow: it triggers only on a file being **absent**. Drop the file in
and the case runs — and fails loudly, like every other case — with no code change.
A file that is present but empty or unreadable still fails rather than skipping.

For structure fixtures, the expected shape is
`{"entities": [{"name": "...", "fields": [...]}]}`, which
`StructureRepresentation.init` parses; see the real examples under
`collectors/codeql-collector/test-resources/endToEnd/*/expected-output/structure.json`.
Do not use `collectors/codeql-collector/data/quizzes-tutor_structure.json` — it is
a 130-byte git-lfs pointer stub and `git lfs` is not installed in this checkout.

## Requirements a fixture must meet

- **At least 2 entities.** SciPy's `linkage()` raises
  `"number of observations cannot be determined on an empty distance matrix"`
  on a single-entity codebase.
- **`accesses.json` must be valid JSON** — it is parsed on upload by
  `AccessesRepresentation.init`. `IDToEntity.json` is stored verbatim.
- **Every entity ID referenced in `accesses.json` must exist as a key in
  `IDToEntity.json`.** The accesses format is nested:
  `{"Controller.method": {"t": [{"id": 0, "a": [["R", 1], ["W", 2]]}]}}`,
  where the second element of each access pair is the entity ID.

Do not use the JSON under `collectors/codeql-collector/data/` — those files are
git-lfs pointers and `git lfs` is not installed in this checkout.

## Why these are not committed

Collector output can carry personal data. In the reference set, the author
representation contains real email addresses; the two files listed above contain
none. Committing is still under discussion, so the whole folder is gitignored
rather than filtered file by file.

## Consequence for the build

`mvn test` runs `DecompositionE2ETest`. With none of these files present every
case skips and the build is green but vacuous, which is why the skipped
combinations are printed after the run — read that list before trusting a pass.

Everything else remains a hard failure: Mongo or the scripts service being down
fails the build, and so does a fixture that is present but empty or unreadable.

Packaging is unaffected: the project `README.md` already uses
`mvn clean install -DskipTests`.

The test also needs the stack running:

```bash
docker compose up -d mongo scripts
```
