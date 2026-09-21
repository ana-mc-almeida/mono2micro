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

# The full suite plus the jacoco:check gate — what the e2e CI job runs.
# Expected to end in BUILD FAILURE until coverage reaches the limits in pom.xml
# (line 50%, instruction 50%, branch 40%). That is by design; the fix is to
# raise coverage, not to lower the limits.
coverage:
	$(STACK_ENV) mvn -B -Pcoverage $(FULL_SUITE) clean test

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
