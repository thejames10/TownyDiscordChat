# TownyDiscordChat synchronization and migration

## Design
Towny membership is authoritative. Discord reports actual guild resources
and membership. SQL persists IDs, desired membership and in-flight operations.
DiscordSRV/JDA handles HTTP 429 response scheduling; the plugin retains its
own durable, two-lane operation queue, callbacks, leases and retries. A
membership revocation is not blocked by a slow channel creation.

For memberships, one row per (guild,member,role) expresses the **latest**
desired state. A rapid ADD / REMOVE / ADD collapses to the latest state.
Revisions fence stale callbacks. Revocations are claimed before creation
and new grants. This does not guarantee immediate access removal during
a Discord outage or if bot permissions prevent removal.

A job is complete only once an API result (or verified already-matching
remote state) has been durably acknowledged. Failed jobs back off, dead
jobs persist for operator attention. In-flight tasks have leases; a
server restart allows retry after lease expiry. Discord REST mutations
are not transactionally atomic with SQL; retries check Discord state
before creating resources or changing membership.

## Three-way startup reconciliation
- Empty database, populated Discord: adopt matching roles, verified
  channels with expected category and role permissions, and existing
  categories. Save observed grants for linked users.
- Populated database, empty Discord: recreate missing roles and channels
  after comparing actual IDs to SQL mappings.
- Partial state: repair only mismatched resources.
- Discord and SQL disagree: remote IDs are checked and verified before
  reusing them. Names alone are insufficient for channel ownership.
- Role assignments: linked players' expected memberships come from
  current Towny state. Persisted grants enable revocation after kicks,
  leave events, relinks, and restarts.
- The DiscordSRV member cache may be incomplete; the dispatcher fetches
  uncached members by REST rather than assuming their role is absent.

**Known remaining limitations:** verified SQL-owned orphan roles/channels
are deleted when the corresponding Towny entity disappears. Categories
are deliberately NOT deleted automatically, as they may contain unrelated
channels. Renames currently take the delete-and-create path, which can
cause temporary access loss; stable Towny UUID identity will improve this.
There is no full-guild audit for unlinked users without SQL history.
The plugin performs rate-limited REST audits for linked members on
startup and when JDA's member cache is missing entries. Newer DiscordSRV
integrations using unshaded JDA require a separate adapter.

## Non-destructive database migration
Back up your entire schema first. `tdc_schema_migrations` tracks the
conversion of the original SQL branch's `tdc_queued_tasks` into
`tdc_discord_jobs`. The original table and SQL mapping tables are
never dropped in this migration. `tdc_member_intents` stores desired
membership with revisions and priorities; `tdc_managed_member_roles`
stores previously observed/granted memberships. Creating either table
does not rewrite existing role mappings. Migration version 2 is
idempotent, but operators should validate historic mappings using a
test guild before running production reconciliation.

## Scheduling and prioritization
1. Membership revocations (priority 0, membership lane)
2. Verified SQL-owned role/channel deletions (priority 10, resource lane)
3. Resource creation, adoption and REST membership audits (priority 100, resource lane)
4. Membership grants (priority 100, membership lane, only after revocations)

A slow resource mutation does not block membership revocations. Each
lane submits one request at a time, while JDA handles actual Discord
API buckets and HTTP 429 responses. Older in-flight callbacks are fenced
with SQL revisions; the newest desired membership wins.

## Safe-mode compatibility
On startup the plugin checks for enabled Towny and DiscordSRV,
minimum legacy versions, and key shaded-JDA methods. When incompatible,
it does not start SQL jobs or Discord operations. The console receives
a warning and admins receive a message on join. Version minimums and
API shape are prerequisites, not a certified support guarantee.

## Testing
Use an isolated guild and backed-up MySQL schema. Test empty DB,
empty guild, partial discord resources, stale IDs, server crash between
Discord creation and SQL acknowledgement, linked/unlinked membership,
kicks, rapid join/kick/join, missing permissions, unknown members, offline
Discord and sustained rate limiting. Examine `/tdcqueue status`
and use `/tdcqueue retry-dead` only after fixing the cause.

## Discord ownership safety
A guild role must be uniquely identified by name and must not have guild-
wide permissions before TDC can adopt it or grant it to a player.
Categories must deny VIEW_CHANNEL to @everyone. Channels must have the
expected parent category, the matching role permission override, and a
VIEW_CHANNEL denial for @everyone. Resource deletions require an exact
stored Discord ID and matching name/parent; mismatched or repurposed
resources are retained and reported for manual review.

## Limits of the version check
DiscordSRV 1.24.0 and Towny 0.97.5.0 are historical **minimum API
baselines**, not a promise of support on the latest Minecraft versions.
The guard also checks for specific shaded JDA methods. A modern
DiscordSRV version may expose an incompatible JDA package and trigger
safe mode despite passing the numeric version test. Use an integration
adapter and tested per-version dependency matrix for official releases.
