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
   <img alt="GitHub release downloads" src="https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2Fthejames10%2FTownyDiscordChat%2Fbadges%2Fdownloads.json">
   </a>
   <a href="https://github.com/thejames10/TownyDiscordChat/releases">
   <img alt="GitHub code size in bytes" src="https://img.shields.io/github/languages/code-size/thejames10/TownyDiscordChat">
   </a>
   <a href="https://github.com/thejames10/TownyDiscordChat/graphs/contributors">
   <img alt="GitHub contributors" src="https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2Fthejames10%2FTownyDiscordChat%2Fbadges%2Fcontributors.json">
   </a>
   <a href="https://github.com/thejames10/TownyDiscordChat/blob/main/LICENSE">
   <img alt="GitHub license" src="https://img.shields.io/github/license/thejames10/TownyDiscordChat">
   </a>
</p>


## Download TownyDiscordChat

**Current version: 1.1.2 (pre-release).** It has one JAR per Minecraft version; choose the one for your server. From the next release, a single JAR covers every [supported version](#compatibility).

**[Download on Hangar](https://hangar.papermc.io/thejames10/TownyDiscordChat/versions)** · **[Download from GitHub](https://github.com/thejames10/TownyDiscordChat/releases/tag/v1.1.2)** · [All releases](https://github.com/thejames10/TownyDiscordChat/releases) · [Compatibility](#compatibility)

Towny and DiscordSRV are separate required plugins. See [Installation](#installation) before deploying.

---

## What is TownyDiscordChat?

**TownyDiscordChat automatically gives Minecraft towns and nations their own private spaces in Discord.** It's a server-side plugin that connects [Towny](https://github.com/TownyAdvanced/Towny) (which knows who belongs to each Minecraft town and nation) with [DiscordSRV](https://github.com/DiscordSRV/DiscordSRV) (which links Minecraft players to their Discord accounts).

Instead of manually creating Discord roles, organizing private channels and updating access every time a player changes towns, **TownyDiscordChat keeps your Discord server aligned with your Towny server**.

### Why use it?

On an active Towny server, membership changes constantly. Players join towns, move to other towns, form nations, rename settlements and sometimes leave entirely. Maintaining corresponding Discord roles and private discussion channels by hand is time-consuming, and outdated roles can leave people with access to conversations they should no longer see.

TownyDiscordChat solves that administrative problem by using **Towny membership as the source of truth** for Discord access. DiscordSRV handles account linking; TownyDiscordChat manages the town- and nation-specific Discord resources and permissions.

### What does it do?

- **Town and nation roles:** Creates and maintains Discord roles such as `town-Oakvale` and `nation-Northlands` for your Towny communities.
- **Private text and voice channels:** Organizes town and nation channels inside configured Discord categories, with access restricted to the corresponding roles.
- **Automatic membership changes:** Grants roles when linked players join a town or nation, and revokes outdated access when they leave or change membership.
- **Renames and cleanup:** Renames existing managed Discord resources when Towny names change, preserving their IDs; carefully handles deletion and name reuse.
- **Simple linking:** Uses DiscordSRV's existing Minecraft ↔ Discord account links, with `/tdc status`, `/tdc link` and optional in-game reminders for players.
- **Recovery and diagnostics:** Saves synchronization work in SQLite (the default) or MySQL, retries interrupted operations, and provides queue status, audits and activity logs for administrators.
- **Admin tools:** `/tdc reload` applies config and message changes without a restart, `/tdc debug` writes a support report with credentials removed, and the plugin tells admins when a new release is out.
- **Optional extras**, all off by default: a [TownyChat bridge](#townychat-bridge) for town and nation chat, [moderation for mayors and kings](#moderation-for-town-and-nation-leaders) in their own channels, and [archiving](#archiving-deleted-town-channels) a deleted town's text channel instead of deleting it.
- **Paper, Purpur and Folia:** one JAR for every supported version. On Folia, the plugin's work runs on Folia's global and async schedulers.

### Example: a player joins a town

Suppose your Minecraft server has a town called **Oakvale**, which belongs to the nation **Northlands**.

1. TownyDiscordChat maintains an `town-Oakvale` role and Oakvale's private text/voice channels, plus the corresponding nation role and channels.
2. A player joins Oakvale in Minecraft and links their Discord account through DiscordSRV.
3. The plugin assigns the appropriate town and nation roles. The player can now access their community's private Discord channels.
4. If the player moves to another town or leaves the nation, the plugin updates their roles and removes access they no longer qualify for.

**What it doesn't do:** TownyDiscordChat is **not** a replacement for Towny or DiscordSRV. DiscordSRV still handles global Minecraft ↔ Discord chat. This plugin focuses on **who gets access to which private Discord spaces**, and on keeping those spaces in sync with Towny. Its optional [TownyChat bridge](#townychat-bridge) only covers town and nation chat.

**Get started:** [Compatibility and downloads](#compatibility) · [Installation](#installation) · [Player linking](#players-linking-minecraft-and-discord) · [Commands](#commands-and-permissions)

## Compatibility

**Current release:** `TownyDiscordChat 1.1.2` — [Pre-Release Build 1.1.2](https://github.com/thejames10/TownyDiscordChat/releases/tag/v1.1.2).

| Minecraft version | Server software | Towny version | DiscordSRV version | Java |
| --- | --- | --- | --- | --- |
| `1.16.5` | Spigot / CraftBukkit / Paper / Purpur | `0.100.3.0` on Paper; `0.100.1.0` on Spigot, CraftBukkit and Purpur | `1.30.5` | Server-dependent |
| `1.17.1`, `1.18.2` | Spigot / CraftBukkit / Paper / Purpur | `0.100.3.0` | `1.30.5` | Server-dependent |
| `1.19.4` | Paper / Purpur | `0.103.2.0` | `1.30.5` | Server-dependent |
| `1.20.1`, `1.20.6` | Paper / Purpur | `0.103.2.0` | `1.30.5` | Server-dependent |
| `1.21.1`, `1.21.4`, `1.21.8`, `1.21.10`, `1.21.11` | Paper / Purpur | `0.103.2.0` | `1.30.5` | 21+ |
| `26.1.2`, `26.2` | Paper / Purpur | `0.103.2.0` | `1.30.5` | 25+ |
| `26.3` | Paper / Purpur | `0.103.2.15` (pre-release) | `1.30.5` | 25+ |

**Why 1.16.5 has two Towny versions:** [Towny 0.100.1.0](https://github.com/TownyAdvanced/Towny/releases/tag/0.100.1.0) is the newest release that starts on Spigot, CraftBukkit and Purpur 1.16.5. Newer Towny releases need libraries those servers do not include (log4j 2.11+ for 0.100.3.0, commons-io for 0.100.2.0) and fail to enable there. Paper 1.16.5 includes both, so it can use 0.100.3.0.

**Compatibility status:** One plugin JAR is built against the oldest listed server and Towny APIs. Automated tests compile the code against every listed version and check that each call the JAR makes still exists in that version's server, Towny and DiscordSRV APIs. Before release, that same JAR is also started on real servers for each listed version and server type, on its own, alongside Towny and alongside DiscordSRV; Discord role and channel management is tested against a test Discord server on Paper 26.3. This is not a guarantee for every server configuration. Minecraft 26.3 requires a compatible Towny pre-release and a matching Paper/Purpur build.

**Folia** is supported from the next release, on the versions Folia publishes builds for, with Towny and DiscordSRV releases that support Folia. TownyDiscordChat 1.1.2 and earlier do not run on Folia.

**Unsupported:** Spigot/CraftBukkit with Towny 0.102+; Minecraft 26.1.0 (no Paper build) and 26.1.1 (obsolete Paper alpha builds). Use **26.1.2** for the 26.1 series. Minecraft 26.4 has not been released as stable.

Minecraft 26.x requires Java 25 and Paper's newer build-qualified API versions. Older Minecraft versions follow their own server, Towny and DiscordSRV Java requirements. The plugin JAR targets Java 8 bytecode for an older-server baseline. See [Paper development requirements](https://docs.papermc.io/paper/dev/project-setup/), [Towny releases](https://github.com/TownyAdvanced/Towny/releases), [DiscordSRV releases](https://github.com/DiscordSRV/DiscordSRV/releases), and [detailed compatibility notes](COMPATIBILITY.md).

### Download TownyDiscordChat 1.1.2

Download from the
[1.1.2 pre-release](https://github.com/thejames10/TownyDiscordChat/releases/tag/v1.1.2).
1.1.2 has one `TownyDiscordChat-MC-<version>.jar` per listed Minecraft
version; choose the one matching your server. Its optional matching ZIP
contains the same JAR and `config.example.yml`.

From the next release, each release has one `TownyDiscordChat-<version>.jar`
for every listed Minecraft version and server type, plus
`config.example.yml`.

Documentation is version-specific. Earlier versions,
including [1.1.0](https://github.com/thejames10/TownyDiscordChat/releases/tag/v1.1.0),
remain available. Towny and DiscordSRV must be installed separately.

Before upgrading, read the
[config upgrade instructions](#automatic-config-upgrade-from-configymlbak).

## Installation

### 1. Prepare a test environment

Before installing the 1.1.2 pre-release, use an isolated Discord guild and a test Minecraft server. The plugin can **create, rename and delete** Towny-managed roles/channels and change access permissions. Back up both your database and your Discord guild's important settings before testing.

You need a compatible **Spigot/CraftBukkit/Paper/Purpur server** for the selected version (Towny 0.102+: Paper/Purpur), **Towny**, **DiscordSRV**, and **local SQLite storage** (created automatically). MySQL is optional. Towny and DiscordSRV must be installed as separate plugins. They are **not packaged inside** TownyDiscordChat.

**Startup preflight:** TownyDiscordChat checks that both Towny and DiscordSRV are
installed, enabled, and compatible *before* reading or migrating its configuration
or selecting a storage backend. When a dependency is missing or incompatible,
the plugin enters an inert safe mode and does not create or modify
`config.yml`, connect to a database, or start Discord synchronization.
Existing configuration and database files are preserved. Install or correct
the dependencies and restart to continue normal configuration initialization.
[Anonymous statistics](#anonymous-statistics) still start in safe mode, so the
safe-mode status is counted unless you have opted out.

Official resources:
- [Towny installation and compatibility](https://github.com/TownyAdvanced/Towny)
- [DiscordSRV initial setup](https://docs.discordsrv.com/installation/initial-setup/)
- [DiscordSRV linking commands](https://docs.discordsrv.com/commands/)
- [TownyDiscordChat 1.1.2 downloads](https://github.com/thejames10/TownyDiscordChat/releases/tag/v1.1.2)

### 2. Set up DiscordSRV and the bot

Configure DiscordSRV's bot token and main guild using its [official guide](https://docs.discordsrv.com/installation/initial-setup/). Ensure the bot has **Manage Roles**, **Manage Channels**, and an appropriately high role in the Discord hierarchy. It must have access to the categories it creates and manages.

Enable Discord's privileged **Server Members Intent** in the bot's Developer Portal and ensure DiscordSRV requests it. This enables complete audits of linked and unlinked guild members. Without it, targeted linking/reconciliation can still work, but a complete unlinked-member audit cannot; the plugin will warn admins.

**Do not enable DiscordSRV's link-to-join gameplay requirement unless that is also your server policy.** TownyDiscordChat only needs linking for players who want private Towny Discord channel access.

### 3. Install the TownyDiscordChat build

Follow **Download TownyDiscordChat 1.1.2** above. Install **only the plugin JAR** into `plugins/`.

**For a new installation:** before the first server start, copy the release's
`config.example.yml` to `plugins/TownyDiscordChat/config.yml` (create
the plugin directory if needed), then configure your Discord categories
and storage. This avoids creating Discord resources with unintended
default names.

**For an upgrade using a previous configuration:** stop the server, back
up the plugin's data/database, and rename your existing
`plugins/TownyDiscordChat/config.yml` to `config.yml.bak` in **that same
directory**. On the next startup, TownyDiscordChat creates a fresh
configuration from the bundled defaults, imports compatible settings
from the backup, and retains the backup untouched. See
[Automatic config upgrade](#automatic-config-upgrade-from-configymlbak)
before performing this step on a populated server.

### Automatic config upgrade from `config.yml.bak`

This is an **opt-in, first-start-only** configuration upgrade: it runs
when **`config.yml` does not exist** but `config.yml.bak` is present
in `plugins/TownyDiscordChat/`. If both files are present, the plugin
loads `config.yml` as-is and **does not** apply the backup. If neither
file exists, it generates the standard default config.

1. **Stop your server.** Back up the plugin's entire directory, including
   its SQLite file (and WAL files if present), or back up the existing
   MySQL database. Rename `config.yml` to `config.yml.bak` in the
   same plugin directory. Keep a separate copy outside that directory
   as an additional backup if needed.
2. Install the SQL-branch JAR and start the server. The plugin reads
   the backup, generates the current `config.yml` with its **new
   settings, defaults and comments**, and overlays compatible old
   values for categories, role creation, messages, database connection,
   queue and other still-supported options. **This first startup stays
   in safe mode, without Discord synchronization or queue processing.**
3. **Read the console's `[CONFIG UPGRADE]` lines** for the number
   of imported settings and the names of ignored keys. Unknown and
   discontinued settings (such as the old manually guessed Discord API
   rate-limit configuration) are not carried forward.
4. Review the new `config.yml`, especially `storage.type`, `SQL.*`,
   private category names/IDs, role colors, and queue settings. Stop
   the server and edit any settings that need changing. **Restart once
   more to activate synchronization** using the reviewed config.

**Safety rules:** Existing `config.yml` is never overwritten, and the
`.bak` is never deleted or modified. Invalid YAML, unsupported
types, or incompatible values cause startup **safe mode**, without
publishing a partially migrated config. Correct the backup and restart,
or manually create `config.yml` from the release's `config.example.yml`.
The migration never copies unknown options or automatically starts
`storage.migrateToMySQL`; that flag resets to `false`.

An old configuration containing real `SQL.database` / MySQL
credentials continues on **MySQL** through the new `storage.type: auto`
default. Missing/invalid MySQL credentials cause safe mode rather
than silently starting an empty SQLite database. **This process only
upgrades YAML settings:** it does not move database records between
SQLite and MySQL, nor does it rebuild Discord resources. Follow the
separate [SQLite → MySQL migration](#switching-from-sqlite-to-mysql)
if you intend to change storage backends.

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

The legacy `messages.DiscordLogTextChannelId: 0` is also allowed, but its
numeric ID is **ignored**. The new SQL activity logger uses
`log.Enabled: true` (the default), `log.Category.Name` and
`log.Channel.Name` to locate **one existing private Discord text channel**.
For example, `MINECRAFT-STAFF` → `#townydiscordchat-logs`. The channel
or its parent category must deny `@everyone` View Channel and the bot must
be able to view/send messages in the channel. Nothing is auto-created or
made public for logging. Look for `[DISCORD LOG] Sending changes...` at
startup and `Discord activity log: enabled` in `/tdcqueue status`.

The new logger posts a startup confirmation, confirmed Discord resource
**creations/renames/deletions**, membership **grants/revocations**, and queue
failures/admin warnings. Adopting an existing role, redundant no-ops,
and periodic reconciliation do **not** send Discord messages, avoiding
flooding on reinstall. All activity still appears in the Minecraft
console with explicit `[QUEUE] CREATED`, `ADOPTED`, `RENAMED`,
`DELETED`, `GRANTED`, `REVOKED`, `SKIPPED` and `FAILED` labels.
Discord logging is best-effort (JDA async delivery), not a second durable
queue; a failure to post is warned in console without stopping sync.
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

## Optional features

Each feature below is off by default and is switched on in `config.yml`. `/tdc reload` applies the change without a restart.

### TownyChat bridge

`chatBridge` relays [TownyChat](https://github.com/TownyAdvanced/TownyChat)'s town and nation channels to the matching private Discord text channel, and Discord messages back to the town or nation members who are online.

1. Install TownyChat. In its `channels.yml`, add `hooked: true` to the town and nation channels. TownyChat only shares messages from hooked channels.
2. Set `chatBridge.enabled: true`. `chatBridge.town` and `chatBridge.nation` choose which channels are relayed.
3. By default messages are posted through DiscordSRV's webhooks, with the player's name and skin. The bot then needs **Manage Webhooks**. With `chatBridge.useWebhooks: false`, the bot posts the message itself using `chatBridge.toDiscordFormat`.

A Discord message reaches the game only if its author is linked through DiscordSRV and belongs to that town or nation right now, so a stale Discord role can't speak in town chat. Colour codes, Markdown and `@` mentions are neutralised in both directions, and messages longer than `chatBridge.maxMessageLength` are cut short. The in-game line is `chat-bridge.from-discord` in `messages.yml`.

### Moderation for town and nation leaders

With `ranks.enabled: true`, mayors and kings can moderate their own channels. Text channels get **Manage Messages**. Voice channels get **Mute**, **Deafen** and **Move Members**. To include more Towny ranks, list them under `ranks.town` and `ranks.nation` (rank names come from Towny's `townyperms.yml`; `assistant` is listed by default).

The bot needs those four permissions itself, because Discord only lets it grant permissions it has. The plugin records every permission it grants and only ever removes those, so moderation an admin set by hand is left alone, and an explicit deny on a member is respected. When a player loses their rank or leaves, or the feature is switched off, the recorded permissions are removed.

### Archiving deleted town channels

With `archive.enabled: true`, a deleted town or nation's text channel is moved into the `archive.categoryName` category instead of being deleted, and takes that category's permissions. The town's members lose access, and whoever can read the archive category can read the history. The category is created private (hidden from `@everyone`) when missing; a public category with that name is refused. Roles and voice channels are still deleted.

### Role names and messages

`names.townRole` and `names.nationRole` set the Discord role names, with `{name}` standing for the Towny name. Changing them renames the existing roles in place, so members keep their access.

Every player and admin message is in `plugins/TownyDiscordChat/messages.yml`, with `&` colour codes and placeholders such as `{town}`. Messages customised in an older `config.yml` are copied over on first start.

## Commands and permissions

| Command | Permission | Purpose |
| --- | --- | --- |
| `/tdc`, `/tdc help` | `townydiscordchat.player` | Plugin information and the commands you can use |
| `/tdc status` | `townydiscordchat.player` | Your town, nation and DiscordSRV link status |
| `/tdc link` | `townydiscordchat.player` and DiscordSRV's `discordsrv.link` | Start DiscordSRV's own account linking |
| `/tdc reload` | `townydiscordchat.reload` | Reload `config.yml` and `messages.yml`; storage and queue timing still need a restart |
| `/tdc debug` | `townydiscordchat.debug` | Write a support report with versions, settings, Discord checks and recent failures to `plugins/TownyDiscordChat/debug`, with credentials removed |
| `/tdc check` | `townydiscordchat.reconcile` | Check every town and nation against Discord now |
| `/tdcqueue status` | `townydiscordchat.queue` | Queue and membership counts |
| `/tdcqueue audit` | `townydiscordchat.queue` | Request a full guild audit; needs Server Members Intent |
| `/tdcqueue retry-dead` | `townydiscordchat.queue` | Retry failed jobs after fixing the cause |

`townydiscordchat.player` is given to everyone. `townydiscordchat.admin` (operators by default) includes every node above and `townydiscordchat.notify`, which shows safe-mode, admin-warning and update notices in game. Tab completion only offers commands you can use. The old `TownyDiscordChat.Admin` node still works, because Bukkit permission names ignore case, and the old `townydiscordchat.check.*` nodes still grant `/tdc check`. DiscordSRV controls its own linking permissions.

`updates.check` looks for a newer GitHub release every 12 hours. It only announces it; nothing is downloaded.

## How syncing and recovery work

The [database-backed two-lane dispatcher](SYNC-MIGRATION.md) remembers intended membership and Discord operations across restarts. **Revocations take priority over grants**, while verified deletions precede new resource creation. JDA handles Discord's real rate limits; the durable queue handles retries, backoff, deduplication and crash recovery.

Towny UUIDs identify towns/nations even when their names change. Existing Discord roles/channels are renamed in place, preserving their IDs. An empty local database is not assumed to mean an empty Discord guild; startup checks and safely adopts verified resources instead of recreating everything. Legacy SQL rows are migrated without deleting historic tables; SQLite can be copied directly into MySQL without Discord API reconstruction.

**Existing Discord role permissions:** Legacy `town-*` / `nation-*` roles do **not** need to have zero server-wide permissions. The synchronizer may adopt a uniquely named role associated with a current Towny identity when its permission bits are limited to ordinary member capabilities (for example, sending messages, creating threads, using soundboard or creating events). Existing role permissions are **never removed or rewritten**. Administrator, role/channel management, moderation, other sensitive permissions, and unknown future permission bits are blocked and require administrator review. Identical role names are ambiguous and are never auto-selected. These checks also apply to grants, revocations, audits, renames and deletions. Channel privacy still requires verified `@everyone` View Channel denial and the town/nation role override.

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
- **Chat bridge relays nothing from Minecraft:** add `hooked: true` to the town and nation channels in TownyChat's `channels.yml`, then `/townychat reload`.
- **A leader didn't get moderation permissions:** give the bot Manage Messages, Mute Members, Deafen Members and Move Members, and check the player is linked. An explicit deny on that member in the channel blocks the grant on purpose.
- **After reinstall or migration:** restore a database backup if needed; keep the original `tdc_queued_tasks` table. If switching databases, follow the offline SQLite → MySQL process above, not manual config-only switching. See [migration details](SYNC-MIGRATION.md) and the [test plan](TEST-PLAN.md).

For reproducible issues, run `/tdc debug` and attach the report it writes, or **redacted** startup logs, your Minecraft version/server software, Towny and DiscordSRV versions, TownyDiscordChat version, and the queue status. Never publish database credentials or Discord bot tokens.

## Versioned documentation

Each release tag preserves the **exact README, configuration defaults, dependency matrix and migration instructions from that release**. To see instructions for an installed version, open its [GitHub release](https://github.com/thejames10/TownyDiscordChat/releases), select its tag, and view `README.md` from that tag (for example `blob/v1.1.2/README.md`).

We intentionally do **not** maintain separate editable copies of every historical README: tags are immutable release snapshots, which prevents the documentation for older versions changing when the latest README is updated.

Useful references: [Compatibility and artifact details](COMPATIBILITY.md) · [SQL migration and security](SYNC-MIGRATION.md) · [Manual integration test plan](TEST-PLAN.md) · [Towny](https://github.com/TownyAdvanced/Towny) · [DiscordSRV](https://github.com/DiscordSRV/DiscordSRV).

## Anonymous statistics

TownyDiscordChat reports anonymous usage statistics to
[bStats](https://bstats.org/plugin/bukkit/TownyDiscordChat/10980), which shows
which Minecraft, Towny and DiscordSRV versions to keep supporting. Besides
bStats' standard server details, it sends:

- whether the plugin is running, waiting for Discord, migrating storage, or in
  safe mode, and the general cause (never the error message itself);
- the storage type (SQLite or MySQL);
- the installed Towny and DiscordSRV versions;
- which channel, logging, account-security, archive, rank-moderation and chat-bridge features are switched on.

No player names, Discord IDs, town names or configuration values are sent. To
opt out, set `enabled: false` in `plugins/bStats/config.yml`.

## Contributing and support

- **Questions and setup help:** [TownyDiscordChat Discord](https://discord.gg/ynsCb2p3ac).
- **Bugs and feature requests:** [open an issue](https://github.com/thejames10/TownyDiscordChat/issues/new/choose) using the templates.
- **Security problems:** report privately as described in [SECURITY.md](SECURITY.md).
- **Code contributions:** read [CONTRIBUTING.md](CONTRIBUTING.md) for build, test and style requirements.

TownyDiscordChat is licensed under the [GNU GPL v3](LICENSE).
