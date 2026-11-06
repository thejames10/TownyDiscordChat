# TownyDiscordChat 1.1.1 — compatibility and deployment

**Status: public pre-release.** Do not deploy to a production
guild until you have tested the queue, database migrations and role revocation.

## Compatibility matrix

Each listed Minecraft version has a separately compiled JAR and a matching optional setup ZIP.

| Minecraft | Server | Towny | DiscordSRV | Java | Validation |
| --- | --- | --- | --- | --- | --- |
| 1.16.5, 1.17.1, 1.18.2 | Spigot / Paper / Purpur | 0.100.3.0 | 1.30.5 | Server-dependent | CI |
| 1.19.4 | Paper / Purpur | 0.103.2.0 | 1.30.5 | Server-dependent | CI |
| 1.20.1, 1.20.6 | Paper / Purpur | 0.103.2.0 | 1.30.5 | Server-dependent | CI |
| 1.21.1, 1.21.4, 1.21.8, 1.21.10, 1.21.11 | Paper / Purpur | 0.103.2.0 | 1.30.5 | 21+ | CI |
| 26.1.2, 26.2 | Paper / Purpur | 0.103.2.0 | 1.30.5 | 25+ | CI |
| 26.3 | Paper / Purpur | 0.103.2.15 (pre-release) | 1.30.5 | 25+ | CI |

**CI** confirms compilation, automated tests and packaging; it is not live-server certification. Java runtime requirements for older versions follow the selected Minecraft server and Towny/DiscordSRV installation.

Minecraft 26.x uses Java 25 and Paper's build-qualified API versions. Minecraft 26.1.0 has no Paper build, and 26.1.1 is obsolete (alpha-only); use 26.1.2 instead. Folia and modern Towny on Spigot/CraftBukkit are not supported.

For 26.3, Towny 0.103.2.15 is a **pre-release**. Check the corresponding Paper/Purpur server build carefully before updating a production server. The matching ZIP's `BUILD-INFO.txt` records the exact Paper/Spigot, Towny and DiscordSRV compile APIs.

## Required server plugins

- [DiscordSRV 1.30.5](https://github.com/DiscordSRV/DiscordSRV/releases/tag/v1.30.5)
  is the DiscordSRV API version used for all release packages. DiscordSRV
  must be loaded and linked to the intended Discord guild.
- [Towny 0.103.2.0](https://github.com/TownyAdvanced/Towny/releases/tag/0.103.2.0)
  is the Towny API used by the 1.19–26.2 builds. It requires Java 17+
  and Paper/Purpur (not Spigot).
- [Towny 0.103.2.15](https://github.com/TownyAdvanced/Towny/releases/tag/0.103.2.15)
  is the 26.3-compatible pre-release required by the 26.3 build.
- [Towny 0.100.3.0](https://github.com/TownyAdvanced/Towny/releases/tag/0.100.3.0)
  is the historical branch recommended by Towny for 1.16–1.18.
  Verify its own Java/server requirements for the selected release.
- Towny 0.102+ does not support Spigot, and this plugin currently
  **does not support Folia's region scheduler**. Use Paper or Purpur.
- TownyDiscordChat defaults to an embedded **SQLite** database. MySQL 8 is optional.

Towny, DiscordSRV and their bundled JDA are **provided by the server**,
not bundled with TownyDiscordChat. Install each from its official site.
SQLite JDBC, MySQL Connector/J, and relocated bStats are packaged in the plugin JAR.

## Installing the 1.1.1 pre-release

1. Open the [v1.1.0 release](https://github.com/thejames10/TownyDiscordChat/releases/tag/v1.1.1).
2. Download `TownyDiscordChat-MC-<version>.jar` for your exact
   Minecraft version, or the matching ZIP with configuration and guides.
3. Install only the JAR in `plugins/`. Before starting a new
   installation, copy `config.example.yml` from the ZIP into
   `plugins/TownyDiscordChat/config.yml`.
4. Configure DiscordSRV, Towny, the bot's private channel permissions
   and privileged Server Members Intent.
5. For new installations, SQLite is automatic when the MySQL credentials
   remain as placeholders. Existing MySQL users must preserve their
   connection settings.
6. For upgrades, **back up your database**, then read
   [config.yml.bak upgrades](README.md#automatic-config-upgrade-from-configymlbak).
   The first successful import remains in safe mode until review/restart.
7. Verify `/tdcqueue status` and optionally request
   `/tdcqueue audit`.

**CI compilation is not live server certification.** Test with backups
or an isolated Discord guild before deploying to production.

## Version and Java policy

The plugin is compiled to Java 8 bytecode. The **server and installed
Towny/DiscordSRV versions determine the actual minimum Java version**.
Towny 0.103.x requires Java 17+. Minecraft 1.21 uses Java 21; 26.x uses Java 25. CompatibilityGuard checks the plugin versions and
selected shaded-JDA methods. The live guild preflight also verifies
Manage Roles, Manage Channels, private category permissions, and the
bot's role hierarchy above current town/nation roles. If incompatible it starts in an inert
safe mode and notifies the console and joining admins.

## Configuration upgrades

When replacing an older TownyDiscordChat build, the recommended upgrade
path is to **stop the server**, back up the plugin's database and data,
then rename `plugins/TownyDiscordChat/config.yml` to
`config.yml.bak` before the first start of the new JAR. The plugin
generates its current template and copies compatible values.
If `config.yml` already exists, it takes precedence and no
automatic backup import is attempted. The backup is retained;
unsupported values abort migration into safe mode. A successful import
also remains in **review-only safe mode** until the administrator checks
the generated settings and restarts. No Discord synchronization occurs
on the import startup.

MySQL credentials are retained for existing MySQL users; the new
`storage.type: auto` selects MySQL when legacy credentials are present.
The config upgrade is **not** a database migration. See
[README.md](README.md#automatic-config-upgrade-from-configymlbak).

## Legacy logging settings

The legacy `messages.DiscordLogTextChannelId` numeric ID is ignored.
When `log.Enabled: true` (default), the **SQL activity logger** finds
one existing private text channel by `log.Category.Name` and
`log.Channel.Name`. The bot must be able to read and write there; a
category or channel-level `@everyone` View Channel denial is required.
A startup confirmation, real Discord changes, and queue failures are
posted asynchronously. Adoption/no-op reconciliation logs only to the
Minecraft console, so existing guilds are not flooded on startup.
If the log channel is missing, ambiguous, public, or unwritable,
`/tdcqueue status` reports its status and the console warns; queue
processing continues. Logs are best effort and not durably replayed.

## Runtime and legacy-code boundaries

The original polling schedulers, direct Towny/DiscordSRV event handlers
and task graph have been removed from SQL-branch builds. Only the durable
queue and reconciliation service may create Discord resources or modify
managed member roles. **Historic database tables and their migrations
remain intact**, so removing unused Java classes does not discard old
town, nation or queue mappings.

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

A versioned GitHub Release provides **one standalone JAR and one setup ZIP
per listed Minecraft version**. Automated checks verify API compilation,
database migration tests, version metadata, and documentation links before
publishing the artifacts. Live Paper/Purpur, Towny and DiscordSRV
integration testing remains necessary before production certification.
