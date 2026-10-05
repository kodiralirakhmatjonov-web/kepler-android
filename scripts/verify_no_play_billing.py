#!/usr/bin/env python3
"""Hard release guard: Google Play Billing must be absent from source and AAB."""
from __future__ import annotations

import argparse
import pathlib
import re
import sys
import zipfile

TEXT_PATTERNS = [
    re.compile(rb"com[./]android[./]billingclient", re.I),
    re.compile(rb"com[./]android[./]vending[./]billing", re.I),
    re.compile(rb"com\.android\.vending\.BILLING", re.I),
    re.compile(rb"\bBillingClient(StateListener)?\b"),
    re.compile(rb"\bPurchasesUpdatedListener\b"),
    re.compile(rb"\bProductDetails\b"),
    re.compile(rb"\bPurchaseManager\b"),
    re.compile(rb"\bBillingManager\b"),
    re.compile(rb"\bQueryProductDetailsParams\b"),
    re.compile(rb"\bqueryProductDetailsAsync\b"),
    re.compile(rb"\bqueryPurchasesAsync\b"),
    re.compile(rb"\blaunchBillingFlow\b"),
    re.compile(rb"\backnowledgePurchase\b"),
    re.compile(rb"\bconsumeAsync\b"),
    re.compile(rb"\bpaywall\b", re.I),
    re.compile(rb"\bpurchaseToken\b"),
    re.compile(rb"\brestorePurchases\b"),
    re.compile(rb"restore[ _-]+purchases", re.I),
    re.compile(rb"in[ -]app purchases?", re.I),
    re.compile(rb"\bIAP\b"),
]

SOURCE_ROOTS = (
    "app/src/main",
    "buildSrc",
    "build-logic",
    "gradle",
)
ROOT_BUILD_NAMES = {
    "build.gradle",
    "build.gradle.kts",
    "settings.gradle",
    "settings.gradle.kts",
    "libs.versions.toml",
}
SKIP_SUFFIXES = {
    ".png", ".jpg", ".jpeg", ".webp", ".gif", ".mp4", ".mov", ".zip", ".gz", ".jar", ".aar", ".so",
}
BUILD_COORD_PATTERNS = [
    re.compile(rb"com\.android\.billingclient\s*[:/]", re.I),
    re.compile(rb"billing-ktx", re.I),
    re.compile(rb"module\s*=\s*[\"\']com\.android\.billingclient:", re.I),
    re.compile(rb"group\s*=\s*[\"\']com\.android\.billingclient[\"\']", re.I),
]

AAB_PATTERNS = [
    b"com/android/billingclient",
    b"com.android.billingclient",
    b"com/android/vending/billing",
    b"com.android.vending.BILLING",
    b"Lcom/android/billingclient/",
]


def scan_bytes(label: str, data: bytes, patterns=TEXT_PATTERNS) -> list[str]:
    return [f"{label}: matched {pattern.pattern.decode('ascii', 'replace')}" for pattern in patterns if pattern.search(data)]


def source_files(root: pathlib.Path):
    # All module/root Gradle declarations, settings, version catalogs and active app source.
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        rel = path.relative_to(root)
        rel_s = rel.as_posix()
        if any(part in {".git", ".gradle", "build", "artifacts"} for part in rel.parts):
            continue
        if path.suffix.lower() in SKIP_SUFFIXES:
            continue
        is_build_decl = path.name in ROOT_BUILD_NAMES or path.name.endswith(".gradle") or path.name.endswith(".gradle.kts")
        is_active_source = any(rel_s == base or rel_s.startswith(base + "/") for base in SOURCE_ROOTS)
        if is_build_decl or is_active_source:
            yield path


def verify_source(root: pathlib.Path) -> None:
    failures: list[str] = []
    forbidden_paths = [
        root / "app/src/main/java/com/iumrah/beta/core/billing",
        root / "app/src/main/java/com/iumrah/beta/core/BillingManager.kt",
        root / "app/src/main/java/com/iumrah/beta/core/PurchaseManager.kt",
    ]
    for path in forbidden_paths:
        if path.exists():
            failures.append(f"legacy Billing path still exists: {path.relative_to(root)}")

    for path in source_files(root):
        try:
            data = path.read_bytes()
        except OSError as exc:
            failures.append(f"cannot read {path}: {exc}")
            continue
        rel_label = path.relative_to(root).as_posix()
        is_build_decl = path.name in ROOT_BUILD_NAMES or path.name.endswith(".gradle") or path.name.endswith(".gradle.kts")
        if is_build_decl:
            # Guard code is allowed to mention the forbidden group while checking it;
            # only dependency/catalog declaration forms are forbidden here.
            for line_no, line in enumerate(data.splitlines(), 1):
                if b"module.group" in line and b"com.android.billingclient" in line:
                    continue
                for pattern in BUILD_COORD_PATTERNS:
                    if pattern.search(line):
                        failures.append(f"{rel_label}:{line_no}: forbidden Billing dependency declaration")
        else:
            failures.extend(scan_bytes(rel_label, data))

    if failures:
        print("Google Play Billing source audit FAILED:", file=sys.stderr)
        for failure in failures:
            print(f" - {failure}", file=sys.stderr)
        raise SystemExit(1)
    print("Verified source: no Google Play Billing SDK/API/declarations in active production source.")


def verify_aab(aab: pathlib.Path) -> None:
    failures: list[str] = []
    with zipfile.ZipFile(aab) as archive:
        for info in archive.infolist():
            lower_name = info.filename.lower()
            if "billingclient" in lower_name or "com/android/billing" in lower_name:
                failures.append(f"forbidden AAB entry name: {info.filename}")
            if info.is_dir():
                continue
            # Read every compressed entry: this catches classes in DEX/JAR/protobuf manifests/resources.
            data = archive.read(info)
            for needle in AAB_PATTERNS:
                if needle.lower() in data.lower():
                    failures.append(f"{info.filename}: contains {needle.decode('ascii', 'replace')}")
    if failures:
        print("Google Play Billing AAB audit FAILED:", file=sys.stderr)
        for failure in sorted(set(failures)):
            print(f" - {failure}", file=sys.stderr)
        raise SystemExit(1)
    print(f"Verified AAB: {aab.name} contains no Google Play Billing SDK markers.")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=pathlib.Path)
    parser.add_argument("--aab", type=pathlib.Path)
    args = parser.parse_args()
    if not args.source and not args.aab:
        parser.error("pass --source and/or --aab")
    if args.source:
        verify_source(args.source.resolve())
    if args.aab:
        verify_aab(args.aab.resolve())


if __name__ == "__main__":
    main()
