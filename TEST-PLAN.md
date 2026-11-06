# Manual integration test plan — TownyDiscordChat 1.1.1

Perform all tests against an **isolated Discord guild**, a throwaway
Paper/Purpur server, and a disposable SQLite database or MySQL schema. Preserve a backup
of any prior TownyDiscordChat tables before migration.

## Configuration and player onboarding

- Leave all four `town/nation.*CategoryId` entries at `0`; verify
  that existing private categories are adopted by name, or new ones
  are created if absent. Do not fill IDs manually.
- Set an invalid numeric ID, an incorrect category name, or a public
  category. Confirm the Discord preflight blocks mutations and tells
  admins what to correct.
- Enter malformed YAML, missing MySQL credentials **with `storage.type: mysql`**, invalid
  role colors and bad queue bounds in separate test runs. Verify
  safe mode instead of stack traces and no SQL queue activity.
- Join while **not in a town** with no Discord link; confirm no
  linking prompt is displayed.
- Join a town while unlinked; confirm one reminder and then cooldown
  suppression for repeated joins.
- Run `/tdc status` and `/tdc link`. Verify the latter invokes
  DiscordSRV's normal linking flow (no TDC-owned codes); complete
  linking and verify the status changes and reconciliation is queued.
- Test `onboarding.enabled: false` and `remindOnTownJoin: false`.
- Check an admin joining during config safe mode sees an actionable
  message without repeated spam.

## Configuration backup and first-start migration

- On a stopped test server, rename an old valid `config.yml` to
  `config.yml.bak`, start the new JAR, and verify the plugin generated
  a new file with current template comments and defaults while
  preserving compatible MySQL credentials, category names, and color
  settings. The old backup must remain byte-for-byte unchanged.
- Add unknown historical `Discord.Api.RateLimit` settings to the
  backup. Verify they are skipped and their **key names** appear in
  the console; no secret values should be logged. The first import boot
  must remain in safe mode without Discord API calls or queue work.
  Confirm sync starts only after the new config is reviewed and the
  server is restarted.
- Set `storage.migrateToMySQL: true` in the old backup. Verify the new
  config contains `storage.migrateToMySQL: false` and does not launch a
  MySQL transfer implicitly.
- Leave an existing `config.yml` alongside the `.bak`. Confirm
  automatic migration does not run and the active file is unchanged.
- Corrupt the YAML, supply an invalid role color, or provide partial
  old MySQL credentials. Confirm migration fails closed, no new
  `config.yml` is written and no Discord sync or DB switching occurs.
- Confirm an absent `.bak` follows standard default installation.
  Test repeated startups after a successful migration; the old
  backup must not be imported again.

## Storage backend and migration verification

- Fresh install with `storage.type: auto` and blank `SQL` fields:
  SQLite file should be created, tables initialized, jobs executed
  without any external database server.
- Existing MySQL credentials with `storage.type: auto`: verify the
  installation continues using the original MySQL schema and does
  not silently create a new SQLite source of truth.
- Explicit `storage.type: mysql` with bad credentials: ensure
  startup fails closed; **never** fall back to an empty SQLite file.
- Queue revocation, creation, a DEAD job and a rename, then shut
  down. Set `storage.type: sqlite`, `storage.migrateToMySQL: true`
  and configure a fresh empty MySQL schema. Verify that **no
  Discord jobs run while the migration is active** and the console
  prints an exact table/row-count summary.
- Check that job IDs, status, attempts, timestamps, name mappings,
  Towny UUIDs, linked/managed role state, and audit checkpoints
  agree across both storage engines.
- Attempt migration to a nonempty MySQL schema: verify it aborts
  before modifying target rows. The SQLite database remains available.
- After successful copy, change to `storage.type: mysql`, set
  `storage.migrateToMySQL: false` and restart. Queue continuity
  must not create duplicate Discord resources.
- Keep the SQLite DB as a backup, and never run two plugin instances
  against the same guild on independent backends.

## Minecraft 1.21.11 and 26.x compatibility

- Verify Minecraft 1.21.11 on Paper/Purpur with Java 21 and Towny
  0.103.2.0, including DiscordSRV linking, roles and revocations.
- Verify Minecraft 26.1.2 and 26.2 on **Java 25**, using matching
  Paper/Purpur servers and Towny 0.103.2.0. Check the generated
  plugin.yml API version and database initialization.
- Verify Minecraft 26.3 on **Java 25** with a compatible Paper/Purpur
  server and Towny 0.103.2.15 pre-release. Test linking, private
  channel creation, resource renames and full-guild audits.
- Confirm unsupported 26.1.0 and 26.1.1 have no published artifacts.
- Verify all 14 JARs report version 1.1.1 and have matching
  Minecraft API declarations; test on an isolated Discord guild.

## Startup / version checks

