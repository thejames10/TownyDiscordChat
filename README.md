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
<h1>Installation</h1>
<ol>
   <li><strong>Install</strong>/<strong>Configure</strong> <strong><a href="https://www.spigotmc.org/resources/towny-advanced.72694/" target="_blank" rel="noopener">Towny</a></strong> and <strong><a href="https://www.spigotmc.org/resources/discordsrv.18494/" target="_blank" rel="noopener">DiscordSRV</a></strong>.</li>
   <li><strong>Download</strong> our latest <strong><a href="https://github.com/thejames10/TownyDiscordChat/releases/latest" target="_blank" rel="noopener">release</a></strong>.</li>
   <li><strong>Delete</strong> existing <strong>config.yml</strong> if you are using a <strong>previous version</strong>.</li>
   <li><strong>Start server</strong> to <strong>generate</strong> <strong>config.yml</strong> then <strong>stop</strong> the <strong>server</strong>.</li>
   <li><strong>Configure</strong> the plugin with <strong><a href="https://github.com/thejames10/TownyDiscordChat/blob/main/README.md#configuration-file" target="_blank" rel="noopener">recommended settings</a></strong>.</li>
   <li><strong>Configure</strong> <a href="https://github.com/thejames10/TownyDiscordChat#nodes-structure" target="_blank" rel="noopener"><strong>permission nodes</strong></a> to <strong>use</strong> in-game <strong>commands</strong>.</li>
   <li><strong>Start</strong> the <strong>server</strong> and <strong>join</strong> the <strong>game</strong>.</li>
   <li><strong>Run</strong> the <a href="https://github.com/thejames10/TownyDiscordChat/blob/main/README.md#when-adding-this-plugin-to-an-existing-towny-server-make-sure-to-run-the-first-4-commands-in-the-order-shown-below-check-they-have-completed-before-running-the-next-one" target="_blank" rel="noopener"><strong>first 4 commands</strong></a> if this is your <strong>first time</strong>.</li>
   <li>Enjoy!</li>
</ol>

<h1>Configuration File</h1>

