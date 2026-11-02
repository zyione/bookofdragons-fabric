# Night Fury

## Quick profile

- **Class:** Strike in this build's data.
- **Entity ID:** `bookofdragons:nightfury`.
- **Activity habit:** night; see the route-specific spawn requirements below.
- **Player mount:** yes, when mature, tamed and saddled.
- **Flight:** supported by the species profile.
- **Minimum natural-spawn world age:** 20 ticking days.

These are source defaults for the unofficial **Fabric 1.20.1** port, updated
**2 November 2026**. Individual genetics, datapacks and server settings matter.

## Where to find it

- Natural spawning unlocks after **20 days of world ticking** (480,000 game ticks). The normal rule requires **nighttime** in the Overworld.
- Search cold mountains, snowy regions and taiga. The cold-biome tag also includes windswept hills/forests, frozen rivers, cold beaches and cold/frozen oceans.
- Sky route: **60-120 blocks above terrain** in a tagged cold biome. Peak route: ground at **Y 140 or higher**, with exposed sky, in a tagged cold biome.
- Sea-cliff route: snowy beach, stony shore, frozen ocean or cold ocean, with exposed sky and a steep slope (rise of at least 8 over a radius of 4).
- The normal rule places it at least **64 blocks from players**. New moon, thunder and an empty cliff roost within 160 blocks add spawn weight; they are not all mandatory.
- Normal roaming rules specify a world cap of 2 and a local cap of 1 within 384 blocks. Other population/roost rules also apply.
- The separate **albino** rule requires **40 ticking days**, a **full moon**, nighttime, and exposed frozen/jagged peaks at Y 140+. Its listed world cap is 1.

Spawn weights are selection inputs, not a guaranteed encounter rate. The general
spawn budget, local population and roost system can prevent an otherwise valid spawn.

## What to bring

- **Favorite food:** raw salmon.
- **Special food:** tropical fish.
- **Avoid:** pufferfish.
- **Treasure preferences:** amethyst shard, echo shard.

Bring several accepted food items, an empty-hand slot, and survival equipment.
A wild dragon can refuse hand-feeding until it is begging/asking you for a meal.
Drop treasure within reach when it requests a gift, then let it collect the item.
Bring a saddle for after taming; it does not skip the taming process.

## How to tame it

1. Approach calmly and read its current message/book guidance.
2. Respond to the requested activity using the supplies above. Feed only when
   it accepts an offer; a refused meal does not mean you should attack it.
3. Build the relationship through the activities it actually requests.
   **Do not treat the list below as a fixed sequence or a fixed number of feeds.**
4. For a touch request, hold right-click with an **empty hand**, normally for
   about 3 seconds. Follow any extra crouch/tool prompt.
5. When it offers its back, right-click with an empty hand and follow the
   bucking/taming prompts. After it accepts you, equip a saddle to ride normally.

**Available request pool:** approach slowly, defend it, play tag, touch/groom, share a meal, rest beside it, walk together, bring treasure, mount/bucking.

**Species-specific advice:** Its species matrix **dislikes live-prey offerings**. Use its actual meal, touch, movement or treasure requests instead of copying a Skrill taming routine.

## Abilities and riding

**Primary:** Fury bolt ranged attack (`bookofdragons:fury_bolt`) and bite.

**Secondary:** **Lightning Strike**: a close-range lightning-themed escape ability with temporary invisibility for the dragon and rider. It has its own cooldown and costs; it is not a requirement for taming.

Default rider controls: **G** inventory, **Space** jump/charged takeoff,
**left mouse** primary attack, **C** secondary where assigned, **Left Ctrl**
ground sprint/flight brake, **right mouse** free camera, **R** dismount
(confirm again in flight). Check your actual key bindings.

Age, injuries, stamina, relationship state and cooldowns can prevent an action.
Use the book/on-screen feedback for the individual dragon rather than assuming
fixed health, damage or speed values apply to every genetic variant.

## Eggs, growth and care

**Egg activation:** Activate in combined light **1 or below**. An enclosed dark chamber is more reliable than an exposed surface.

Place an egg with the item. Right-click it for progress and conditions;
crouch + right-click to pick it up with its incubation data. Maintain its
activation conditions until it says **Activated**, then keep it loaded and
ticking. The current implementation continues its incubation timer after activation.

Care for young dragons and let them grow. Accepted feeding advances growth;
favorite food gives a larger bonus. Hatchlings are not automatically ready for
riding or flight, and ownership depends on lineage/taming state.

For an owned dragon, hold the book and Shift + right-click to open its inventory.
With both hands empty and while not riding, Shift + right-click cycles stance.
Use the feedback to confirm its new state. Respect ownership and let injured
or tired animals recover.

## Scales and materials

The default entity loot table lists **4-7
nightfury scales** as the base death drop, before Looting adjustments.

For renewable collection, **right-click your own tamed dragon with shears**
when its shearing cooldown is clear. The shared implementation drops **1-2
scales**, consumes substantial shear durability, and sets a **6,000-12,000-tick**
cooldown (about 5-10 minutes at 20 TPS). Keep the dragon ticking for that timer.
This harvesting interaction requires ownership/taming; it is not a wild-taming step.
Recipe uses and pack overrides should be checked in your recipe viewer.

## Sources and coverage

- [nightfury.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/fauna/species/nightfury.json)
- [nightfury.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/spawn/nightfury.json)
- [NightfuryEntity.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/custom/dragon/NightfuryEntity.java)
- [DragonType.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/data/DragonType.java)
- [EggHatchCondition.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/data/EggHatchCondition.java)
- [nightfury.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/loot_tables/entities/nightfury.json)
- [ShearingComponent.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/component/ShearingComponent.java)

These pages describe shipped data/code, not a claim that every natural taming
sequence and secondary ability has been exhaustively playtested. Central client,
riding, persistence and ownership checks have passed; see the verification docs.

[Shared taming mechanics](TAMING_AND_SPAWNING.md) | [Wiki index](README.md) | [Player controls/admin guide](../PLAYER_GUIDE.md)
