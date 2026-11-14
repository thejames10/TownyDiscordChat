# Contributing to TownyDiscordChat

Thanks for helping improve TownyDiscordChat. This guide covers building, testing
and the conventions a pull request is reviewed against.

## Before you start

- For bugs, open an issue with the bug report template first, so the Minecraft
  version, server software and dependency versions are recorded.
- For larger changes, open an issue or ask on [Discord](https://discord.gg/ynsCb2p3ac)
  before writing code, so the design can be agreed early.
- Security problems must not be reported in public issues. See [SECURITY.md](SECURITY.md).

## Building

Requirements: JDK 21 (Minecraft 1.16.5–1.21.11) or JDK 25 (Minecraft 26.x), and Maven 3.9+.

```sh
mvn --batch-mode clean verify
```

The default build targets the Spigot 1.20.1 API. To build for another Minecraft
version, pass the same properties that CI uses for that row of the matrix in
[`.github/workflows/compatibility.yml`](.github/workflows/compatibility.yml), for example:

```sh
mvn --batch-mode clean verify \
  -Dserver.api.groupId=io.papermc.paper \
  -Dserver.api.artifactId=paper-api \
  -Dserver.api.version=26.3.build.218-stable \
  -Dbukkit.api.version=26.3 \
  -Dadventure.version=5.2.0 \
  -Dtowny.version=0.103.2.15
```

Towny and DiscordSRV are `provided` dependencies. They must never be shaded into
the plugin JAR; CI fails if they are.

## Tests

- `mvn verify` runs the JUnit 5 suite. New behaviour needs a test, and a bug fix
  needs a test that fails without the fix.
- `python3 scripts/check_repo_consistency.py` checks that the README, `pom.xml`,
  `plugin.yml`, `config.yml` and the CI matrix agree.
- `python3 scripts/export_compatibility_matrix.py --check` validates the
  machine-readable compatibility matrix.

Changes that touch Discord synchronization, storage migrations or startup safety
should also follow the manual steps in [TEST-PLAN.md](TEST-PLAN.md).

## Code style

Java follows the Sun/Oracle code conventions as used across Bukkit, Spigot and
Paper plugins, matching IntelliJ IDEA's default Java style:

- 4 spaces, no tabs; K&R braces; braces on every control statement.
- `PascalCase` types, `camelCase` methods and fields, `UPPER_SNAKE_CASE` constants.
- One statement per line; explicit imports, never wildcard imports.
- Small, single-purpose methods (aim for under ~30 lines) with early returns
  instead of deep nesting.
- No magic values: name numbers, timeouts and config keys as constants.
- Comment *why* a non-obvious decision was made, not *what* the code does.
- Do not reformat code unrelated to your change.

The project's [`.editorconfig`](.editorconfig) applies the basic whitespace rules
in most editors.

## Commits and pull requests

- Use [Conventional Commits](https://www.conventionalcommits.org/) prefixes:
  `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`.
- Keep each pull request focused on one change. Fill in the pull request template.
- All CI jobs must pass: the 14-version compatibility build, the historical API
  check and the repository consistency checks.
- Describe only what the change does to this plugin. Commit messages, pull request
  text and files must not describe private infrastructure or deployment
  environments; CI rejects known internal identifiers.

## Licence

By contributing, you agree that your contributions are licensed under the
project's [GPL-3.0 licence](LICENSE).
