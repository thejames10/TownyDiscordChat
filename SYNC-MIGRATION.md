# Synchronization, migration and safety design

## Sources of truth

Towny defines who belongs to each town/nation. Discord defines the
resources and member-role state actually present. SQL stores an
auditable intent, ownership mappings and retry history; neither
an empty DB nor a stale Discord cache is treated as proof that
resources do not exist.

The plugin retains **its own durable JDBC-backed SQLite/MySQL queue**, with two independent lanes.
Revocations take precedence over grants in the membership lane.
Verified role/channel deletions (priority 10) precede resource
renames (priority 20) and creations (priority 100) in the resource lane.
DiscordSRV's relocated JDA still handles Discord HTTP 429 rate limits.

## Migrations

Migration 2 imports the legacy `tdc_queued_tasks` entries into
`tdc_discord_jobs`, leaving the old table in place and recording
completion in `tdc_schema_migrations`. The migration is idempotent.
Migration 3 adds `tdc_towny_identity` with primary key
`(guild_id,kind,towny_uuid)`, storing the last-known name plus
role/text/voice Discord IDs. The original legacy SQL mapping tables
are preserved. During a rename, their name-based keys are updated
transactionally without changing Discord IDs. Back up the schema
before starting a new version. Migration 4 imports older queued tasks
where `Expired` was the string `false`, `0`, or empty rather than SQL
NULL. Migration 5 normalizes these active flags in the **legacy resource
mapping tables** to NULL (the modern active-row convention); rows marked
`true` remain retired and original legacy queue history is preserved.
Migration 6 records a completed offline SQLite-to-MySQL copy on the
destination. These migrations are idempotent.

The identity table is backfilled from verified existing resources when
the plugin first runs. If a town was renamed **before the identity
table existed**, and there is no safe UUID link between the old name
and new Towny object, automatic inference is impossible. The old
mapping is left untouched for manual review rather than deleted.

## REST callback and ownership race protection

Discord CREATE, grant and revoke callbacks can outlive the configured
SQL lease. The dispatcher **quarantines the exact in-flight job ID**
instead of allowing another worker attempt to claim it. Other pending
jobs remain eligible, so a hung addition does not starve unrelated
revocations, and resource deletion work can overtake slower creations.

The number of unresolved callbacks is capped at 16 membership requests
and 4 resource requests. Hitting the cap pauses new work in that lane
and raises an administrator warning; this avoids uncontrolled REST
concurrency during a sustained outage. Individual callbacks remove
their IDs from the quarantine after database finalization. JDA's
rate-limit handler still governs every Discord HTTP request.

After a process restart, expired SQL leases become eligible for normal
recovery. The worker checks Discord's visible state and adopts only
unique resources with verified role/category privacy. Multiple
same-name channels or unsafe permission overrides require manual
resolution rather than automatic duplicate creation.

CREATE jobs verify that the live Towny UUID matches the one recorded
for the requested mutable name. If a town/nation renames during an
old-name create, the stable UUID mapping retains its old name until
the original callback finishes. If the Towny entity is deleted, an
identity with a running create is **not retired**; a recovered remote
resource is recorded against that deleted UUID and queued for cleanup.
A newly created entity that reuses the name cannot inherit the older
UUID's role or channel. Verified stable IDs also permit cleanup even
if the legacy name table has been lost.

The old direct-event REST handlers, polling schedulers and Tasks
package have been removed, while all historical SQL mapping tables
and migration code remain supported.

Discord REST does not provide exactly-once side effects across a
network disconnect or crash. Startup discovery and conservative
identity/privacy checks minimize the risk, but ambiguous objects
may still need administrator review.

## YAML configuration upgrade (separate from database migration)

