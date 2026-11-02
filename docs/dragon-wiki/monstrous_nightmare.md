# Monstrous Nightmare

## Quick profile

- **Class:** Stoker in this build's data.
- **Entity ID:** `bookofdragons:monstrousnightmare`.
- **Activity habit:** day; see the route-specific spawn requirements below.
- **Player mount:** yes, when mature, tamed and saddled.
- **Flight:** supported by the species profile.
- **Minimum natural-spawn world age:** 8 ticking days.

These are source defaults for the unofficial **Fabric 1.20.1** port, updated
**2 November 2026**. Individual genetics, datapacks and server settings matter.

## Where to find it

- Natural spawning unlocks after **8 ticking days** (192,000 game ticks), in the Overworld, during its active **daytime**, with exposed sky.
- Arid route: temperature at least **1.5**, no precipitation, and no nearby creatures within the rule's 96-block check.
- Lava-pool route: temperature at least **0.8**, lava within 5 blocks, and no nearby creatures within its 64-block check.
- Barrens-roost route: airborne **16-40 blocks above terrain**, within 128 blocks of a barrens roost.
- The base roaming cap is 4, with 1 within 192 blocks locally (160 for the roost route). Very hot temperatures, clear weather and Hard difficulty add weight; Hard is not a universal requirement.

Spawn weights are selection inputs, not a guaranteed encounter rate. The general
spawn budget, local population and roost system can prevent an otherwise valid spawn.

## What to bring

- **Favorite food:** raw beef.
- **Special food:** tropical fish.
- **Avoid:** rotten flesh.
- **Treasure preferences:** coal, charcoal, blaze rod.

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
4. This species does not have a TOUCH step in its shipped pool.
5. When it offers its back, right-click with an empty hand and follow the
   bucking/taming prompts. After it accepts you, equip a saddle to ride normally.

**Available request pool:** approach slowly, defend it, spar when invited, provide live prey, share a meal, rest beside it, walk together, hunt together, bring treasure, mount/bucking.

**Species-specific advice:** The species **hates grooming** and **likes live prey**. It has no TOUCH stage in its shipped taming pool. Follow invited sparring, meal, movement or treasure requests rather than repeatedly trying to pet it.

## Abilities and riding

**Primary:** Flame breath (`bookofdragons:standard_flame`) and bite.

**Secondary:** **Self-Immolation** is its registered secondary ability. Treat its fire effects as dangerous around other entities and builds.

Default rider controls: **G** inventory, **Space** jump/charged takeoff,
**left mouse** primary attack, **C** secondary where assigned, **Left Ctrl**
ground sprint/flight brake, **right mouse** free camera, **R** dismount
(confirm again in flight). Check your actual key bindings.

Age, injuries, stamina, relationship state and cooldowns can prevent an action.
Use the book/on-screen feedback for the individual dragon rather than assuming
fixed health, damage or speed values apply to every genetic variant.

## Eggs, growth and care

**Egg activation:** Activate with **fire or soul fire at the egg's block position**. A nearby campfire or lava pool does not satisfy this specific fire-block check.

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

The default entity loot table lists **3-5
monstrousnightmare scales** as the base death drop, before Looting adjustments.

For renewable collection, **right-click your own tamed dragon with shears**
when its shearing cooldown is clear. The shared implementation drops **1-2
scales**, consumes substantial shear durability, and sets a **6,000-12,000-tick**
cooldown (about 5-10 minutes at 20 TPS). Keep the dragon ticking for that timer.
This harvesting interaction requires ownership/taming; it is not a wild-taming step.
Recipe uses and pack overrides should be checked in your recipe viewer.

## Sources and coverage

- [monstrous_nightmare.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/fauna/species/monstrous_nightmare.json)
- [monstrous_nightmare.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/spawn/monstrous_nightmare.json)
- [MonstrousnightmareEntity.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/custom/dragon/MonstrousnightmareEntity.java)
- [DragonType.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/data/DragonType.java)
- [EggHatchCondition.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/data/EggHatchCondition.java)
- [monstrousnightmare.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/loot_tables/entities/monstrousnightmare.json)
- [ShearingComponent.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/component/ShearingComponent.java)

These pages describe shipped data/code, not a claim that every natural taming
sequence and secondary ability has been exhaustively playtested. Central client,
riding, persistence and ownership checks have passed; see the verification docs.

[Shared taming mechanics](TAMING_AND_SPAWNING.md) | [Wiki index](README.md) | [Player controls/admin guide](../PLAYER_GUIDE.md)
