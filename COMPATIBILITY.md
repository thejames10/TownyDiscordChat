# TownyDiscordChat SQL branch — test deployment

**Status: pre-release integration candidate.** Do not deploy to a production
guild until you have tested the queue, database migrations and role revocation.

## Intended test combinations

One ZIP is produced per Minecraft API version. No special `-modern` download is needed.

| Minecraft API | Server implementation | Towny compile API | DiscordSRV compile API | Status |
| --- | --- | --- | --- | --- |
| 1.16.5, 1.17.1, 1.18.2 | Spigot / Paper / Purpur | 0.100.3.0 | 1.30.5 | 🧪 CI test package |
| 1.19.4 | Paper / Purpur | 0.103.2.0 | 1.30.5 | 🧪 CI test package |
| 1.20.1, 1.20.6 | Paper / Purpur | 0.103.2.0 | 1.30.5 | 🧪 CI test package |
| 1.21.1, 1.21.4, 1.21.8, 1.21.10 | Paper / Purpur | 0.103.2.0 | 1.30.5 | 🧪 CI test package |

The `legacy-api-check` workflow job still compiles the source against
DiscordSRV 1.24.0 and Towny 0.97.5.0 to detect historical API regressions.
It does **not** produce an alternative installer. The old `-modern`
suffix indicated compile-time dependencies only: Towny, DiscordSRV and
JDA are provided by the server and are never shaded inside our JAR.

These checks establish compilation, tests, and packaging; **not**
runtime compatibility with an actual Discord guild or production server.
The `BUILD-INFO.txt` file in each ZIP records the exact compile
API versions. See [README.md](README.md#compatibility) for an at-a-glance
status table.

## Required server plugins

- [DiscordSRV 1.30.5](https://github.com/DiscordSRV/DiscordSRV/releases/tag/v1.30.5)
  is the DiscordSRV API version used by current CI test packages. DiscordSRV
  must be loaded and linked to the intended Discord guild.
- [Towny 0.103.2.0](https://github.com/TownyAdvanced/Towny/releases/tag/0.103.2.0)
  is the modern Towny API version used by current CI packages; supports MC
  1.19 and newer, requires Java 17+, and Paper/Purpur (not Spigot).
- [Towny 0.100.3.0](https://github.com/TownyAdvanced/Towny/releases/tag/0.100.3.0)
  is the historical branch recommended by Towny for 1.16–1.18.
  Verify its own Java/server requirements for the selected release.
- Towny 0.102+ does not support Spigot, and this plugin currently
  **does not support Folia's region scheduler**. Use Paper or Purpur.
- TownyDiscordChat defaults to an embedded **SQLite** database. MySQL 8 is optional.

Towny, DiscordSRV and their bundled JDA are **provided by the server**,
not bundled with TownyDiscordChat. Install each from its official site.
SQLite JDBC, MySQL Connector/J, and relocated bStats are packaged in the plugin JAR.

## Install a CI artifact for testing

1. Open a successful **Test Minecraft compatibility** run on
   [the SQL branch](https://github.com/thejames10/TownyDiscordChat/actions?query=branch%3ASQL).
2. Download `TownyDiscordChat-MC-<version>` for your exact Minecraft
   version. The ZIP has **no** `-modern` suffix. Read the included
   `BUILD-INFO.txt` for the exact compile API versions.
3. Extract the ZIP. Put **only the .jar** inside `plugins/`; do not
   install the ZIP itself. Before the first server start, copy the
   included `config.example.yml` to `plugins/TownyDiscordChat/config.yml`
   and review storage/category names.
4. Configure DiscordSRV, install the appropriate Towny version,
   and enable **Server Members Intent** in the Discord Developer Portal
   and the matching DiscordSRV/JDA configuration. Without that intent,
   complete guild audits cannot run.
5. For a new server, leave `storage.type: auto` and the MySQL credentials at
   their defaults; SQLite will be created automatically. For an existing
   MySQL server, set `storage.type: mysql` and fill in the `SQL` fields.
6. Back up the existing database first, start the server, check logs and
   run `/tdcqueue status` as console/operator.
7. Use `/tdcqueue audit` to start a full-guild audit; it runs through
   the durable queue. Use `/tdcqueue retry-dead` only after fixing
   the reason a job exhausted its retries.

**Never point your first test at a live guild that contains irreplaceable
roles/channels.** This plugin intentionally creates, renames, revokes
membership from, and deletes recognized Towny-managed Discord resources.

## Version and Java policy

The plugin is compiled to Java 8 bytecode. The **server and installed
Towny/DiscordSRV versions determine the actual minimum Java version**.
Towny 0.103.2.0 requires Java 17+. Newer Minecraft 1.21 servers may
require Java 21. CompatibilityGuard checks the plugin versions and
selected shaded-JDA methods. The live guild preflight also verifies
Manage Roles, Manage Channels, private category permissions, and the
bot's role hierarchy above current town/nation roles. If incompatible it starts in an inert
safe mode and notifies the console and joining admins.

## Legacy logging settings

`log.Category.Name`, `log.Channel.Name`, and
`messages.DiscordLogTextChannelId` remain in the config for upgrade
compatibility. The old Discord log-channel scheduler is intentionally
inactive; critical API/queue errors are printed to the Minecraft console.

## Queuing, migration and security

SQLite or MySQL stores the latest desired membership and two independent queues:
security revocations are preferred in the membership lane, while
resource deletions take priority over additions in the resource lane.
DiscordSRV/JDA handles actual Discord rate-limit responses.

The old `tdc_queued_tasks` table (including unexpired NULL/false/0 rows) is imported non-destructively into
`tdc_discord_jobs`; its records are not dropped. New tables include
`tdc_member_intents`, `tdc_managed_member_roles`,
`tdc_towny_identity`, `tdc_schema_migrations` and `tdc_audit_runs`.
Stable Towny UUID mappings preserve Discord IDs across in-place renames.
See [SYNC-MIGRATION.md](SYNC-MIGRATION.md).

Automatic deletion requires an existing stable UUID-to-Discord mapping,
a grace period (120 seconds by default), and exact Discord ownership
verification. It does not delete categories, repurposed roles/channels,
or ambiguous old resources.

## Operator commands

- `/tdcqueue status` — counts of pending/running/dead jobs and intentions.
- `/tdcqueue audit` — queue a full guild member scan.
- `/tdcqueue retry-dead` — retry exhausted work after resolving root cause.

All three require `TownyDiscordChat.Admin` (operator by default).
See [TEST-PLAN.md](TEST-PLAN.md) for deployment and recovery tests.

## Release policy

The GitHub Actions matrix generates **one** test ZIP per Minecraft version. A tagged `v*`
build can publish ZIPs. CI now checks the Markdown compatibility matrix,
local links, Maven version and packaged artifact contents; a release
tag must match the checked-in README and POM versions, but **do not tag a public release** until actual
Paper/Purpur + Towny + DiscordSRV combinations have been tested for
Discord permissions, SQLite/MySQL rollback, restarts and rate limits.
