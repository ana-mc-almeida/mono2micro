# Shortcuts for the test commands that are awkward to type. `mvn test` is
# deliberately NOT wrapped: it already does the right thing (offline unit tests,
# ~2s) and is the one you run while writing code.
#
# The stack targets need mongo + scripts up:
#     docker compose up -d mongo scripts

# Same values the e2e CI job sets. Override on the command line if your stack
# runs elsewhere, e.g. `make coverage MONGO_DB=mongodb://otherhost:27017`.
MONGO_DB ?= mongodb://localhost:27017
MONGO_DB_NAME ?= mono2micro
SCRIPTS_ADDRESS ?= http://localhost:5002

STACK_ENV = MONGO_DB=$(MONGO_DB) \
	MONGO_DB_NAME=$(MONGO_DB_NAME) \
	SCRIPTS_ADDRESS=$(SCRIPTS_ADDRESS)

# -Dexcluded.groups= (empty) clears the default <excludedGroups>integration so
# the tagged stack tests run alongside the offline unit tests in one invocation.
# Not -Dgroups=integration: that runs ONLY the tagged ones and drops the unit
# suite from the reports test-summary.py aggregates.
FULL_SUITE = -Dexcluded.groups=

# Formatting. Tabs, 160 columns, per backend/eclipse-formatter.xml — which VS
# Code reads too (see ../.vscode/settings.json), so format-on-save and these
# targets cannot disagree.
#
# format-check is the CI gate: the `format` job in ci.yml runs it, and both
# `fast` and `e2e` declare `needs: format`, so unformatted code never reaches a
# test run. It exits non-zero on any violation — indentation, line width,
# trailing whitespace, a missing final newline.
format-check:
	mvn -B spotless:check

# The fix. Idempotent: a second run is a no-op.
format-apply:
	mvn -B spotless:apply

# Import hygiene, reported and never rewritten. Exits 0 even with findings, so
# it can never block you — read the output. Deliberately not part of
# format-check: Spotless can only fix, and imports are a judgement call (the
# *Factory classes' static wildcards are intentional and are not reported).
#
# `checkstyle:checkstyle`, NOT `checkstyle:check`. The `check` goal only counts
# violations at or above <violationSeverity> (error), and checkstyle-imports.xml
# emits everything at `warning` — so `check` cheerfully prints
# "You have 0 Checkstyle violations." while the report holds 97. The
# report-generating goal is the one that tells the truth.
lint-imports:
	mvn -B checkstyle:checkstyle
	@python3 import-summary.py

# The full suite, then the gate — the same two steps CI runs as its `e2e` and
# `coverage` jobs. Split so a coverage shortfall cannot be mistaken for a test
# failure: if this stops at the second command, the tests passed.
# Expected to end in BUILD FAILURE until coverage reaches the limits in pom.xml
# (line 50%, instruction 50%, branch 40%). That is by design; the fix is to
# raise coverage, not to lower the limits.
coverage:
	$(STACK_ENV) mvn -B $(FULL_SUITE) clean test
	$(MAKE) coverage-gate

# The gate alone, replayed against the exec file the last run left in target/.
# Runs no tests, so it is instant and needs no stack.
#
# The @jacoco-check suffix is load-bearing: a bare `mvn jacoco:check` creates a
# `default-cli` execution that does NOT inherit <rules> from the named execution
# in the coverage profile, and fails with "The parameters 'rules' for goal
# jacoco:check are missing or invalid".
coverage-gate:
	mvn -B -Pcoverage jacoco:check@jacoco-check

# The full suite without the gate, for when you want the tests to be the only
# thing that can fail.
test-all:
	$(STACK_ENV) mvn -B $(FULL_SUITE) clean test

# Per-case pass/skip/fail with reasons; surefire names a parameterized case only
# by index. Run after any of the test targets.
test-summary:
	python3 test-summary.py

# Coverage percentages and the worst packages, as Markdown. Reports only, never
# gates — pom.xml is authoritative for the thresholds.
coverage-summary:
	python3 coverage-summary.py

# Both summaries after a full gated run, whether or not the gate fired.
coverage-report:
	-$(MAKE) coverage
	$(MAKE) test-summary
	$(MAKE) coverage-summary
