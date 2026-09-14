# Test fixtures — representation files

`DecompositionE2ETest` drives the real decomposition pipeline, so it needs real
collector output. Those files are **not committed to this repository**; download
them and place them here.

## Download

> **TODO:** publish the archive and put the link here, in the style of the
> "Experimentation Data" section of the project `README.md`.

## Expected layout

One folder per case, named after the codebase. Filenames must match exactly —
the test derives them from the folder name:

```
backend/src/test/resources/representations/
└── <case-name>/
    ├── <case-name>_IDToEntity.json
    └── <case-name>_accesses.json
```

The reference case is `quizzes-tutor`:

```
representations/quizzes-tutor/
├── quizzes-tutor_IDToEntity.json     ~236 B   13 entities
└── quizzes-tutor_accesses.json       ~129 KB  47 functionalities
```

Only these two files are needed for the Accesses strategy — the group
`Accesses Based` requires exactly `IDToEntity` and `Accesses`
(`Representation.java:26,31`). Other collector outputs (author, commit,
code embeddings, entityToID) belong to strategies that are not yet covered.

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

`mvn test` runs `DecompositionE2ETest`, which **fails loudly** when these files
are missing — it never skips, because a skipped test that reports green is the
exact failure this suite exists to prevent. The same applies to a plain
`mvn clean install`.

Packaging is unaffected: the project `README.md` already uses
`mvn clean install -DskipTests`.

The test also needs the stack running:

```bash
docker compose up -d mongo scripts
```
