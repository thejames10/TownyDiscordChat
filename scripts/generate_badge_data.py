#!/usr/bin/env python3
"""Write shields.io endpoint badge files for release downloads and contributors.

The README reads these files instead of the live shields.io GitHub badges,
which intermittently render "invalid" when shields.io exhausts its shared
GitHub API token pool.
"""
from __future__ import annotations

import argparse
import json
import os
import urllib.request
from pathlib import Path
from typing import Any, Iterator

GITHUB_API_ROOT = "https://api.github.com"
DEFAULT_REPOSITORY = "thejames10/TownyDiscordChat"
PAGE_SIZE = 100
REQUEST_TIMEOUT_SECONDS = 30
BOT_ACCOUNT_TYPE = "Bot"
BADGE_SCHEMA_VERSION = 1
BADGE_COLOR = "brightgreen"
DOWNLOADS_LABEL = "downloads"
CONTRIBUTORS_LABEL = "contributors"
DOWNLOADS_FILE_NAME = "downloads.json"
CONTRIBUTORS_FILE_NAME = "contributors.json"


def build_request(url: str, token: str) -> urllib.request.Request:
    headers = {
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return urllib.request.Request(url, headers=headers)


def fetch_page(url: str, token: str) -> list[dict[str, Any]]:
    request = build_request(url, token)
    with urllib.request.urlopen(request, timeout=REQUEST_TIMEOUT_SECONDS) as response:
        return json.load(response)


def iterate_paginated(endpoint: str, token: str) -> Iterator[dict[str, Any]]:
    page_number = 1
    while True:
        separator = "&" if "?" in endpoint else "?"
        url = f"{GITHUB_API_ROOT}{endpoint}{separator}per_page={PAGE_SIZE}&page={page_number}"
        items = fetch_page(url, token)
        yield from items
        if len(items) < PAGE_SIZE:
            return
        page_number += 1


def count_release_downloads(repository: str, token: str) -> int:
    releases = iterate_paginated(f"/repos/{repository}/releases", token)
    return sum(
        asset["download_count"]
        for release in releases
        for asset in release.get("assets", [])
    )


def count_human_contributors(repository: str, token: str) -> int:
    contributors = iterate_paginated(f"/repos/{repository}/contributors", token)
    return sum(1 for contributor in contributors if contributor.get("type") != BOT_ACCOUNT_TYPE)


def format_count(value: int) -> str:
    if value >= 1_000_000:
        return f"{value / 1_000_000:.1f}M"
    if value >= 1_000:
        return f"{value / 1_000:.1f}k"
    return str(value)


def build_badge(label: str, value: int) -> dict[str, Any]:
    return {
        "schemaVersion": BADGE_SCHEMA_VERSION,
        "label": label,
        "message": format_count(value),
        "color": BADGE_COLOR,
    }


def write_badge(output_directory: Path, file_name: str, badge: dict[str, Any]) -> None:
    output_path = output_directory / file_name
    output_path.write_text(json.dumps(badge, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {output_path}: {badge['message']}")


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output_directory", type=Path)
    parser.add_argument("--repository", default=os.environ.get("GITHUB_REPOSITORY", DEFAULT_REPOSITORY))
    return parser.parse_args()


def main() -> None:
    arguments = parse_arguments()
    token = os.environ.get("GITHUB_TOKEN", "")
    arguments.output_directory.mkdir(parents=True, exist_ok=True)

    downloads = count_release_downloads(arguments.repository, token)
    contributors = count_human_contributors(arguments.repository, token)

    write_badge(arguments.output_directory, DOWNLOADS_FILE_NAME, build_badge(DOWNLOADS_LABEL, downloads))
    write_badge(arguments.output_directory, CONTRIBUTORS_FILE_NAME, build_badge(CONTRIBUTORS_LABEL, contributors))


if __name__ == "__main__":
    main()
