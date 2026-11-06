# Compatibility and test deployments

## Status
These are CI-built **test candidates** until exercised against a running server. The
matrix compiles the same source against Spigot API snapshots for Minecraft 1.16.5
through 1.21.x. This does NOT prove binary compatibility with each available
Towny/DiscordSRV release, nor does it guarantee runtime support on Paper/Folia.

## Installing
1. Install a server implementation for your selected Minecraft version.
2. Install a Towny release supported by that exact server version.
3. Install a DiscordSRV release supported by that exact server version.
4. Copy only TownyDiscordChat-MC-<version>.jar into plugins; do not install the ZIP.
5. Install MySQL 8-compatible database, configure SQL settings in config.yml,
   and check server log for queue migration / initialization.
6. Back up MySQL tables and Discord guild configuration before first run. Test on
   a non-production guild: reconciliation is capable of creating Discord resources.

DiscordSRV and Towny are **server-provided** dependencies, NOT shaded into this
plugin's JAR. Each third-party plugin must be obtained from its official release
source under its own license. DiscordSRV's bundled JDA performs HTTP rate-limit
handling; this plugin adds database-backed durable scheduling and retry handling.

## Dependency baselines used for compilation
- DiscordSRV 1.24.0 (legacy JDA 4 API used in this source)
- Towny 0.97.5.0 (legacy Towny API)
- MySQL Connector/J 8.0.33 (bundled)
- bStats Bukkit 2.2.1 (relocated and bundled)
- Spigot API version is selected per matrix job.

This old DiscordSRV/Towny combination is **not guaranteed** to work on current
1.20+/1.21+ servers. A separate modern integration adapter will be needed if
their API compatibility has changed. Do not upgrade the DiscordSRV Maven
dependency without also checking JDA package and API changes.

## Java
The generated plugin targets Java 8 bytecode; Minecraft 1.16 can use Java 8.
Minecraft 1.17+ has its own higher Java runtime minimums.
Choose the Java runtime required by the server implementation, not by this JAR.

## SQL queue
The SQL branch's legacy tdc_queued_tasks table is imported into
tdc_discord_jobs and is otherwise kept in place. Deduplication keys are
kind+resource name; the worker claims one job at a time with a lease.
Retries use exponential backoff; exhausted jobs are marked DEAD and retained.
Application pacing does not replace JDA's API rate-limit handling.
The legacy listener mutation paths remain disabled while they are converted.

## Testing checklist
- Start with an empty guild and an isolated MySQL schema.
- Verify roles, categories, text and voice channels for town and nation.
- Restart server during a Discord operation and check that no duplicate resource
  is created and the job is eventually acknowledged.
- Verify callback failure produces retries and eventually a DEAD job.
- Disconnect DiscordSRV and reconnect; database backlog should remain queued.
- Check role/category permissions and enforce Discord hierarchy.
- Confirm there are no Bukkit asynchronous-access warnings.
- Test manual Discord resource deletion, guild role rename, and town rename.
- Validate account link/unlink, Towny delete/rename, role removal and
  permissions before production; these reconciliation behaviors are not finished.

## Future release workflow
Create signed/manual releases only after CI passes and live compatibility tests
pass. Separate artifacts for each Minecraft version are created by the
GitHub Actions build and can be downloaded from the workflow run's artifacts.

## Operator commands
- `/tdcqueue status`: show pending, running and dead job counts.
- `/tdcqueue retry-dead`: retry exhausted jobs after diagnosing the root cause.
Requires permission `TownyDiscordChat.Admin`; works from the console.
