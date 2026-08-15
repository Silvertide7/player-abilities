1.0.8
---
- Fixed a passive ability whose id nests under another's (like example:speed and example:speed/two) having its attribute bonuses stripped when the other activates.

1.0.7
---
- Players with PMMO's ignoreReqs flag (/pmmo admin <player> ignoreReqs) now bypass ability skill requirements, matching how PMMO treats its own requirements.
- Fixed passive abilities losing their attribute bonuses after returning through the End exit portal.
- A triggered ability that throws an error no longer crashes the server; it is logged and skipped.
- Fixed rarer issues found in a full audit: a possible login crash with addon abilities, a config leak between servers on the client, skill-mod grant reconciliation running against a stale player after respawn or during the End credits, and duplicate tick jobs stacking on death.
