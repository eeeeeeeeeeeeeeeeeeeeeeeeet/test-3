# Fork World (NeoForge 1.21.1)

Fork the area around you into a private simulation, rehearse, then **merge** the
winning input sequence into the real world by replaying it through your real
client. The server only ever sees a normal player sending normal packets.

## Status

This is a first slice. It has **not been compiled or run** (written without
network access to the NeoForge Maven), so expect small API fix-ups on first build.

| Piece | State |
|---|---|
| Snapshot of blocks, block entities and entities around you (`ForkSnapshot`) | written |
| Tick-exact input recorder (`InputRecorder`, `Recording`, JSON save/load) | written |
| Merge: replay recording via real client input + divergence abort (`Replayer`) | written |
| Keybinds, messages | written |
| **Sandbox server that runs the fork (`SandboxBackend`)** | **not built** |
| Corner "window" showing the real world | not built |

## Keys (rebindable)

- G: fork (currently only captures the snapshot, no sandbox yet)
- N: rewind fork, M: discard fork / cancel a merge in progress
- B: merge the last recording into the real world
- V: debug only (`-Dforkworld.debug=true`): records in the REAL world, so you can
  test the merge path in a creative superflat world: press V, walk and place
  blocks, press V, return to the exact start position, press B.

## Merge safety

Merge refuses to start unless you stand within 0.3 blocks of the fork origin in
the same dimension, and aborts the moment your real position is more than 0.5
blocks from where the recording expects it. Mobs, other players and lag will
break exact replays, so a merge is best-effort, not guaranteed.

## What is still missing (the hard part)

Booting a sandbox needs a second `MinecraftServer` inside the client process
(the `IntegratedServer` machinery: `LevelStorageAccess`, `PackRepository`,
`WorldStem`, ...) loaded from the snapshot, plus swapping the client's
`mc.level` / `mc.player` / `mc.gameMode` over to it while keeping the real
`Connection` alive and ticking (keep-alives, chunk and entity packets buffered
or applied to a background `ClientLevel`). That needs mixins and a spike to
find out how much of the real connection can be held in the background.
Implement `SandboxBackend` and assign it to `ForkWorld.backend`.

## Build

Copy the Gradle wrapper from the NeoForge MDK (or run `gradle wrapper`), then
`./gradlew runClient`. If `neo_version` in `gradle.properties` does not resolve,
pick the latest `21.1.x` from https://projects.neoforged.net/neoforged/neoforge.
