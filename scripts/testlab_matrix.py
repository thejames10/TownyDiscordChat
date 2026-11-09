#!/usr/bin/env python3
"""Export the build matrix without maintaining a second, divergent version list.

Standard-library only: read GitHub Actions' compatibility.yml inline matrix.
The 'jdk' field is a BUILD JDK, not proof of Minecraft runtime support.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = REPO_ROOT / ".github" / "workflows" / "compatibility.yml"
REQUIRED = (
    "minecraft", "towny", "townyGroup", "townyArtifact", "apiVersion",
    "apiGroup", "apiArtifact", "apiRelease", "jdk",
)


def read_matrix(path: Path = WORKFLOW) -> list[dict[str, str]]:
    data = path.read_text(encoding="utf-8")
    section = data.split("\n  build:\n", 1)
    if len(section) != 2:
        raise ValueError("Missing build job in compatibility workflow")
    section = section[1].split("\n    steps:\n", 1)[0]
    rows = []
    for line in section.splitlines():
        if not re.match(r"^\s*-\s*\{\s*minecraft:", line):
            continue
        fields = dict(re.findall(r"(\w+):\s*'([^']*)'", line))
        missing = set(REQUIRED) - fields.keys()
        if missing:
            raise ValueError("Incomplete Minecraft matrix entry: " + ", ".join(sorted(missing)))
        rows.append({
            "minecraft": fields["minecraft"],
            "build_jdk": fields["jdk"],
            "towny": fields["towny"],
            "towny_group": fields["townyGroup"],
            "towny_artifact": fields["townyArtifact"],
            "api_version": fields["apiVersion"],
            "api_group": fields["apiGroup"],
            "api_artifact": fields["apiArtifact"],
            "api_release": fields["apiRelease"],
            "adventure": fields.get("adventure", "4.21.0"),
            "discordsrv": "1.30.5",
        })
    versions = [r["minecraft"] for r in rows]
    if len(rows) != 14 or len(set(versions)) != 14:
        raise ValueError(f"Expected 14 distinct Minecraft targets, got {len(rows)}")
    return rows


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", default="all", help="Minecraft version or 'all'")
    parser.add_argument("--check", action="store_true", help="Validate matrix and exit")
    args = parser.parse_args()
    rows = read_matrix()
    if args.version != "all":
        rows = [r for r in rows if r["minecraft"] == args.version]
        if not rows:
            parser.error(f"Unknown Minecraft version: {args.version}")
    if args.check:
        print(f"Validated {len(rows)} Minecraft targets")
    else:
        print(json.dumps(rows, indent=2))


if __name__ == "__main__":
    main()
