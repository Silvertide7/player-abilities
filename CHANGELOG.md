1.0.1
---
- Fixed a false "ability is ready" notice: an ability revoked while its cooldown was still running (for example a boon mod taking its boon away on death) announced itself as ready anyway, most visibly right after respawning.
- Fixed abilities with kill or damage requirements never announcing that they were ready. The notice now appears when the requirement is actually met, rather than being skipped because the cooldown happened to end first.
- Fixed clearing a cooldown early through the API not announcing the ability as ready.
- Ready notices are now decided by the server at the moment a cooldown expires instead of being inferred by the client, so they survive respawns, dimension changes, and lag spikes.
- Network protocol version bumped. Clients and servers must both be on 1.0.1.

1.0.0
---
- Port to Forge 1.20.1
