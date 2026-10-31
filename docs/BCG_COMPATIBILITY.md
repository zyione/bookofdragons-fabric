# BigChadGuys Plus compatibility

Passed on 2026-10-31: all ten client checks completed, the test world saved and
the Java 17 process exited cleanly. No additional gameplay-jar changes were
required. The user's original 409 mod-jar hashes remain unchanged.

Machine-readable evidence and exact release hashes are in
`bcg-compatibility-results.json`.

## Verified

- Normal and renamed book icons, held book, rattlestaff models and textures.
- All nine species finalized with synchronized genetic appearances and rendered
  with the enabled resource packs and Complementary shader pack.
- Nearest-dragon commands from beside a hitbox select the parent; setting
  ownership and completing the ritual allows saddled mounting.
- Book UI and synchronized data; extended dragon inventory and entity preview.
- A real fragmented 100-page note save over Fabric's C2S transport.
- Mount/camera/HUD, adult takeoff and synchronized flight, primary breath
  charge/release over C2S, and dismount synchronized on server and client.

Screenshots of the dragon gallery, book UI and flight were visually reviewed.
The original resource-pack selection was retained in the test copy.

## Environment

- User's BigChadGuysPlus instance: Minecraft 1.20.1, Fabric Loader 0.19.5,
  Java 17, 409 original top-level mod jars (plus the test-only verification jar).
- Copied mods, configs, default configs, KubeJS scripts, resource packs,
  shader packs, TACZ resources and pack-specific supplemental resources.
- Original selection of 34 resource-pack entries and
  ComplementaryReimagined_r5.9.3 enabled through Iris 1.7.6; BCG Sodium build
  0.5.13+mc1.20.1.bcg.1. Windowed mode is the only launch display override.
- A separate test world; no existing saves, accounts or server list copied.
  Test character uses Human/Nitwit to complete Origins onboarding. Other origin
  and class combinations remain outside this check.
- The installed Book of Dragons and FAUNA jars already matched the latest
  development delivery before testing.

## Test setup

Starter-kit mods can replace initial hotbar contents, so model checks use
controlled item stacks. Origins' selection screen can pause the integrated
server while client ticks continue, so the harness initializes a neutral test
character and waits for simulation ticks before querying hitboxes. These changes
belong to the test-only jar and do not alter normal gameplay jars.

## Reproduce

Prepare the official isolated runtime with `tools/prepare_packaged_client.py`,
build `:bookofdragons:remapVerificationJar`, then run:

```powershell
python tools/verify_instance_client.py --instance '<launcher instance folder>' --name bcg
```

Use `--reuse` to repeat with the same isolated copy. The tool checks that the
source mod hashes still match. Output stays under `.reference/instance-client-bcg`:
`launch.log`, `result.json`, source jar hashes and `verification` screenshots.
Never install the verification jar into a normal instance.

## Limits

This checks the current local configuration and central dragon gameplay paths.
It does not cover every interaction across the modpack, all origins/classes,
long-term natural taming/breeding, existing saves or remote multiplayer servers.
The startup-heavy screenshots are not a steady-state performance benchmark.
