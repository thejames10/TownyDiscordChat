<p align="center">
   <a href="https://discord.gg/ynsCb2p3ac">
   <img width="500" height="362" src="https://townydiscordchat.com/logo_transparent_background.png">
   </a>
</p>
<p align="center">
   <a href="https://discord.gg/ynsCb2p3ac">
   <img src="https://discord.com/api/guilds/827698003208962099/widget.png">
   </a>
   <a href="https://github.com/thejames10/TownyDiscordChat/releases/latest">
   <img alt="GitHub release (latest by date including pre-releases)" src="https://img.shields.io/github/v/release/thejames10/TownyDiscordChat?color=blue&include_prereleases">
   </a>
   <a href="https://github.com/thejames10/TownyDiscordChat/issues">
   <img alt="GitHub issues" src="https://img.shields.io/github/issues/thejames10/TownyDiscordChat?color=orange">
   </a>
   <a href="https://github.com/thejames10/TownyDiscordChat/releases">
   <img src="https://img.shields.io/github/downloads/thejames10/TownyDiscordChat/total.svg?color=brightgreen">
   </a>
   <a href="https://github.com/thejames10/TownyDiscordChat/releases">
   <img alt="GitHub code size in bytes" src="https://img.shields.io/github/languages/code-size/thejames10/TownyDiscordChat">
   </a>
   <a href="https://github.com/thejames10/TownyDiscordChat/graphs/contributors">
   <img src="https://img.shields.io/github/contributors/thejames10/TownyDiscordChat.svg?color=brightgreen">
   </a>
   <a href="https://github.com/thejames10/TownyDiscordChat/blob/main/LICENSE">
   <img alt="GitHub license" src="https://img.shields.io/github/license/thejames10/TownyDiscordChat">
   </a>
</p>

## Compatibility