- Install Towny + DiscordSRV supported for the selected Minecraft server.
- Enable the Discord bot's **Server Members Intent** in the developer portal
  and configure DiscordSRV to request `GUILD_MEMBERS`.
- Start with default SQLite or explicitly configured MySQL; confirm `/tdcqueue status`
  lists no unexpected DEAD jobs.
- Temporarily install an incompatible DiscordSRV in a *throwaway server*.
  Verify inert safe mode, clear console instructions, and admin messages.
- Check no Towny, DiscordSRV or JDA class is shaded in the test JAR.

## Console and Discord activity logging

- With `log.Enabled: true`, create a private text channel named
  `log.Channel.Name` under `log.Category.Name`. Confirm the log
  prints its destination and posts one startup confirmation message.
- Confirm `/tdcqueue status` displays synchronization readiness and
  whether Discord log delivery is enabled or unavailable.
- Create a new Towny role/channel and alter membership: console must
  distinguish `CREATED` and `GRANTED`/`REVOKED`; the actual changes
  must appear in the log channel after SQL acknowledgement.
- Restore a previously populated Discord guild. Existing resources
  should show `ADOPTED` in console but must **not** generate new Discord
  log messages or duplicate remote resources.
- Simulate missing, ambiguous, public, or unwritable logging channels.
  Queue jobs must still complete, while the console reports a delivery
  warning and `/tdcqueue status` reports a logging problem.
- Verify a Towny name with `@everyone` cannot ping members via Discord
  log messages, and long messages do not exceed Discord's message limit.
- Check text channel names such as `bradfordshire` versus
  `Bradfordshire` do not repeatedly enqueue `RENAME_TOWN_TEXT` jobs
  on every reconciliation. A no-op reports `SKIPPED` with its reason,
  not a misleading `missing Towny entity` error.

## Database/Discord state matrix

1. Empty database (SQLite or MySQL) + empty guild: establish one town and one nation and
   verify role/category/text/voice creation and correct permissions.
2. Empty database (SQLite or MySQL) + populated guild: reinstall the plugin with a new
   empty schema and existing managed guild resources. Confirm no duplicate
   roles/channels and that mapped Discord IDs are recovered.
3. Half-created Discord guild: manually remove one voice channel; verify
   only that channel is recreated, with no new role or duplicate category.
4. Stale SQL references: delete a mapped Discord role and channel after
   taking a backup. Check new Discord IDs are recorded after repair.
5. Pre-1.1.0 database schema: restore a 2023 backup with active `Expired='false'`
   resource rows, start this build, verify the IDs were preserved, and
   verify `tdc_schema_migrations` versions 2, 3, 4 and 5 and the preserved
   `tdc_queued_tasks` legacy table.

## Cross-module regression checks

- Rename a town while a linked member's targeted REST audit is pending.
  Verify the audit uses the original **Towny UUID and Discord role ID**,
  and does not revoke access solely because the Discord role still has
  its pre-rename name.
- Delay a Discord member lookup while kicking/unlinking the player.
  Verify the grant is **revalidated on the Bukkit thread after the
  REST lookup** and no stale membership permission is granted.
- Create a voice channel with the expected name and role override
  but allow `@everyone` to view it. Confirm it is NOT adopted,
  renamed, or deleted automatically, and the violation is reported.
  Repeat with a matching text channel and a public parent category.
- Duplicate a `town-...` role name. Verify the synchronizer refuses
  ambiguous adoption rather than choosing the first role.
- Legacy role adoption: assign ordinary server-level permissions to a
  uniquely named existing `town-...` role (e.g. Create Expressions,
  Create Public/Private Threads, Use Soundboard, Create Events).
  Confirm the plugin adopts its **existing role ID**, grants/revokes
  membership, and leaves all permission bits unchanged; do not create
  a duplicate Discord role. Verify channel overrides remain private.
- Add Administrator or Manage Roles (or a currently unknown permission
  bit) to a test town role. Confirm the worker refuses adoption and
  automatic member changes, with an actionable warning. Remove it,
  then verify pending jobs can complete after retry.
- Create two roles with the same `town-...` name, one safe and one
  privileged; verify the safe twin is not adopted by name. Verify
  the full-guild and targeted audits do not incorrectly treat the
  privileged twin as an adopted Towny role.
- Move the bot's highest role below a managed Towny role. Guild preflight
  should reject synchronization until its role hierarchy is corrected.
- Verify legacy `Expired='false'` or `Expired='0'` mapping rows
  normalize to active NULL (migration 5), while `Expired='true'`
  records and original queued-task history remain unchanged.
- Verify the unpacked plugin JAR's `plugin.yml` version matches the
  Maven version. The documentation consistency workflow must fail
  if its compatibility matrix or local Markdown links are stale.

## Stable identity / deletion

