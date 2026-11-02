# Shared Taming and Spawning Mechanics

This page explains the common mechanics used by the nine species pages. It is
for the unofficial **Book of Dragons Fabric 1.20.1** port, updated 2 November 2026.
Follow the dragon's actual prompt and your server's loaded configuration.

## Before starting

Bring the species' favorite/special food, appropriate survival equipment and
an empty hand slot. Bring a **brush** for Whispering Death's touch request.
Treasure is useful only where a gift request is part of the species' pool.

Approach calmly. The social system responds to familiarity, fear, needs,
previous actions and the dragon's temperament. Repeating one action forever
is not an instant-tame recipe. If a dragon is fighting, frightened or issuing
a warning, give it space instead of treating every refusal as a broken mod.

The species page lists activities that can be selected. **It does not prescribe
their order**, and a dragon need not ask for every listed activity.

## World age and natural spawning

An age gate means actual world simulation time, measured by
`level.getGameTime() / 24000`. A default 24,000-tick day is about 20 real minutes
at 20 TPS. Pausing, a stopped server and server lag affect the real-time wait.
Sleeping or `/time set` changes the day/night clock; it does not grant the
skipped simulation ticks.

With cheats/operator permission, run:

```text
/time query gametime
```

The default gates are:

- **Speed Stinger:** 6 ticking days / 144,000 ticks / about 2 hours.
- **Monstrous Nightmare:** 8 / 192,000 / about 2 hours 40 minutes.
- **Whispering Death:** 12 / 288,000 / about 4 hours.
- **Skrill:** 15 / 360,000 / about 5 hours.
- **Night Fury:** 20 / 480,000 / about 6 hours 40 minutes.
- **Albino Night Fury rule:** 40 / 960,000 / about 13 hours 20 minutes.
- Nadder, Gronckle, Zippleback and Terror have no additional age minimum in
  their species files; all their other conditions still apply.

These are **minimums**, not one-day windows. A Night Fury can spawn after the
world is older than 20 ticking days, whenever its remaining conditions qualify.

## Why a suitable area may still have no dragons

The Overworld, biome/tag, time, weather, light, height and population checks all
matter. A mountain may look suitable without having the required biome ID/tag.
Modded biomes need to match the relevant lists or tags too.

The normal spawner runs periodically (the shipped period is 1,200 ticks),
uses weights/abundance and enforces shared population budgets. A weight is not
a guarantee that an encounter appears on arrival. Roosts have additional rules.
The caps described in the pages are spawn-rule settings, not a universal limit
on how many owned dragons may exist. The census world-cap check counts relevant
wild, unroosted animals; local checks can also count nearby matching animals.

**Activity habit and spawning are different:** Skrill is day-active, yet its
storm/ocean routes are not all daytime-only. Gronckle and Whispering Death cave
routes also differ from their general activity habits. Follow the page's exact
route requirements rather than assuming every nocturnal animal spawns only at night.

## Meals, gifts and live prey

**Meal:** right-click with accepted food when the wild dragon is begging/asking
you for food. Wild or estranged dragons can refuse hand-feeding otherwise.
Favorite, special and disliked items come from the species' data. Special food
can be unusual: Gronckle accepts gold ore, while Zippleback has a pufferfish
preference. Those items do not work as universal foods for other species.

**Gift:** when requested, drop suitable treasure where the dragon can reach it
and let it collect the item. Holding a lightning rod near a Skrill is not an
automatic tame. A gift and an ordinary meal are different delivery types.

**Live prey:** this is a separate prey-delivery activity; cooked meat in your
hand is not a live animal. Use the dragon's prey/request cues. Species differ:
Night Fury, Gronckle and Terror dislike live-prey offerings, while Skrill,
Nightmare, Speed Stinger and Whispering Death are configured to like them.

**Hunt together:** participate against the prey it is hunting while it can
recognize your participation. Its knowledge checks distinguish an enemy,
its chosen prey, and an unrelated animal; killing arbitrary mobs is not a
guaranteed substitute.

## Touch and grooming

When a TOUCH request appears, hold right-click with the requested item/hand,
normally for **60 ticks** (about 3 seconds at normal ticking). Follow any
additional crouch requirement shown by the request.

- Night Fury, Nadder, Gronckle, Zippleback and Terror have an ordinary touch stage.
- Whispering Death's touch stage specifies `#bookofdragons:brushes`; the shipped
  tag contains **Minecraft's brush**.
- Skrill and Monstrous Nightmare **hate grooming** and have no TOUCH stage.
- Speed Stinger **dislikes grooming** and has no TOUCH stage.

