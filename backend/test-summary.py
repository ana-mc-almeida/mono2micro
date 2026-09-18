#!/usr/bin/env python3
"""Readable summary of the last `mvn test` run.

Surefire's console output names a failing parameterized case only by index
("[7]"); the phrased display name and the skip reason exist solely in the XML
reports. This prints them.

Usage:  mvn test ; ./test-summary.py
"""
import glob
import sys
import xml.etree.ElementTree as ET

GREEN, RED, YELLOW, DIM, RESET = "\033[32m", "\033[31m", "\033[33m", "\033[2m", "\033[0m"
if not sys.stdout.isatty():
    GREEN = RED = YELLOW = DIM = RESET = ""

reports = sorted(glob.glob("target/surefire-reports/TEST-*.xml"))
if not reports:
    sys.exit("No reports in target/surefire-reports — run `mvn test` first.")

passed = failed = skipped = 0
for path in reports:
    suite = ET.parse(path).getroot()
    print(f"\n{suite.get('name')}")
    for case in suite.findall("testcase"):
        name = case.get("name")
        failure = case.find("failure")
        if failure is None:
            failure = case.find("error")
        skip = case.find("skipped")
        if failure is not None:
            failed += 1
            print(f"  {RED}FAIL{RESET}  {name}")
            print(f"        {DIM}{(failure.get('message') or '').strip()}{RESET}")
        elif skip is not None:
            skipped += 1
            print(f"  {YELLOW}SKIP{RESET}  {name}")
            # The assumption message says *why* — the console never shows it.
            print(f"        {DIM}{(skip.get('message') or 'no reason given').strip()}{RESET}")
        else:
            passed += 1
            print(f"  {GREEN}ok{RESET}    {name}")

print(f"\n{GREEN}{passed} passed{RESET}, {RED}{failed} failed{RESET}, {YELLOW}{skipped} skipped{RESET}")
sys.exit(1 if failed else 0)
