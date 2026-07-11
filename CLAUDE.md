# Player Abilities

A personal Forge mod (this branch: Minecraft **1.20.1**, Forge 47.2.1, Java 17, ForgeGradle 6 — `main` is the NeoForge 1.21.1 original) that provides a framework / API for player abilities, both active and passive. Other mods (and this one) register abilities through the API; players acquire and use them in game. The reusable NeoForge→Forge porting guide lives at `../claude_reference/forge-port/`.

## What we're building

### Active abilities

Castable abilities that behave like spells in Iron's Spells and Spellbooks:

- The player selects an ability from those they know, then casts it (instant, charged, or channeled casting is still to be worked out).
- Hotkey support so abilities can be selected/cast without opening a menu.
- Cooldowns, cast conditions, and resource costs are open design questions.

### Passive abilities

Always-on abilities that work like tags on the player: if the player has the passive, the effect is active. Example: a water-walking passive lets the player walk on water. No casting, no selection — possession is the ability. The exact data we need per passive (just an id, or id + config values) is still to be worked out.

### Triggered abilities (planned)

A third kind: fire automatically on a condition (e.g. drop below half health → heal), then go on an internal cooldown before they can fire again. Not yet designed — see the TODO in `claude_reference/backend_data_design.md`.

### Registration

A registry so abilities can be declared and looked up by id — both built-in abilities and abilities added by other mods through the API. How players are *granted* abilities (commands, data attachments, compat mods below) is part of the design.

### Mod compat

- **Project MMO**: gate ability *use* behind a skill level (e.g. can't cast until Magic 20).
- **Pufferfish's Skills**: both gate and *grant* abilities (active or passive) via skill tree nodes — unlocking a node gives the player the ability.
- **Iron's Spells and Spellbooks** (later): let abilities cost mana from Iron's mana pool.

Compat must be optional: the mod works standalone when neither is installed.

## Project info

- Mod id: `player_abilities`
- Base package / group: `net.silvertide.player_abilities`
- Main mod class: `src/main/java/net/silvertide/player_abilities/PlayerAbilities.java`
- Mod metadata: `src/main/resources/META-INF/mods.toml` (values are substituted from `gradle.properties` by `processResources`)
- Per-player state: mutable `AbilityData` lives in a Forge **capability** (`data/AbilityCapability`), serialized via its `Codec`; death/end-portal copying happens in `PlayerLifecycleHandler.onClone` via `AbilityCapability.copy` (reviveCaps → loadFrom → invalidateCaps).
- Client-bound packet handlers are registered through the `DistExecutor.unsafeRunWhenOn` double-lambda in `AbilityNetworking` — never a bare `ClientPayloadHandlers::` method reference; the bytecode verifier would classload client types on the dedicated server and crash it.

## Conventions

- All mod id, name, version, and dependency ranges live in `gradle.properties` — change them there, not in mods.toml.
- Register content with `DeferredRegister` on the mod event bus from the `PlayerAbilities` constructor.
- Assets go under `src/main/resources/assets/player_abilities/`.

## Dev-only test content

Test abilities used to exercise the framework in dev but NOT shipped live under:
- Package `net.silvertide.player_abilities.dev`
- Asset namespace `player_abilities_dev` (`src/main/resources/assets/player_abilities_dev/`)

The `jar` task excludes both (see `build.gradle`). They compile with the rest and load in `runClient`/`runServer` (dev runs use `build/classes` + `build/resources`, not the jar), but are absent from the published jar. This is only safe because **no shipped code references the dev package** — `dev.DevAbilities` self-registers via an `@EventBusSubscriber` + `RegisterEvent` listener (found by annotation scan in dev, simply absent in production). Put new throwaway/test abilities here; graduate one to `content/` + the `player_abilities` namespace only when it's meant to ship.

## Build & run

- `./gradlew build` — build the mod jar (output in `build/libs/`, reobfuscated via `reobfJar`).
- `./gradlew runClient` / `./gradlew runServer` — launch a dev instance.
- `./gradlew publishMods` — publish to CurseForge/Modrinth + Discord announce (env vars: `CF_TOKEN`, `MODRINTH_TOKEN`, `DISCORD_WEBHOOK_URL`; `PUBLISH_DRY_RUN=true` + `DISCORD_WEBHOOK_TEST_URL` for a dry run). Wrapper is Gradle 8.8 — required by mod-publish-plugin 0.8.x; do not "upgrade" to 9.x (ForgeGradle 6 breaks).

---

# Reusable Engineering Standards

The sections below are project-agnostic. Copy this block (everything below the `---` separator) into any other project's `CLAUDE.md` unchanged to apply the same standards there.

## Code Style

**Never write comments.** No inline `//` comments, no `/* */` blocks, no javadoc, no leading explanatory headers on methods or fields. Code must be self-documenting through naming alone.

- Variable names describe what the value *is* (e.g. `armorCoveragePercent`, not `acp` with a comment).
- Method names describe what they *do* and under what conditions (e.g. `applyMultiplierIfAttackerIsPlayer`, not `applyBonus` with a comment explaining the player check).
- Extract a well-named helper method instead of writing a comment to explain a block.
- Constants get descriptive names that encode their meaning and unit (e.g. `KNIGHTMETAL_BONUS_DAMAGE_AT_FULL_ARMOR`, not `MAX` with a `// 2.0 vs fully-armored target` comment).
- If a name would need a comment to explain it, rename it until it doesn't.

Existing files may still contain comments and javadoc — leave them in place when editing unrelated code, but do not add new ones and prefer to delete obsolete ones when touching the surrounding code.

**Never leave dead code.** No unused methods, fields, classes, parameters, or imports. No "escape hatch" or "just in case" code. No commented-out blocks. If it's not called, delete it — the git history is the archive.

## Code Review

When asked to review code, do a "pass", check for issues, or otherwise audit a recent change, do **two** passes in order:

1. **Self-audit first.** Read the diff yourself. Fix the obvious — dead code, comments, naming, anything that violates the Code Style rules above. Report findings.
2. **Then spawn an independent reviewer** via the `/code-review` skill or a fresh agent. Give it only the diff and the goal, no context about why you made the choices you did. That catches the bugs you would otherwise rationalize away.
3. **Write to audit.md** Write the findings from the audit to audit.md in the claude_reference folder so we can checkmark them as we complete them.
4. **Project Problems check** by running a whole project search in the problems / project tab. Have the user download and put this file into the claude_reference/problems to check. Ask before deploys if we should do this.
   Don't skip step 2 because step 1 looked clean — the value of the independent reviewer is exactly that it doesn't share your blind spots.

## Version Control

**The user handles commits in git.** Never run `git add`, `git commit`, or `git push` — and don't suggest doing so — unless the user explicitly asks. Wrap up work by reporting what changed; staging and pushing are the user's job.
