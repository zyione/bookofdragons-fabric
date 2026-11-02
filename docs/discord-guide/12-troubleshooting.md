## 12. Troubleshooting

**Game fails during startup**
- Check Minecraft/Fabric versions and all required dependencies.
- Replace old copies of both port jars; avoid duplicate mod IDs.
- The repaired FAUNA build fixes the known explosion-hook conflict with
  Dragons of Berk and the ray-query conflict with Create's Porting Library.
- Send the complete `logs/latest.log` or relevant crash report when asking
  for help. An old repair does not diagnose every future startup error.

**Book or rattlestaff is invisible**
- Replace the Book of Dragons jar with the repaired item-model build.
- Restart Minecraft. If it persists, compare with custom resource packs disabled.

**Dragon refuses to offer its back**
- It is still being treated as untamed. Follow its requests or check the admin
  ownership/ritual fields in section 11. A tame dragon may separately need a saddle.

**Dragon cannot take off or use an ability**
- Check species, maturity, saddle, ownership, health, stamina, cooldowns and
  the exact on-screen message. Confirm your key bindings.

**Food is refused**
- Use an accepted species-specific food. A wild or estranged dragon normally
  needs to be asking you for a meal before hand-feeding succeeds.

**Egg is not progressing**
- Read its condition message, complete activation and keep its area loaded.

**Whistle will not call**
- Check binding, ownership, the Overworld/open-sky requirement and the message.

For useful help, include mod-jar versions, game/loader version, the action you
tried, the exact message, and relevant logs. A screenshot helps with visual bugs.