On startup, if `config.yml` is absent and `config.yml.bak` exists,
the plugin builds a new file using its bundled defaults and comments,
then overlays only recognized, type-compatible legacy settings.
Old MySQL credentials, Towny/Discord category values and other
supported options are retained; new settings use current defaults.
Obsolete keys are listed in the console without printing their values.
The source `.bak` is never modified and an existing `config.yml`
always takes precedence.

A malformed backup, an incompatible setting, or failed current-schema
validation leaves the new file unpublished and starts the plugin in
safe mode. The offline `storage.migrateToMySQL` flag is deliberately
reset rather than implicitly initiating a database transfer.
This file migration has no dependency on Discord API calls and
does **not** migrate SQL/SQLite data.

## Storage backends and offline migration

The same durable queue and mapping tables run on embedded **SQLite** or an
external **MySQL** database. `storage.type: auto` keeps existing MySQL
installations on MySQL when the legacy `SQL` credentials are configured;
otherwise it creates `plugins/TownyDiscordChat/townydiscordchat.db`.
`storage.type: sqlite` or `mysql` selects the backend explicitly.

The storage adapter translates the limited legacy MySQL SQL used by TDC
(`INSERT IGNORE`, `ON DUPLICATE KEY UPDATE`, `IF`, `FOR UPDATE`
and `AUTO_INCREMENT`) to SQLite while preserving transactions, unique
constraints and retry metadata. SQLite enables write-ahead logging,
a busy timeout and indexes for claim queries. This setup supports a
**single Minecraft server**, not multiple competing writers/processes.

### Direct SQLite → MySQL transfer

Use `storage.type: sqlite` plus `storage.migrateToMySQL: true` with a
new **empty** target MySQL database and valid MySQL credentials. Starting
the plugin initializes SQLite and schedules a **migration-only**
asynchronous copy without starting Discord workers. The migration reads
all `tdc_*` tables, creates matching MySQL schemas, transfers all
records in batches within a transaction, verifies row counts and
records migration version 6. This includes:

- Legacy role, category, channel, player and queued-task mappings.
- Durable `tdc_discord_jobs` with IDs, priorities, attempts and retry state.
- `tdc_member_intents` revisions, `tdc_managed_member_roles` grants.
- `tdc_towny_identity`, guild audit checkpoints, schema migration markers.

Existing populated destination tables are **rejected**; a failed copy
rolls back inserts. The original SQLite file is **never modified by the
transfer** (normal schema initialization may have run beforehand).
After the console confirms success, stop the server, change to
`storage.type: mysql`, set `storage.migrateToMySQL: false`, restart
and check the queue. Keep SQLite as a backup. Never have both storage
backends processing the same Discord guild concurrently.

Unlike a blank-DB recovery scan, copying the actual mappings consumes
**zero Discord API requests**, preserves pending revocations, and avoids
rediscovering every remote resource. Discord reconciliation remains the
fallback for truly missing or inconsistent records, not the preferred
way to change databases.

## State recovery

| State | Action |
| --- | --- |
| No SQL rows; Discord already populated | Adopt only verified uniquely-named roles and private channels, then scan guild membership |
| SQL references missing Discord objects | Recreate using the current Towny entity and correct SQL mappings |
| Partially created Discord guild | Enqueue only missing roles/categories/channels |
| Existing Towny UUID was renamed | Migrate SQL keys and rename the existing Discord role/text/voice channels **in-place** |
| Towny entity truly deleted | After a grace period, delete only resources whose exact IDs are mapped to that deleted UUID |
| Guild roles modified manually | Refuse unsafe or ambiguous adoption/deletion; notify administrators |
| Server crashes during a Discord operation | Recover expired SQL leases, inspect actual Discord state before retrying |

One unique job key prevents repeated creation requests for the
same resource. In-flight and failed tasks survive restarts. Attempts
are retried with exponential backoff; exhausted jobs are retained as
`DEAD` for operator diagnosis.

Resource deletion is intentionally delayed by
`queue.deletionGraceSeconds` (default 120) to allow rapid renames.
Categories are never auto-deleted because they might contain
unrelated Discord content. In-flight HTTP actions cannot be
cancelled transactionally; reconciliation detects stale outcomes.

