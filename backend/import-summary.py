#!/usr/bin/env python3
"""Print Checkstyle's import findings, and emit GitHub annotations under CI.

Sibling of test-summary.py and coverage-summary.py: reports only, never gates.
Reads target/checkstyle-result.xml, which `mvn checkstyle:checkstyle` writes.

Why this exists rather than just reading Maven's console output: the
`checkstyle:check` goal counts only violations at or above <violationSeverity>
(error), while checkstyle-imports.xml deliberately emits everything at
`warning`. So `check` prints "You have 0 Checkstyle violations." and exits 0
while the report file holds every finding. The report is the source of truth.

Always exits 0. Import hygiene is advisory by design -- the gate is
`spotless:check`, which is a separate concern (see backend/Makefile).
"""

import os
import sys
import xml.etree.ElementTree as ET
from collections import Counter

REPORT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "target", "checkstyle-result.xml")

# Repo-relative prefix for GitHub annotations, which resolve from the repo root
# while Checkstyle records absolute paths.
PREFIX = "backend/"


def rel(path):
    """Absolute Checkstyle path -> repo-relative, for clickable output."""
    marker = "/src/"
    return PREFIX + "src/" + path.split(marker, 1)[1] if marker in path else path


def main():
    if not os.path.exists(REPORT):
        print("No checkstyle report at target/checkstyle-result.xml.")
        print("Run `make lint-imports` (or `mvn checkstyle:checkstyle`) first.")
        return 0

    root = ET.parse(REPORT).getroot()
    findings = [
        (rel(f.get("name", "")), e)
        for f in root.findall("file")
        for e in f.findall("error")
    ]

    if not findings:
        print("No import findings.")
        return 0

    kinds = Counter(e.get("source", "").split(".")[-1].replace("Check", "") for _, e in findings)

    print(f"\n{len(findings)} import finding(s): " + ", ".join(f"{v} {k}" for k, v in kinds.most_common()))
    print()

    for path, e in sorted(findings, key=lambda t: (t[0], int(t[1].get("line", 0)))):
        line = e.get("line", "0")
        msg = e.get("message", "")
        print(f"  {path}:{line}  {msg}")

        # Surface findings on the PR's Files-changed tab, not only in the log.
        if os.environ.get("GITHUB_ACTIONS") == "true":
            col = e.get("column")
            loc = f"file={path},line={line}" + (f",col={col}" if col else "")
            print(f"::warning {loc}::{msg}")

    print("\nAdvisory only - nothing here fails a build.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
