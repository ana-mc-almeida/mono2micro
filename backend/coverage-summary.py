#!/usr/bin/env python3
"""Coverage percentages from the last `mvn test` run, as Markdown.

JaCoCo writes an HTML report nobody sees in CI (it is inside an uploaded zip) and a
CSV nobody reads by hand. This turns the CSV into a table for `$GITHUB_STEP_SUMMARY`,
so the numbers behind a passing or failing build are on the run page itself.

Reporting only. The gate is `jacoco:check` in pom.xml's `coverage` profile, which is
the authoritative copy of the limits below; these are duplicated here purely to mark
the table, and a mismatch changes nothing but a tick mark.

Usage:  mvn test ; ./coverage-summary.py >> "$GITHUB_STEP_SUMMARY"
"""
import csv
import os
import sys
from collections import defaultdict

REPORT = os.path.join("target", "site", "jacoco", "jacoco.csv")

# Mirror of the <limits> in pom.xml's `coverage` profile. Counters absent here are
# reported without a verdict.
THRESHOLDS = {"LINE": 0.50, "INSTRUCTION": 0.50, "BRANCH": 0.40}

COUNTERS = ["LINE", "INSTRUCTION", "BRANCH", "METHOD", "COMPLEXITY"]
WORST_PACKAGES = 10


def totals(rows):
    """Sum <counter>_MISSED / _COVERED across every class in the bundle."""
    acc = {c: [0, 0] for c in COUNTERS}
    for row in rows:
        for counter in COUNTERS:
            acc[counter][0] += int(row[counter + "_MISSED"])
            acc[counter][1] += int(row[counter + "_COVERED"])
    return acc


def worst_packages(rows):
    """Packages with the most uncovered lines - where the next test pays off most."""
    missed = defaultdict(lambda: [0, 0])
    for row in rows:
        pkg = missed[row["PACKAGE"]]
        pkg[0] += int(row["LINE_MISSED"])
        pkg[1] += int(row["LINE_COVERED"])
    ranked = sorted(missed.items(), key=lambda kv: -kv[1][0])
    return ranked[:WORST_PACKAGES]


# `if: always()` runs this after a compile failure too, when no report exists. Say so
# and exit 0: a second red X here would only hide the real error above.
if not os.path.exists(REPORT):
    print(f"No coverage report at `{REPORT}` - nothing to summarise.")
    sys.exit(0)

with open(REPORT) as handle:
    rows = list(csv.DictReader(handle))

acc = totals(rows)

print("## Coverage")
print()
print("| Counter | Covered | Total | % | Gate |")
print("| --- | ---: | ---: | ---: | :---: |")
for counter in COUNTERS:
    miss, cov = acc[counter]
    total = miss + cov
    if not total:
        continue
    ratio = cov / total
    limit = THRESHOLDS.get(counter)
    if limit is None:
        verdict = "-"
    else:
        verdict = "OK" if ratio >= limit else f"FAIL (< {limit:.0%})"
    print(f"| {counter.title()} | {cov:,} | {total:,} | {ratio:.1%} | {verdict} |")

print()
print(f"<details><summary>Top {WORST_PACKAGES} packages by uncovered lines</summary>")
print()
print("| Package | Uncovered | Line % |")
print("| --- | ---: | ---: |")
for pkg, (miss, cov) in worst_packages(rows):
    total = miss + cov
    ratio = f"{cov / total:.1%}" if total else "n/a"
    print(f"| `{pkg}` | {miss:,} | {ratio} |")
print()
print("</details>")
