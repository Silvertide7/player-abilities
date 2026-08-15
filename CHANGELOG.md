1.0.2
---
- Fixed ability data being permanently lost — crashing the server — the first time a player died or traveled between dimensions.
- Fixed passive abilities losing their attribute bonuses after returning through the End exit portal.
- A triggered ability that throws an error no longer crashes the server; it is logged and skipped.
- Fixed rarer issues found in a full audit: a config leak between servers on the client, skill-mod grant reconciliation running against a stale player after respawn, and duplicate tick jobs stacking on death.
- Trimmed per-frame and per-tick allocations in the HUD and in ability effect ticking.

