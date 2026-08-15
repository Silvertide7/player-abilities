1.0.3
---
- Players with PMMO's ignoreReqs flag (/pmmo admin <player> ignoreReqs) now bypass ability skill requirements, matching how PMMO treats its own requirements.
- Fixed stale passive attribute bonuses surviving a relog after a datapack changes an ability's attribute_grants.
- Fixed an ability mid-use being silently dropped when returning through the End exit portal, and ability effects now restart properly after that return.
- An ability that throws an error in any of its callbacks no longer crashes the server; it is logged and skipped.
- Datapack configs now warn about keys that have no effect on that kind of ability.

1.0.2
---
- Fixed ability data being permanently lost — crashing the server — the first time a player died or traveled between dimensions.
- Fixed passive abilities losing their attribute bonuses after returning through the End exit portal.
- A triggered ability that throws an error no longer crashes the server; it is logged and skipped.
- Fixed rarer issues found in a full audit: a config leak between servers on the client, skill-mod grant reconciliation running against a stale player after respawn, and duplicate tick jobs stacking on death.
- Trimmed per-frame and per-tick allocations in the HUD and in ability effect ticking.

