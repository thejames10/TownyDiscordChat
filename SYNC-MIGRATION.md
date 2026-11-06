# TownyDiscordChat synchronization and migration

## Design
Towny membership is authoritative. Discord reports actual guild resources
and membership. SQL persists IDs, desired membership and in-flight operations.
DiscordSRV/JDA handles HTTP 429 response scheduling; the plugin retains its
own durable, single-flight operation queue, callbacks, leases and retries.

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

**Known remaining limitations:** the currently implemented scan does not
automatically delete orphaned Discord guild roles/channels when a Towny
town or nation is deleted, nor rename them automatically. A prefix is
not sufficient proof of ownership for destructive deletion. There is
not yet a bulk complete-guild membership audit for unlinked users, and
a guild with no retained SQL records and incomplete member cache cannot
infer all prior grants. Newer DiscordSRV integrations using unshaded
JDA require a separate adapter.

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
1. Revocations (priority 0, up to one per active worker)
2. Resource creation and adoption
3. Membership grants (priority 100)
Both membership directions use durable retries and lease-based
recovery; DiscordSRV's JDA enforces actual API limits.

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