Gronckle, Zippleback and Terror are explicitly configured to like grooming.
Do not apply a Night Fury petting routine to every dragon.

## Resting, walking, tag and defending

**Rest:** the shared implementation recognizes calm company while the dragon
is asleep and you have been seen crouching within **8 blocks**. It accumulates
**400 simulation ticks** of suitable rest company (about 20 seconds at 20 TPS).
Moving out of the conditions can reset the accumulation.

**Walk:** travel with a calm, restless dragon. The shared check uses company
within **16 blocks** and accumulates about **32 blocks of its travel**. This
is not simply standing still beside any idle dragon for a fixed timer.

**Tag/follow:** respond to a play invitation with right-click, then participate
in the movement/play activity it requests. This is
different from setting an already-tamed animal's FOLLOW command.

**Defend:** help against a threat it recognizes while it can witness your
actions. Harming an unrelated animal is not automatically defending the dragon.

**Spar:** wait for an invited sparring bout. The system tracks a spar session;
its presence in a species' pool is not permission to treat every encounter as
an ordinary fight. Watch the prompts and stop if the situation changes.

These thresholds describe the shared implementation, not an exhaustive
end-to-end playtest of every request and temperament combination.

## Mounting trial and normal riding

For a rideable species, wait until it offers its back. Right-click with an
empty hand and complete the displayed bucking/taming interaction. Follow the
keys on screen; the default letter bindings are **F/G/H/J/K/L**.

After acceptance, equip a **saddle** for normal riding. The mount must be old
enough and in an acceptable health/relationship state. Default rider inputs
include **G** inventory, **Space** charged takeoff for eligible fliers,
**left mouse** primary attack, **C** secondary where available, and **R** dismount
(confirm again in flight). Resolve modpack key conflicts in Controls.

**Terrible Terror and Speed Stinger are not player mounts.** Their normal
taming pools omit MOUNT. Terror can fly/perch as a companion; Stinger's profile
disables flight. Ownership commands or a saddle do not add missing capabilities.

If it says **"It has not offered me its back"**, it is still treated as wild
and has not reached the permitted riding trial. If already tame, a different
message can identify a missing saddle, estrangement, youth or another restriction.

## Eggs, growth, ownership and care

Use the species page's egg-activation environment. Right-click an egg for its
status; crouch + right-click picks it up with saved incubation data. Maintain
the conditions until **Activated**, then keep it loaded and ticking. The current
timer continues after activation; the required real-world duration is not
guaranteed under lag or unloading.

Feed accepted foods and allow growth, rest and recovery. A favorite food gives
a larger growth-progress bonus. A hatchling's appearance and lineage can be
inherited, but ownership/readiness should be checked rather than assumed.
There is no universal fixed health/damage/speed value for every genetic individual.

Hold the book and Shift + right-click an owned tame dragon for its inventory.
With both hands empty and while not riding, Shift + right-click cycles stance.
A whistle can bind to your tamed dragon; calling has ownership, state and
Overworld/open-sky restrictions. See the player guide for the full controls.

For scale harvesting, right-click your own tamed dragon with shears after its
cooldown clears. The source defaults are 1-2 scales, significant shear durability
use and a 6,000-12,000-tick cooldown. This is separate from wild taming and from
the species' death-loot table. Each species page lists its material details.

## Creative/admin shortcuts and troubleshooting

Creative mode alone does not auto-tame in this port. The
[player guide's admin section](../PLAYER_GUIDE.md) contains the tested Owner
and TamingRitualCompleted commands. Run them as your player, with the latest
repaired FAUNA jar: old builds could select a hitbox instead of the parent.
Changing ownership/ritual data does not add age, saddles or flight capabilities.

For refusals, check the exact prompt, food, tool, distance, current activity and
relationship state first. For technical bugs, include the jar versions,
Minecraft/Fabric versions, exact action/message and relevant `latest.log` or crash
report. The [verification report](../VERIFICATION.md) distinguishes passed
checks from longer/manual scenarios still needing coverage.

## Source references

- [Courtship request selection and rest/walk thresholds](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/fauna/src/main/java/net/magister/liberanimalium/fauna/know/Courtship.java)
- [Knowledge, company and witnessed actions](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/fauna/src/main/java/net/magister/liberanimalium/fauna/know/Knowledge.java)
- [Spawn conditions and world age](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/fauna/src/main/java/net/magister/liberanimalium/spawn/SpawnCondition.java)
- [Population census](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/fauna/src/main/java/net/magister/liberanimalium/spawn/Census.java)
- [Spawn settings](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/spawn_settings.json)
