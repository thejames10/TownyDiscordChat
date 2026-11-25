#!/usr/bin/env python3
"""Export the supported build matrix directly from the compatibility workflow.

Standard-library only: read GitHub Actions' compatibility.yml inline matrix.
The 'jdk' field indicates the build JDK, not the Minecraft runtime requirement.
Exactly one row has ``release_build`` set: the target whose APIs match the
pom.xml defaults, which builds the single JAR released for every version.
"""
from __future__ import annotations

import argparse
import json
import re
import xml.etree.ElementTree as ElementTree
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = REPO_ROOT / ".github" / "workflows" / "compatibility.yml"
POM = REPO_ROOT / "pom.xml"
POM_NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}
# Exported matrix field -> pom.xml property that the release build uses.
RELEASE_BUILD_PROPERTIES = {
    "api_group": "server.api.groupId",
    "api_artifact": "server.api.artifactId",
    "api_release": "server.api.version",
    "api_version": "bukkit.api.version",
    "adventure": "adventure.version",
    "towny": "towny.version",
    "towny_group": "towny.groupId",
    "towny_artifact": "towny.artifactId",
    "discordsrv": "discordsrv.version",
}
REQUIRED = (
    "minecraft", "towny", "townyGroup", "townyArtifact", "apiVersion",
    "apiGroup", "apiArtifact", "apiRelease", "jdk",
)


def read_pom_properties(path: Path = POM) -> dict[str, str]:
    properties = ElementTree.parse(path).getroot().find("m:properties", POM_NAMESPACE)
    if properties is None:
        raise ValueError("pom.xml has no <properties> block")
    return {element.tag.split("}", 1)[1]: (element.text or "").strip() for element in properties}


def is_release_build(row: dict[str, str], properties: dict[str, str]) -> bool:
    return all(row[field] == properties.get(name)
               for field, name in RELEASE_BUILD_PROPERTIES.items())


def mark_release_build(rows: list[dict], properties: dict[str, str]) -> None:
    for row in rows:
        row["release_build"] = is_release_build(row, properties)
    count = sum(row["release_build"] for row in rows)
    if count != 1:
        raise ValueError(f"Expected one target matching the pom.xml release build, got {count}")


def read_matrix(path: Path = WORKFLOW, pom: Path = POM) -> list[dict]:
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
    mark_release_build(rows, read_pom_properties(pom))
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
