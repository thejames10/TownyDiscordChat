#!/usr/bin/env python3
"""Fail CI when Minecraft packaging, plugin metadata or Markdown guides drift."""
from __future__ import annotations

import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
README = ROOT / "README.md"
WORKFLOW = ROOT / ".github/workflows/compatibility.yml"
POM = ROOT / "pom.xml"
PLUGIN_YML = ROOT / "src/main/resources/plugin.yml"
CONFIG = ROOT / "src/main/resources/config.yml"
GUIDES = [README, ROOT / "COMPATIBILITY.md", ROOT / "SYNC-MIGRATION.md",
          ROOT / "TEST-PLAN.md"]
errors = []


def require(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)


def read(path: Path) -> str:
    if not path.is_file():
        errors.append(f"Required file missing: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


docs = {p: read(p) for p in GUIDES}
readme = docs[README]
workflow = read(WORKFLOW)
pom = read(POM)
plugin = read(PLUGIN_YML)
config = read(CONFIG)

require("\n## Compatibility\n" in readme, "README: missing proper '## Compatibility' heading")
require("\n## Installation\n" in readme, "README: missing Installation heading")
compat_section = readme.split("\n## Compatibility\n", 1)[-1].split("\n## Installation\n", 1)[0]

matrix_section = workflow.split("\n  build:\n", 1)[-1].split("\n  legacy-api-check:\n", 1)[0]
matrix = re.findall(
    r"minecraft:\s*'([0-9.]+)'\s*,\s*towny:\s*'([0-9.]+)'",
    matrix_section,
)
require(len(matrix) == 10 and len(set(mc for mc, _ in matrix)) == len(matrix),
        "CI: expected exactly 10 unique Minecraft-version builds")

readme_rows = [
    line for line in compat_section.splitlines()
    if line.strip().startswith("|") and line.count("|") >= 5
    and "Minecraft version" not in line and "---" not in line
]
for mc, towny in matrix:
    matching = [
        row for row in readme_rows
        if f"`{mc}`" in row.split("|")[1]
    ]
    require(len(matching) == 1,
            f"README compatibility matrix must contain Minecraft {mc} in exactly one row")
    if len(matching) == 1:
        require(f"`{towny}`" in matching[0].split("|")[3],
                f"README Minecraft {mc} Towny version differs from CI ({towny})")
        require("`1.30.5`" in matching[0].split("|")[4],
                f"README Minecraft {mc} DiscordSRV differs from CI (1.30.5)")

build_dep = re.search(r"-Ddiscordsrv\.version=([0-9.]+)", matrix_section)
require(build_dep is not None and build_dep.group(1) == "1.30.5",
        "CI: user-facing builds must use documented DiscordSRV 1.30.5")
for key, expected in [
    ("spigot.version", "1.20.1-R0.1-SNAPSHOT"),
    ("towny.version", "0.103.2.0"),
    ("discordsrv.version", "1.30.5"),
]:
    actual = re.search(rf"<{re.escape(key)}>([^<]+)</{re.escape(key)}>", pom)
    require(actual is not None and actual.group(1) == expected,
            f"pom.xml {key} should be {expected}, matching current development default")

pom_version = re.search(
    r"<artifactId>TownyDiscordChat</artifactId>\s*<version>([^<]+)</version>", pom
)
require(pom_version is not None, "pom.xml: project version not found")
if pom_version:
    require(pom_version.group(1) in compat_section,
            "README development version must match the Maven project version")
require("version: '${project.version}'" in plugin,
        "plugin.yml must obtain its version from Maven resource filtering")

for item in ("storage:", "  type: auto", "  migrateToMySQL: false",
             "queue:", "  deletionGraceSeconds:", "onboarding:",
             "  enabled: true", "security:"):
    require(item in config, f"config.yml is missing expected default: {item}")
require("sqlite-jdbc" in pom and "mysql-connector-java" in pom,
        "pom.xml must bundle both SQLite and MySQL JDBC drivers")
for doc in GUIDES:
    content = docs[doc]
    require(r"\n" not in content,
            f"{doc.name} contains literal backslash-n sequences")
    require(not re.search(r"/actions/runs/\d+/artifacts/\d+", content),
            f"{doc.name} has an expiring, hard-coded artifact download link")
    for target in re.findall(r"\]\(([^)]+)\)", content):
        if target.startswith(("https://", "http://", "#", "mailto:", "app://")):
            continue
        local = target.split("#", 1)[0]
        if local:
            require((doc.parent / local).is_file(),
                    f"{doc.name} has a broken local Markdown link: {target}")

for doc in (README, ROOT / "COMPATIBILITY.md", ROOT / "SYNC-MIGRATION.md",
            ROOT / "TEST-PLAN.md"):
    require("config.yml.bak" in docs[doc],
            f"{doc.name} must describe the optional configuration backup upgrade")
require((ROOT / "src/main/java/com/TownyDiscordChat/TownyDiscordChat/ConfigUpgrade.java").is_file(),
        "ConfigUpgrade.java is required for README documented first-start backup import")

for name in ("README.md", "COMPATIBILITY.md", "SYNC-MIGRATION.md",
             "TEST-PLAN.md", "config.example.yml", "BUILD-INFO.txt"):
    require(name in workflow, f"CI artifact bundle is missing {name}")

# Dormant 2023 scheduler/listener/task graph must not silently reappear
# and bypass our desired-state queue.
javaRoot = ROOT / "src/main/java/com/TownyDiscordChat/TownyDiscordChat"
retiredDirectories = [
    javaRoot / "Tasks",
    javaRoot / "Schedulers" / "Categories",
    javaRoot / "Schedulers" / "Roles",
    javaRoot / "Schedulers" / "Channels",
    javaRoot / "Schedulers" / "Log",
    javaRoot / "Schedulers" / "Players",
]
for directory in retiredDirectories:
    require(not any(directory.rglob("*.java")) if directory.is_dir() else True,
            f"Dormant Java scheduler/task package was restored: {directory.relative_to(ROOT)}")
for filename in ("Listeners/TDCTownyListener.java",
                 "Listeners/TDCDiscordSRVListener.java",
                 "MySQL/MySQL.java",
                 "MySQL/Tables/QueuedTask.java"):
    require(not (javaRoot / filename).exists(),
            f"Retired executable file was restored: {filename}")

allowedMutators = {
    javaRoot / "Schedulers" / "Queue" / "Queue.java",
    javaRoot / "Core" / "TDCManager.java",
}
for file in javaRoot.rglob("*.java"):
    if file in allowedMutators:
        continue
    content=file.read_text(encoding="utf-8")
    for forbidden in ("addRoleToMember(", "removeRoleFromMember(",
                      ".createRole()", ".createTextChannel(",
                      ".createVoiceChannel(", ".createCategory("):
        require(forbidden not in content,
                f"{file.relative_to(ROOT)} issues Discord REST mutations outside the queue")

# Git tags provide permanent, version-specific documents. Fail fast if a
# release tag was cut while the checked-in README still says SNAPSHOT.
ref = os.environ.get("GITHUB_REF", "")
if ref.startswith("refs/tags/v"):
    tagged_version = ref[len("refs/tags/v"):]
    require(
        f"TownyDiscordChat `{tagged_version}`" in compat_section,
        f"Release tag {tagged_version} does not match the README compatibility version; "
        "update README.md and pom.xml before tagging",
    )
    require(pom_version is not None and pom_version.group(1) == tagged_version,
            f"Release tag {tagged_version} must match pom.xml project version")

if errors:
    print("Repository consistency audit FAILED:")
    for error in errors:
        print("  -", error)
    sys.exit(1)
print(f"Repository consistency audit passed: {len(matrix)} CI versions and "
      f"{len(GUIDES)} Markdown guides checked.")
