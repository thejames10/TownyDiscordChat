# TownyDiscordChat SQL branch — test deployment

**Status: pre-release integration candidate.** Do not deploy to a production
guild until you have tested the queue, database migrations and role revocation.

## Compiled dependency combinations

| Minecraft API | DiscordSRV API | Towny API | Workflow |
| --- | --- | --- | --- |
| 1.16.5, 1.17.1, 1.18.2, 1.19.4, 1.20.1, 1.20.6, 1.21.1, 1.21.4, 1.21.8, 1.21.10 | 1.24.0 | 0.97.5.0 | Baseline matrix |
| 1.20.1 | 1.30.5 | 0.103.2.0 | Modern API test |
| 1.21.10 | 1.30.5 | 0.103.2.0 | Modern API test |

These results demonstrate **Maven compilation and packaging**, not
runtime correctness on all listed Minecraft versions. The modern test
checks the published DiscordSRV and Towny APIs and excludes their
classes from the shaded JAR. Minecraft API version alone does not
certify that a particular Towny/DiscordSRV combination works.

## Required server plugins

- [DiscordSRV 1.30.5](https://github.com/DiscordSRV/DiscordSRV/releases/tag/v1.30.5)
  is the current release tested in the modern API lane. DiscordSRV
  must be loaded and linked to the intended Discord guild.
- [Towny 0.103.2.0](https://github.com/TownyAdvanced/Towny/releases/tag/0.103.2.0)
  is the current stable tested in the modern API lane; supports MC
  1.19 and newer, requires Java 17+, and Paper/Purpur (not Spigot).
- [Towny 0.100.3.0](https://github.com/TownyAdvanced/Towny/releases/tag/0.100.3.0)
  is the historical branch recommended by Towny for 1.16–1.18.
  Verify its own Java/server requirements for the selected release.
- Towny 0.102+ does not support Spigot, and this plugin currently
  **does not support Folia's region scheduler**. Use Paper or Purpur.
- TownyDiscordChat needs a working MySQL database (MySQL 8 preferred).

Towny, DiscordSRV and their bundled JDA are **provided by the server**,
not bundled with TownyDiscordChat. Install each from its official site.
MySQL Connector/J and relocated bStats are packaged in the plugin JAR.

## Install a CI artifact for testing

1. Open the successful **Test Minecraft compatibility** workflow run
   on the SQL branch in [GitHub Actions](https://github.com/thejames10/TownyDiscordChat/actions).
2. Download the `TownyDiscordChat-MC-<version>-modern` artifact ZIP for
   a modern Paper/Purpur 1.20.1 or 1.21.10 server, or a baseline
   `TownyDiscordChat-MC-<version>` ZIP when evaluating older APIs.
3. Extract the ZIP. Put **only the .jar** inside `plugins/`; do not
   install the ZIP itself.
4. Configure DiscordSRV, install the appropriate Towny version,
   and enable **Server Members Intent** in the Discord Developer Portal
   and the matching DiscordSRV/JDA configuration. Without that intent,
   complete guild audits cannot run.
5. Set `SQL.host`, `SQL.port`, `SQL.database`, `SQL.username` and
   `SQL.password` in `plugins/TownyDiscordChat/config.yml`.
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
selected shaded-JDA methods. If incompatible it starts in an inert
safe mode and notifies the console and joining admins.

## Queuing, migration and security

MySQL stores the latest desired membership and two independent queues:
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

The GitHub Actions matrix generates separate test ZIPs. A tagged `v*`
build can publish ZIPs, but **do not tag a public release** until actual
Paper/Purpur + Towny + DiscordSRV combinations have been tested for
Discord permissions, MySQL rollback, restarts and rate limits.
