# Security policy

## Supported versions

Security fixes are made for the latest release on the
[releases page](https://github.com/thejames10/TownyDiscordChat/releases).
Older releases are not patched; please upgrade.

## Reporting a vulnerability

Please **do not** open a public issue for a security problem.

Report it privately using GitHub's
[private vulnerability reporting](https://github.com/thejames10/TownyDiscordChat/security/advisories/new),
or send a direct message to a maintainer on the
[TownyDiscordChat Discord](https://discord.gg/ynsCb2p3ac).

Include, where possible:

- the TownyDiscordChat, Minecraft, server software, Towny and DiscordSRV versions;
- the steps to reproduce, and what an attacker could gain (for example, access to
  a private town or nation channel they should not see);
- any relevant log excerpts, with tokens, IP addresses and database credentials removed.

You should receive an acknowledgement within a few days. Please give us a
reasonable time to release a fix before disclosing the issue publicly.

## Scope

Issues that are in scope include unauthorized access to Discord roles or channels
managed by the plugin, privilege escalation through plugin commands, and leaks of
stored credentials. Problems in Towny, DiscordSRV, JDA or the server software
itself should be reported to those projects.
