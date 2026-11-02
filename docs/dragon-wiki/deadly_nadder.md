# Deadly Nadder

## Quick profile

- **Class:** Tracker in this build's data.
- **Entity ID:** `bookofdragons:deadlynadder`.
- **Activity habit:** day; see the route-specific spawn requirements below.
- **Player mount:** yes, when mature, tamed and saddled.
- **Flight:** supported by the species profile.
- **Minimum natural-spawn world age:** no additional age gate in this species rule.

These are source defaults for the unofficial **Fabric 1.20.1** port, updated
**2 November 2026**. Individual genetics, datapacks and server settings matter.

## Where to find it

- Natural spawning requires the Overworld and **daytime**. No additional minimum world age is specified in this species rule.
- Forest/jungle route: a biome tagged as forest or jungle, or cherry grove, with temperature at least **0.5** and exposed sky.
- Flower route: flower forest, meadow, sunflower plains or cherry grove, with at least **3 flower blocks within 4 blocks**.
- Typical groups are 1-2; the flower route uses 2-3. The base local cap is 4 within 128 blocks. Glade roosts within 160 blocks and clear weather add spawn weight.

Spawn weights are selection inputs, not a guaranteed encounter rate. The general
spawn budget, local population and roost system can prevent an otherwise valid spawn.

## What to bring

- **Favorite food:** raw chicken.
- **Special food:** tropical fish.
- **Avoid:** spider eye.

Bring several accepted food items, an empty-hand slot, and survival equipment.
A wild dragon can refuse hand-feeding until it is begging/asking you for a meal.
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

**Available request pool:** approach slowly, play tag, touch/groom, share a meal, rest beside it, walk together, mount/bucking.

**Species-specific advice:** Its shipped taming pool includes touch and movement activities, but **does not include sparring, hunting or a gift stage**. Do not attack it because another species asks for sparring.

## Abilities and riding

**Primary:** Beam breath (`bookofdragons:precise_beam`) and bite.

**Secondary:** **Spike Barrage** is its registered secondary ability. Availability follows the ability checks and cooldowns.

Default rider controls: **G** inventory, **Space** jump/charged takeoff,
**left mouse** primary attack, **C** secondary where assigned, **Left Ctrl**
ground sprint/flight brake, **right mouse** free camera, **R** dismount
(confirm again in flight). Check your actual key bindings.

Age, injuries, stamina, relationship state and cooldowns can prevent an action.
Use the book/on-screen feedback for the individual dragon rather than assuming
fixed health, damage or speed values apply to every genetic variant.

## Eggs, growth and care

**Egg activation:** Activate with **block light 10 or above** at the egg. Daylight alone is not the block-light condition; use suitable nearby light sources.

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
deadlynadder scales** as the base death drop, before Looting adjustments.

For renewable collection, **right-click your own tamed dragon with shears**
when its shearing cooldown is clear. The shared implementation drops **1-2
scales**, consumes substantial shear durability, and sets a **6,000-12,000-tick**
cooldown (about 5-10 minutes at 20 TPS). Keep the dragon ticking for that timer.
This harvesting interaction requires ownership/taming; it is not a wild-taming step.
Recipe uses and pack overrides should be checked in your recipe viewer.

## Sources and coverage

- [deadly_nadder.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/fauna/species/deadly_nadder.json)
- [deadly_nadder.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/spawn/deadly_nadder.json)
- [DeadlynadderEntity.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/custom/dragon/DeadlynadderEntity.java)
- [DragonType.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/data/DragonType.java)
- [EggHatchCondition.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/data/EggHatchCondition.java)
- [deadlynadder.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/loot_tables/entities/deadlynadder.json)
- [ShearingComponent.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/component/ShearingComponent.java)

These pages describe shipped data/code, not a claim that every natural taming
sequence and secondary ability has been exhaustively playtested. Central client,
riding, persistence and ownership checks have passed; see the verification docs.

[Shared taming mechanics](TAMING_AND_SPAWNING.md) | [Wiki index](README.md) | [Player controls/admin guide](../PLAYER_GUIDE.md)
