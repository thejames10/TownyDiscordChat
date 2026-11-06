# Manual integration test plan (SQL branch)

Perform all tests against an **isolated Discord guild**, a throwaway
Paper/Purpur server, and a disposable MySQL schema. Preserve a backup
of any prior TownyDiscordChat tables before migration.

## Startup / version checks

- Install Towny + DiscordSRV supported for the selected Minecraft server.
- Enable the Discord bot's **Server Members Intent** in the developer portal
  and configure DiscordSRV to request `GUILD_MEMBERS`.
- Start with plugin + MySQL correctly configured; confirm `/tdcqueue status`
  lists no unexpected DEAD jobs.
- Temporarily install an incompatible DiscordSRV in a *throwaway server*.
  Verify inert safe mode, clear console instructions, and admin messages.
- Check no Towny, DiscordSRV or JDA class is shaded in the test JAR.

## Database/Discord state matrix

1. Empty MySQL + empty guild: establish one town and one nation and
   verify role/category/text/voice creation and correct permissions.
2. Empty MySQL + populated guild: reinstall the plugin with a new
   empty schema and existing managed guild resources. Confirm no duplicate
   roles/channels and that mapped Discord IDs are recovered.
3. Half-created Discord guild: manually remove one voice channel; verify
   only that channel is recreated, with no new role or duplicate category.
4. Stale SQL references: delete a mapped Discord role and channel after
   taking a backup. Check new Discord IDs are recorded after repair.
5. Old SQL branch schema: restore a 2023 backup, start this build,
   verify `tdc_schema_migrations` versions 2, 3 and 4 and the preserved
   `tdc_queued_tasks` legacy table.

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

## Crash, rate limits and shutdown

18. Stop the server while an operation is in flight; restart and ensure
    SQL recovery does not duplicate Discord objects.
19. Create many towns in the test database; ensure the queue drains
    without hard-coded Discord API limits, and that revoke jobs still
    take priority.
20. Shut down while DiscordSRV is unavailable. Restart and confirm
    pending work persists and recovers.

## Observability / read-only SQL diagnostics

Use a MySQL client with your selected schema:

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
