#!/usr/bin/env python3
"""Prevent public source and documentation from naming internal test infrastructure."""
from __future__ import annotations

import os
import subprocess
from pathlib import Path

REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
INTERNAL_IDENTIFIER = "test" + "lab"
TEXT_EXTENSIONS = {
    ".java", ".md", ".py", ".yml", ".yaml", ".xml",
    ".txt", ".properties", ".toml", ".gradle", ".json",
}


def tracked_files() -> list[Path]:
    output = subprocess.check_output(
        ["git", "ls-files", "-z"], cwd=REPOSITORY_ROOT
    )
    return [Path(value.decode("utf-8")) for value in output.split(b"\0") if value]


def find_internal_references() -> list[str]:
    violations: list[str] = []
    commit_message = subprocess.check_output(
        ["git", "log", "-1", "--format=%B"], cwd=REPOSITORY_ROOT,
        text=True,
    )
    metadata = {
        "latest commit message": commit_message,
        "pull request title": os.environ.get("PUBLIC_PR_TITLE", ""),
        "pull request description": os.environ.get("PUBLIC_PR_BODY", ""),
    }
    for label, value in metadata.items():
        if INTERNAL_IDENTIFIER in value.casefold():
            violations.append(label)
    for relative_path in tracked_files():
        if INTERNAL_IDENTIFIER in relative_path.as_posix().casefold():
            violations.append(str(relative_path) + ": filename")
            continue
        if relative_path.suffix.lower() not in TEXT_EXTENSIONS:
            continue
        lines = (REPOSITORY_ROOT / relative_path).read_text(
            encoding="utf-8"
        ).splitlines()
        for number, line in enumerate(lines, 1):
            if INTERNAL_IDENTIFIER in line.casefold():
                violations.append(f"{relative_path}:{number}")
    return violations


def main() -> None:
    violations = find_internal_references()
    if violations:
        raise SystemExit(
            "Public files must not reference internal test infrastructure:\n"
            + "\n".join(violations)
        )
    print("Public repository boundary verified")


if __name__ == "__main__":
    main()