6. Record all town role/text/voice Discord snowflake IDs, rename the
   Towny town, and verify **all IDs remain identical** while their names
   change. Repeat for a nation and restart during a queued rename.
7. Rename twice rapidly. Verify only the latest Towny name remains;
   no extra roles/channels appear.
8. Delete a town. Verify its mapped role and channels are removed only
   after `queue.deletionGraceSeconds`. Categories must remain untouched.
9. Create a new town using a recently deleted town name while cleanup
   is pending. Confirm the old role is **not granted** to the new town.
10. Manually repurpose a managed Discord role with elevated permissions
    and confirm automatic mutation is refused and admins are warned.

## Membership / full-guild audit

11. Link a player, join town/nation, observe role grant and channel access.
12. Kick the player; verify a priority revoke is queued and processed
    ahead of membership grants, even if a channel creation is running.
13. Join → kick → rejoin quickly: verify the final role state equals
    *current* Towny membership; no stale action is acknowledged.
14. Unlink a player: verify managed town/nation access is revoked.
15. Have an unlinked Discord member hold a managed role while absent
    from JDA's cache. Execute `/tdcqueue audit`, confirm the full scan
    finds and revokes that role.
16. Disable `GUILD_MEMBERS`; verify the audit fails visibly with
    actionable warnings, while individual linked-account audits continue.
17. Force an API failure (e.g. remove the bot's Manage Roles permission),
    verify retries/DEAD tracking, restore permissions, then run
    `/tdcqueue retry-dead`.

## Delayed Discord callbacks, reuse and duplicate prevention

- Artificially delay a role/channel creation response past
  `queue.leaseSeconds`. Verify that only its **job ID** is quarantined:
  an unrelated deletion is still claimed, but the same expired creation
  is not dispatched a second time. Confirm admin warnings appear.
- Delay a role grant for one player past its lease, then kick a different
  player. Confirm the second player's revoke proceeds before the first
  player's hung grant resolves; the original grant ID is not reclaimed.
- Force repeated callbacks to remain unresolved until 16 membership or
  4 resource jobs are quarantined. Confirm the corresponding lane
  stops dispatching new work and warns the operator rather than
  increasing concurrency indefinitely.
- Let the first API callback finish late. Confirm the original Discord
  snowflake is persisted and that normal queue activity resumes.
- Restart after a request is known to have completed remotely but
  before its callback was saved. Once its lease expires, the worker
  should safely **adopt** the existing private role/channel rather
  than send a second create. Inspect the Discord guild for duplicates.
- Put two matching channels in managed categories, or give one
  matching channel an unsafe `@everyone` / role override. Confirm
  automatic adoption refuses the ambiguity/unsafe resource rather
  than creating a third object. Resolve manually and retry.
- Rename a Towny town/nation while an old-name CREATE request is
  still running. Check the stable UUID record remains on the old
  name until the callback is persisted, then moves to the new
  name and enqueues an in-place Discord rename with the original ID.
- Delete a Towny object, then create a different UUID using the
  old name. Confirm the old UUID's resources are cleaned up before
  granting the new entity's roles; no access passes to the new UUID.
- Delete a town while its last CREATE is running and there is no
  saved Discord ID yet. Verify the deleted UUID record is retained until
  the callback completes, then the orphan is adopted by verified ID
  and scheduled for deletion. A newly created town with the same name
  must never receive the old role.
- Remove a legacy resource-mapping row while retaining its stable
  UUID and Discord snowflake. Confirm the resource can be cleaned
  up using that stable ID without adopting another guild object.
  Restore backups after the test.
- Verify only one active REST-mutating scheduler remains. No legacy
  direct-event listener should perform unqueued role mutations.

## Crash, rate limits and shutdown

18. Stop the server while an operation is in flight; restart and ensure
    SQL recovery does not duplicate Discord objects.
19. Create many towns in the test database; ensure the queue drains
    without hard-coded Discord API limits, and that revoke jobs still
    take priority.
20. Shut down while DiscordSRV is unavailable. Restart and confirm
    pending work persists and recovers.

## Observability / read-only SQL diagnostics

Use a SQLite or MySQL client with your selected schema:

```sql
SELECT status,kind,COUNT(*) AS jobs
FROM tdc_discord_jobs GROUP BY status,kind;
SELECT status,desired,COUNT(*) AS intents
FROM tdc_member_intents GROUP BY status,desired;
SELECT * FROM tdc_schema_migrations ORDER BY version;
SELECT * FROM tdc_towny_identity ORDER BY kind,last_name LIMIT 100;
SELECT * FROM tdc_audit_runs ORDER BY last_success_at DESC;
```

Do not manually delete or edit pending/in-flight SQL rows during tests.
If a scenario produces unexpected access, save the logs, relevant IDs,
and redacted SQL status counts for diagnosis before production rollout.