## Membership security

`tdc_member_intents` uses one unique key per (guild, member, role).
Its `desired` bit and monotonically increasing `revision` collapse
ADD/REMOVE/ADD into the latest requested state. Late callbacks cannot
acknowledge a newer intent. Before sending a grant, the dispatcher
rechecks the member's current Towny membership *and* the immutable
Towny UUID ownership of the target Discord role. A new town that
reuses a deleted town's name cannot inherit the old Discord role.
Targeted member REST audits also resolve the expected role through the
**stable Towny UUID**, preventing transient revocation when Discord's
role rename has not completed. A delayed member lookup returns to
Bukkit's main thread and revalidates Towny membership before granting.

Fast Towny and DiscordSRV events request a coalesced reconciliation.
Revocations remain prioritized independently of slow category
creation, and failures are logged prominently.

**No application can guarantee immediate revocation during Discord
outages, missing bot permissions or Discord rate-limit delays.**
In such conditions admins should manage the affected Discord access
directly until the bot recovers.

## Complete guild member audits

DiscordSRV's JDA `Guild.getMembers()` reports cache contents only.
The durable `AUDIT_GUILD` job instead invokes JDA's
`Guild.loadMembers(Consumer<Member>)` gateway chunking. This
requires **privileged GUILD_MEMBERS / Server Members Intent** to
be enabled for the bot in the Discord Developer Portal and in
DiscordSRV's gateway setup.

The audit records every observed *verified Towny-managed* role
grant, including completely unlinked/uncached members, in
`tdc_managed_member_roles`. The following regular reconciliation
compares those grants to Towny's current linked accounts and queues
revocations where appropriate. Routine targeted REST audits also
cover linked members when their caches are incomplete.

The guild audit runs on startup and periodically (default every
21,600 seconds), or on demand via `/tdcqueue audit`. SQL checkpoints
in `tdc_audit_runs` avoid scanning the entire guild repeatedly.
If the privileged intent is disabled, the job fails without altering
unrelated Discord resources and an actionable warning goes to
console and administrators. Targeted audits remain operational.

## Startup validation and DiscordSRV onboarding

The preflight checks YAML, selected storage settings, role color formats,
category names/legacy ID hints, queue intervals and date/time
patterns before starting SQL. When the guild is connected, it
validates private category candidates and bot permissions before
enabling the queue. Legacy category IDs of `0` are `auto`; the
reconciler uses group names, not a magic ID. Old numeric IDs are
checked as safety hints.

DiscordSRV alone generates/consumes link verification codes.
TownyDiscordChat exposes `/tdc link` as a delegate and
`/tdc status` as a contextual view. Join and town-join reminders
require the player to actually belong to a town and to have
no DiscordSRV-linked account; a configurable cooldown limits
repeated prompts. Link events request a reconciled final state.

## Compatibility and limitations

The published CI packages compile against DiscordSRV 1.30.5 and
Towny 0.100.3.0 (Minecraft 1.16–1.18) or 0.103.2.0 (newer
Minecraft versions). An additional development-only job checks
historical DiscordSRV 1.24.0 / Towny 0.97.5.0 source compatibility. Modern Towny 0.102+ requires
Paper/Purpur/Folia; this plugin's Bukkit scheduler does **not**
support Folia, so use Paper/Purpur. The version guard verifies
minimum versions and selected shaded-JDA methods, and enters
inert safe mode when unsupported.

Compilation is not a live integration test. Test a separate Discord
guild and backed-up SQL database for empty/partial state,
rename and delete, rapid join/kick/rejoin, unlinked accounts,
HTTP failures, full-guild audit permissions and server crash recovery.

See [COMPATIBILITY.md](COMPATIBILITY.md) and
[TEST-PLAN.md](TEST-PLAN.md).
