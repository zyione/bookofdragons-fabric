## 11. Creative/admin force-taming

Creative mode does **not** automatically complete taming in this build.
With cheats or operator permission, these commands can assign the nearest
Night Fury within 10 blocks and complete its taming ritual.

**Use the latest repaired FAUNA jar first.** Older builds could target an
invisible hitbox instead of the dragon. Stand beside only the dragon you
intend to change. Run the commands **as your player in chat**, not as a bare
server-console command: `@s` must refer to you.

Run first:

```text
/data modify entity @e[type=bookofdragons:nightfury,sort=nearest,limit=1,distance=..10] Owner set from entity @s UUID
```

Then run:

```text
/data merge entity @e[type=bookofdragons:nightfury,sort=nearest,limit=1,distance=..10] {TamingRitualCompleted:1b,AwaitingTamingRitual:0b}
```

Equip a saddle afterward. This does not change age, injuries or the species'
natural abilities. Other species need their correct entity ID; use command
autocomplete rather than guessing the spelling.

To check the result:

```text
/data get entity @e[type=bookofdragons:nightfury,sort=nearest,limit=1,distance=..10] Owner
/data get entity @e[type=bookofdragons:nightfury,sort=nearest,limit=1,distance=..10] TamingRitualCompleted
```

Expect your UUID and `1b`. **"Found no elements matching Owner"** or a missing
ritual field means that target does not have the field. On an old port build,
this can mean a hitbox was selected. Update FAUNA and recheck the target.
