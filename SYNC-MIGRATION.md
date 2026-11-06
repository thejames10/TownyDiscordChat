# Synchronization, migration and safety design

## Sources of truth

Towny defines who belongs to each town/nation. Discord defines the
resources and member-role state actually present. SQL stores an
auditable intent, ownership mappings and retry history; neither
an empty DB nor a stale Discord cache is treated as proof that
resources do not exist.

The plugin retains **its own MySQL queue**, with two independent lanes.
Revocations take precedence over grants in the membership lane.
Verified role/channel deletions (priority 10) precede resource
renames (priority 20) and creations (priority 100) in the resource lane.
DiscordSRV's relocated JDA still handles Discord HTTP 429 rate limits.

## Migrations

Migration 2 imports the legacy `tdc_queued_tasks` entries into
`tdc_discord_jobs`, leaving the old table in place and recording
completion in `tdc_schema_migrations`. The migration is idempotent.
Migration 4 also imports older unexpired queue rows where the legacy\n`Expired` column contains the string `false`, `0` or empty instead of SQL\nNULL. No historical rows are removed. Migration 3 adds `tdc_towny_identity` with primary key
`(guild_id,kind,towny_uuid)`, storing the last-known name plus
role/text/voice Discord IDs. The original legacy SQL mapping tables
are preserved. During a rename, their name-based keys are updated
transactionally without changing Discord IDs. Back up the schema
before starting a new version.

The identity table is backfilled from verified existing resources when
the plugin first runs. If a town was renamed **before the identity
table existed**, and there is no safe UUID link between the old name
and new Towny object, automatic inference is impossible. The old
mapping is left untouched for manual review rather than deleted.

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

## Compatibility and limitations

Both the legacy (DiscordSRV 1.24.0 / Towny 0.97.5.0)
and modern (DiscordSRV 1.30.5 / Towny 0.103.2.0) APIs are
included in the compilation matrix. Modern Towny 0.102+ requires
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