```YAML
nation:
  # Make sure this is 'true' during pre-release
  CreateTextChannelForRole: true
  # Make sure this is 'true' during pre-release
  CreateVoiceChannelForRole: true
  # Make sure this is 'true' during pre-release
  UseCategoryForText: true
  # Make sure this is 'true' during pre-release
  UseCategoryForVoice: true
  # Make sure to set this to a category during pre-release - https://www.youtube.com/watch?v=NLWtSHWKbAI
  TextCategoryId: 0 # This can be the same as the nation.VoiceCategoryID but make sure they are NOT the same as the town categoryIds
  # Make sure to set this to a category during pre-release - https://www.youtube.com/watch?v=NLWtSHWKbAI
  VoiceCategoryId: 0 # This can be the same as the nation.TextCategoryID but make sure they are NOT the same as the town categoryIds
  # Make sure this is 'true' during pre-release
  CreateRoleIfNoneExists: true
  # Make sure this is a valid color during pre-release
  RoleCreateColorCode: '0xc9c012'
town:
  # Make sure this is 'true' during pre-release
  CreateTextChannelForRole: true
  # Make sure this is 'true' during pre-release
  CreateVoiceChannelForRole: true
  # Make sure this is 'true' during pre-release
  UseCategoryForText: true
  # Make sure this is 'true' during pre-release
  UseCategoryForVoice: true
  # Make sure to set this to a category during pre-release - https://www.youtube.com/watch?v=NLWtSHWKbAI
  TextCategoryId: 0 # This can be the same as the town.VoiceCategoryID but make sure they are NOT the same as the nation categoryIds
  # Make sure to set this to a category during pre-release - https://www.youtube.com/watch?v=NLWtSHWKbAI
  VoiceCategoryId: 0 # This can be the same as the town.TextCategoryID but make sure they are NOT the same as the nation categoryIds
  # Make sure this is 'true' during pre-release
  CreateRoleIfNoneExists: true
  # Make sure this is a valid color during pre-release
  RoleCreateColorCode: '0x006798'
messages:
  # See https://github.com/thejames10/TownyDiscordChat/blob/main/AvailableTimeZones.txt
  TimeZone: UTC
  # See https://github.com/thejames10/TownyDiscordChat/blob/main/AvailableDateFormats.txt
  DateFormat: dd/MM/yyyy - hh:mm a
  # Make sure to set this to a channel during pre-release - https://www.youtube.com/watch?v=NLWtSHWKbAI
  DiscordLogTextChannelId: 0 # I would set this to a channelId that is NOT within the TOWN or NATION categories.
  # Prefix used when sending messages via discord or minecraft
  Prefix: '&8[&2TDC Bot&8]'
  
  Commands:
    PleaseWait: '&7Please wait...'
    NoPermission: '&7You don''t have permission to use this command!'
  Role:
    DoNothing:
      Success: '&7Role change not required!'
      Failure: '&7Failed to do nothing!'
    Remove:
      Success: '&7Removed role!'
      Failure: '&7Failed to remove role!'
    Add:
      Success: '&7Added role!'
      Failure: '&7Failed to add role!'
    Create:
      Success: '&7Created role!'
      Failure: '&7Failed to create role!'
    Delete:
      Success: '&7Deleted role!'
      Failure: '&7Failed to delete role!'
    Rename:
      Success: '&7Renamed role!'
      Failure: '&7Failed to rename role!'
  TextChannel:
    DoNothing:
      Success: '&7TextChannel change not required!'
      Failure: '&7Failed to do nothing!'
    Remove:
      Success: '&7Removed TextChannel!'
      Failure: '&7Failed to remove TextChannel!'
    Add:
      Success: '&7Added TextChannel!'
      Failure: '&7Failed to add TextChannel!'
    Create:
      Success: '&7Created TextChannel!'
      Failure: '&7Failed to create TextChannel!'
    Delete:
      Success: '&7Deleted TextChannel!'
      Failure: '&7Failed to delete TextChannel!'
    Rename:
      Success: '&7Renamed TextChannel!'
      Failure: '&7Failed to rename TextChannel!'
  VoiceChannel:
    DoNothing:
      Success: '&7VoiceChannel change not required!'
      Failure: '&7Failed to do nothing!'
    Remove:
      Success: '&7Removed VoiceChannel!'
      Failure: '&7Failed to remove VoiceChannel!'
    Add:
      Success: '&7Added VoiceChannel!'
      Failure: '&7Failed to add VoiceChannel!'
    Create:
      Success: '&7Created VoiceChannel!'
      Failure: '&7Failed to create VoiceChannel!'
    Delete:
      Success: '&7Deleted VoiceChannel!'
      Failure: '&7Failed to delete VoiceChannel!'
    Rename:
      Success: '&7Renamed VoiceChannel!'
      Failure: '&7Failed to rename VoiceChannel!
```
<h4>When adding this plugin to an existing Towny server make sure to run the first 4 commands in the order shown below. <br>Check they have completed before running the next one.</h4>
<h1>Commands</h1>
<h4>/TDC Check Role CreateAllTownsAndNations</h4>
<p>Permission Node: TownyDiscordChat.Check.Role.CreateAllTownsAndNations:</p>
<p>Description: Allows you to force run a check for discord roles of all players in a town or nation and creates them if needed</p>
<h4>/TDC Check TextChannel AllTownsAndNations</h4>
<p>Permission Node: TownyDiscordChat.Check.TextChannel.AllTownsAndNations:</p>
<p>Description: Allows you to force run a check for discord text channels of all players in a town or nation and creates them if needed</p>
<h4>/TDC Check VoiceChannel AllTownsAndNations</h4>
<p>Permission Node: TownyDiscordChat.Check.VoiceChannel.AllTownsAndNations:</p>
<p>Description: Allows you to force run a check for discord voice channels of all players in a town or nation and creates them if needed</p>
<h4>/TDC Check Role AllLinked</h4>
<p>Permission Node: TownyDiscordChat.Check.Role.AllLinked:</p>
<p>Description: Allows you to force run a check for discord roles of all players in a town or nation then adds or removes them if needed</p>
<h4>/TDC Check Role</h4>
<p>Permission Node: TownyDiscordChat.Check.Role:</p>
<p>Description: Allows you to force run a check for discord roles of a single player in a town or nation then adds or removes</p>
<h1>Nodes Structure</h1>
<h4>TownyDiscordChat.Admin</h4>
<p>- TownyDiscordChat.Check.Role.CreateAllTownsAndNations<br />- TownyDiscordChat.Check.TextChannel.AllTownsAndNations<br />- TownyDiscordChat.Check.VoiceChannel.AllTownsAndNations<br />- TownyDiscordChat.Check.Role.AllLinked<br />- TownyDiscordChat.Check.Role</p>
<h4>TownyDiscordChat.Player</h4>
<p>- TownyDiscordChat.Check.Role</p>