**Development version:** `TownyDiscordChat 1.1.0-SNAPSHOT` ([SQL branch](https://github.com/thejames10/TownyDiscordChat/tree/SQL)).

| Minecraft version | Server software | Towny version | DiscordSRV version |
| --- | --- | --- | --- |
| `1.16.5`, `1.17.1`, `1.18.2` | Spigot / Paper / Purpur | `0.100.3.0` | `1.30.5` |
| `1.19.4` | Paper / Purpur | `0.103.2.0` | `1.30.5` |
| `1.20.1`, `1.20.6` | Paper / Purpur | `0.103.2.0` | `1.30.5` |
| `1.21.1`, `1.21.4`, `1.21.8`, `1.21.10` | Paper / Purpur | `0.103.2.0` | `1.30.5` |

🧪 **Test builds:** These combinations have passed compilation, automated tests, and packaging checks. **Live server, SQLite/MySQL storage, and Discord permission testing is still required** before production support can be confirmed.

**Unsupported configurations:** Folia, and Spigot/CraftBukkit with Towny `0.102+`. These are not supported by the current plugin/server scheduler combination.

The table lists intended test configurations, not certified production support. Java requirements depend on the server and Towny release (`0.103.x` requires Java 17+). See [Towny releases](https://github.com/TownyAdvanced/Towny/releases), [DiscordSRV releases](https://github.com/DiscordSRV/DiscordSRV/releases), and [detailed compatibility notes](COMPATIBILITY.md).

### Downloading a test build

Open the [SQL branch builds on GitHub Actions](https://github.com/thejames10/TownyDiscordChat/actions?query=branch%3ASQL), select the latest **successful** compatibility workflow, and download the artifact named `TownyDiscordChat-MC-<your-version>`. Each ZIP contains the plugin JAR, configuration example, setup instructions, and `BUILD-INFO.txt` recording the exact compile-time dependencies.

**GitHub Actions artifacts are temporary**, so the README does not link to individual expiring artifact downloads. Once the test builds are ready for public release, [GitHub Releases](https://github.com/thejames10/TownyDiscordChat/releases) will provide permanent versioned downloads.

Earlier development runs included a separate `-modern` artifact. That suffix described compile-time APIs, not extra functionality. There is now one user-facing test package per Minecraft version. Towny and DiscordSRV are installed separately on the server.

Older TownyDiscordChat `1.0.x` releases do not include the SQL queue or migrations. Refer to each historical Git tag for the matching README.

## Installation

### 1. Prepare a test environment

Before your first SQL-branch installation, use an isolated Discord guild and a test Minecraft server. The plugin can **create, rename and delete** Towny-managed roles/channels and change access permissions. Back up both your database and your Discord guild's important settings before testing.

You need a compatible **Spigot/Paper/Purpur server** for the selected version (modern Towny: Paper/Purpur), **Towny**, **DiscordSRV**, and **local SQLite storage** (created automatically). MySQL is optional. Towny and DiscordSRV must be installed as separate plugins. They are **not packaged inside** TownyDiscordChat.

Official resources:
- [Towny installation and compatibility](https://github.com/TownyAdvanced/Towny)
- [DiscordSRV initial setup](https://docs.discordsrv.com/installation/initial-setup/)
- [DiscordSRV linking commands](https://docs.discordsrv.com/commands/)
- [Test build artifacts](https://github.com/thejames10/TownyDiscordChat/actions?query=branch%3ASQL) and [future tagged releases](https://github.com/thejames10/TownyDiscordChat/releases)

### 2. Set up DiscordSRV and the bot

Configure DiscordSRV's bot token and main guild using its [official guide](https://docs.discordsrv.com/installation/initial-setup/). Ensure the bot has **Manage Roles**, **Manage Channels**, and an appropriately high role in the Discord hierarchy. It must have access to the categories it creates and manages.

Enable Discord's privileged **Server Members Intent** in the bot's Developer Portal and ensure DiscordSRV requests it. This enables complete audits of linked and unlinked guild members. Without it, targeted linking/reconciliation can still work, but a complete unlinked-member audit cannot; the plugin will warn admins.

**Do not enable DiscordSRV's link-to-join gameplay requirement unless that is also your server policy.** TownyDiscordChat only needs linking for players who want private Towny Discord channel access.

### 3. Install the TownyDiscordChat build

Follow **Downloading a test build** above to locate the correct Minecraft-version ZIP in the latest successful SQL-branch workflow. Each ZIP includes `BUILD-INFO.txt` describing its compile-time dependency versions. Extract the ZIP, then copy **the plugin JAR, not the ZIP**, into `plugins/`.

**Before the first server start**, copy the ZIP's `config.example.yml` to `plugins/TownyDiscordChat/config.yml` (create the plugin directory if needed), then configure your Discord categories and storage. This prevents the plugin from creating Discord resources with unintended default names. **On upgrades**, keep and back up the existing config, then merge any new settings from the version-matched example.

### 4. Configure storage and categories

**No database server is required.** On a fresh installation, TownyDiscordChat
uses **SQLite** and creates `plugins/TownyDiscordChat/townydiscordchat.db`
automatically. The queue, Towny identity mappings, retries and audits all
use the selected storage backend.

In `plugins/TownyDiscordChat/config.yml`, the default is:

```yaml
storage:
  type: auto
  sqlite:
    file: "townydiscordchat.db"
  migrateToMySQL: false

SQL:
  host: "localhost"
  port: "3306"
  database: "blank"
  username: "blank"
  password: "blank"
```

**How selection works:**

- `auto` (default): if MySQL credentials have already been entered in
  the legacy `SQL` section, continue using **MySQL**. Otherwise use SQLite.
  Even partially configured real MySQL credentials cause a MySQL startup
  validation error rather than silently creating a different database.
- `sqlite`: always use the local `.db` file, even if MySQL credentials
  are present.
- `mysql`: require the configured MySQL server; startup checks the
  connection and enters safe mode if it is unavailable.

For MySQL, set `storage.type: mysql` and fill the existing
`SQL.host`, `SQL.port`, `SQL.database`, `SQL.username` and
`SQL.password`. **Do not switch a populated installation's database
type without migrating its data first.**

Keep your Discord category names configured as before:

```yaml
category:
  SubChannelsMax: 50
  Town:
    Name: "MINECRAFT-TOWN-CHAT"
  Nation:
    Name: "MINECRAFT-NATION-CHAT"
  Admin:
    Name: "MINECRAFT-STAFF"

town:
  TextCategoryId: 0
  VoiceCategoryId: 0
nation:
  TextCategoryId: 0
  VoiceCategoryId: 0
```

These snippets are examples, **not full replacement configuration files**.

**Category IDs can remain `0`.** That means automatic discovery by
`category.Town.Name` and `category.Nation.Name`; verified private
categories are reused and missing ones created. Nonzero legacy category IDs
are verified for matching name/privacy before syncing.

The legacy `messages.DiscordLogTextChannelId: 0` is also allowed.
`log.Category.Name` and `log.Channel.Name` are retained for backward
compatibility but the old Discord log-channel scheduler is **not active**
in the SQL branch; queue/API diagnostics go to the server console.
DiscordSRV/JDA handles actual Discord rate-limit responses, while the
durable SQLite or MySQL queue handles retries and job priority.

#### Switching from SQLite to MySQL

A direct copy is more efficient and safer for large Discord guilds
than rebuilding all mappings through Discord API discovery.

1. **Stop the server and back up** `townydiscordchat.db` (include its
   `-wal` and `-shm` files if present) and your configuration.
2. Create a **new, empty MySQL schema**. Migration refuses any
   pre-existing populated `tdc_*` tables and never merges databases.
3. Keep `storage.type: sqlite`, set
   `storage.migrateToMySQL: true`, and fill in the target MySQL
   credentials in `SQL`.
4. Start the server. TownyDiscordChat enters **migration-only mode**:
   no Discord synchronization, queue processing, or API calls run.
   When the console reports the copied table/row counts and success,
   stop the server again.
5. Set `storage.type: mysql` and
   `storage.migrateToMySQL: false`, then restart the server.
   Check `/tdcqueue status` and reconcile against Discord normally.

The entire active database is copied, including old SQL mapping tables,
stable Towny UUIDs, queued and failed operations, membership revisions and
audit checkpoints. SQLite remains unchanged as a fallback backup.
Rows are copied transactionally and checked for count consistency; MySQL may retain empty newly created tables if copying fails. The migration
aborts if the target already has plugin data. **Do not operate both backends
as independent servers after migration.** See [storage migration details](SYNC-MIGRATION.md).

### 5. Restart and verify startup

At startup, TownyDiscordChat validates YAML syntax, selected storage settings, category names, role colors, time zones, queue limits, and plugin compatibility **before** beginning synchronization. When DiscordSRV's guild connects, it checks the guild/category privacy and bot permissions.

- **Valid configuration:** synchronization starts and adoption/creation proceeds automatically.
- **Invalid configuration:** no database-backed queue is started; the plugin reports safe mode in console and to administrators joining the server.
- **Discord not connected yet:** synchronization stays gated until the guild is available and validated.
- **Unsafe or ambiguous categories:** sync is suspended; correct category privacy/names before restarting.

From the console or as an administrator, check:

```text
/tdcqueue status
/tdcqueue audit
```

`/tdcqueue audit` requests a full-guild reconciliation of managed role membership. It is not necessary to run the old sequence of four `/tdc Check ...` commands on first installation.

## Players: linking Minecraft and Discord

TownyDiscordChat **does not maintain another linking database or issue its own verification codes**. DiscordSRV is the source of truth for account linking.

1. **Join a Towny town.** If you're not in a town, you don't need to link just for TownyDiscordChat.
2. Use **`/tdc status`** to see your town/nation and whether DiscordSRV has linked your Minecraft account.
3. If you're unlinked, run **`/tdc link`**, which delegates to DiscordSRV's built-in **`/discord link`**. Follow DiscordSRV's instructions to finish linking.
4. Your linked account's town/nation roles and private channel access will synchronize through the durable queue. It may take a short time; `/discord linked` confirms the DiscordSRV connection.

Players in a town can receive a **configurable, cooldown-limited reminder** when joining the Minecraft server or a town. Players outside towns are not prompted. Linking/unlinking events trigger reconciliation, and DiscordSRV's existing command and permissions continue to control identity.

We do **not** recommend enabling DiscordSRV's [link-to-join requirement](https://docs.discordsrv.com/installation/link-to-join-setup/) just to use TownyDiscordChat: that separate option may prevent unlinked players from joining Minecraft at all.

## Commands and permissions

| Command | Who can use it | Purpose |
| --- | --- | --- |
| `/tdc` | Players | Plugin information and linking status |
| `/tdc status` | Players | See Towny membership and DiscordSRV linking status |
| `/tdc link` | Players with `discordsrv.link` | Launch DiscordSRV's own account linking |
| `/discord linked` | Players with `discordsrv.linked` | Verify your link using DiscordSRV |
| `/tdcqueue status` | Admins (`TownyDiscordChat.Admin`) | View durable queue and membership intent counts |
| `/tdcqueue audit` | Admins | Request a full guild audit; requires Server Members Intent |
| `/tdcqueue retry-dead` | Admins | Retry failed jobs after fixing the underlying issue |
| `/tdc Check ...` | Authorized admins / legacy permissions | Request reconciliation; not required for initial setup |

`TownyDiscordChat.Admin` defaults to server operators. Standard players do not need administrative permissions for the linking flow. DiscordSRV controls its own linking permission nodes.

## How syncing and recovery work

The [database-backed two-lane dispatcher](SYNC-MIGRATION.md) remembers intended membership and Discord operations across restarts. **Revocations take priority over grants**, while verified deletions precede new resource creation. JDA handles Discord's real rate limits; the durable queue handles retries, backoff, deduplication and crash recovery.

Towny UUIDs identify towns/nations even when their names change. Existing Discord roles/channels are renamed in place, preserving their IDs. An empty local database is not assumed to mean an empty Discord guild; startup checks and safely adopts verified resources instead of recreating everything. Legacy SQL rows are migrated without deleting historic tables; SQLite can be copied directly into MySQL without Discord API reconstruction.

**Security note:** Discord access cannot be revoked instantaneously if the bot is offline, permission-blocked, or waiting behind Discord API rate limits. Investigate any failed revocations immediately.

### Late Discord responses and restart recovery

A Discord REST operation can exceed the configured SQL lease even while
the original request is still in progress. Instead of reissuing that
request, TownyDiscordChat **quarantines its exact job ID**. Other queued
operations — including urgent member revocations and verified resource
deletions — can continue through the independent dispatcher, while the
quarantined ID remains ineligible for reclaim in the same process.

Concurrency is deliberately bounded to at most **16 unresolved member
operations** or **4 unresolved resource operations**. At the limit, the
affected queue stops accepting additional work and warns admins to
inspect DiscordSRV/Discord before restarting. JDA still manages all
Discord HTTP rate limits. The original callback can eventually clear
the quarantine and persist its result.

On restart, unfinished callbacks from the previous process cannot
execute, so expired DB leases are eligible for recovery. The worker
first checks for uniquely identifiable, safe Discord resources before
trying to create them again. Ambiguous or publicly accessible channels
are never adopted or duplicated automatically. Stable Towny UUIDs
protect renamed and name-reused entities, and incomplete legacy SQL
mappings are repaired conservatively.

**No automatic system can guarantee exactly-once REST side effects
across a crash.** Resolve any reported ambiguous resource manually,
then use `/tdcqueue retry-dead` when appropriate. Never operate two
plugin installations against the same guild using independent databases.

## Troubleshooting

- **SAFE MODE: invalid config.yml:** inspect the logged setting names. Correct the file and restart; queue jobs won't run until startup validation succeeds.
- **Category ID is 0:** expected; automatic discovery is enabled. Don't look up an ID unless you have a specific reason.
- **Guild preflight rejected a category:** ensure the target category denies `View Channel` to `@everyone`, and that the Town/Nation category names are distinct.
- **Bot can't grant or revoke roles:** grant Manage Roles, place its role above managed Towny roles, and verify it has Manage Channels.
- **Player is linked but cannot see a town channel:** check that they belong to a town, run `/tdc status`, and inspect `/tdcqueue status`. Queue jobs may be pending or dead.
- **Full guild audit fails:** enable Server Members Intent in both the bot's Developer Portal and DiscordSRV, then run `/tdcqueue audit` again.
- **After reinstall or migration:** restore a database backup if needed; keep the original `tdc_queued_tasks` table. If switching databases, follow the offline SQLite → MySQL process above, not manual config-only switching. See [migration details](SYNC-MIGRATION.md) and the [test plan](TEST-PLAN.md).

For reproducible issues, attach **redacted** startup logs, your Minecraft version/server software, Towny and DiscordSRV versions, TownyDiscordChat version, and the queue status. Never publish database credentials or Discord bot tokens.

## Documentation for older releases

Each release tag preserves the **exact README, configuration defaults, dependency matrix and migration instructions from that release**. To see instructions for an installed version, open its [GitHub release](https://github.com/thejames10/TownyDiscordChat/releases), select its tag, and view `README.md` from that tag (for example `blob/v1.1.0/README.md` **after** that version is released). Installation ZIPs also include a snapshot of the README and relevant setup guides.

We intentionally do **not** maintain separate editable copies of every historical README: tags are immutable release snapshots, which prevents the documentation for older versions changing when the latest README is updated.

Useful references: [Compatibility and artifact details](COMPATIBILITY.md) · [SQL migration and security](SYNC-MIGRATION.md) · [Manual integration test plan](TEST-PLAN.md) · [Towny](https://github.com/TownyAdvanced/Towny) · [DiscordSRV](https://github.com/DiscordSRV/DiscordSRV).
