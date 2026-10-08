#!/usr/bin/env python3
"""Print *all* Android lint errors from AGP release reports to GitHub logs.

Do not turn lint off, do not baseline new issues, and do not read credentials.
The builder calls this only when :app:lintRelease exits nonzero.
"""
from __future__ import annotations

from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

root = Path("app/build")
text_reports = sorted(
    root.glob("intermediates/lint_intermediate_text_report/release/**/lint-results-release.txt"),
    key=lambda p: p.stat().st_mtime,
    reverse=True,
)
text_reports += sorted(
    root.glob("reports/lint-results-release.txt"),
    key=lambda p: p.stat().st_mtime,
    reverse=True,
)
errors = []
if text_reports:
    report = text_reports[0]
    print(f"Lint text report: {report}")
    for line in report.read_text(errors="replace").splitlines():
        if re.search(r":\d+: (?:Error|Fatal):", line):
            errors.append(line.strip())
else:
    xml_reports = list(root.glob("reports/lint-results-release.xml"))
    if xml_reports:
        print(f"Lint XML report: {xml_reports[0]}")
        try:
            tree = ET.parse(xml_reports[0])
            for issue in tree.findall(".//issue"):
                if issue.attrib.get("severity", "").lower() not in ("error", "fatal"):
                    continue
                loc = issue.find("location")
                where = f"{loc.get('file')}:{loc.get('line')}" if loc is not None else "unknown"
                errors.append(f"{where}: Error: {issue.get('message')} [{issue.get('id')}]")
        except ET.ParseError as e:
            print(f"Could not parse lint XML report: {e}")

if errors:
    print(f"Full lint report contains {len(errors)} error(s):")
    for idx, e in enumerate(errors, 1):
        print(f"LINT_ERROR_{idx}: {e}")
else:
    print("Lint error report not present or not in expected format.")
    print("Look at the Gradle :app:lintRelease output above.")

# The builder is already failing; this diagnostic script should not hide that.
sys.exit(0)
