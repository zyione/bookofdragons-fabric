# Terrible Terror

## Quick profile

- **Class:** Stoker in this build's data.
- **Entity ID:** `bookofdragons:terrible_terror`.
- **Activity habit:** day; see the route-specific spawn requirements below.
- **Player mount:** no.
- **Flight:** supported by the species profile.
- **Minimum natural-spawn world age:** no additional age gate in this species rule.

These are source defaults for the unofficial **Fabric 1.20.1** port, updated
**2 November 2026**. Individual genetics, datapacks and server settings matter.

## Where to find it

- Natural spawning requires the Overworld and active **daytime**. No additional world-age minimum is specified.
- Search beach-tagged biomes, stony shore, ocean-tagged sea-stack areas, or jungle-tagged biomes with water within 5 blocks.
- Typical groups are 2-4 (2-3 on the jungle route). Fisherman points of interest within 48 blocks add weight and use groups of 2-3.
- A full moon adds weight and can increase the group to 5-8. This does **not** remove the base daytime requirement. Base local cap: 8 within 64 blocks; special routes can change it.

Spawn weights are selection inputs, not a guaranteed encounter rate. The general
spawn budget, local population and roost system can prevent an otherwise valid spawn.

## What to bring

- **Favorite food:** raw cod.
- **Special food:** raw salmon.
- **Avoid:** spider eye.

Bring several accepted food items, an empty-hand slot, and survival equipment.
A wild dragon can refuse hand-feeding until it is begging/asking you for a meal.

## How to tame it

1. Approach calmly and read its current message/book guidance.
2. Respond to the requested activity using the supplies above. Feed only when
   it accepts an offer; a refused meal does not mean you should attack it.
3. Build the relationship through the activities it actually requests.
   **Do not treat the list below as a fixed sequence or a fixed number of feeds.**
4. For a touch request, hold right-click with an **empty hand**, normally for
   about 3 seconds. Follow any extra crouch/tool prompt.
5. It has no mounting trial in this pool. Finish the relationship interactions
   it requests; do not try to force a ride as the final step.

**Available request pool:** approach slowly, touch/groom, play tag, share a meal, rest beside it, walk together.

**Species-specific advice:** The species **likes grooming** and **dislikes live prey**. Once you own it, an empty-main-hand right-click without crouching beckons it for its perching interaction. It has no MOUNT stage; do not wait for a riding ritual.

## Abilities and riding

**Primary:** Small flame breath (`bookofdragons:terror_flame`) and bite.

**Secondary:** No separate secondary ability is assigned in its species class. It is a flying **companion that supports perching**, not a player mount.

Rider key instructions do not make a non-mount rideable. Its attacks belong
to the companion/AI system; do not expect a mounted C-key interface.

Age, injuries, stamina, relationship state and cooldowns can prevent an action.
Use the book/on-screen feedback for the individual dragon rather than assuming
fixed health, damage or speed values apply to every genetic variant.

## Eggs, growth and care

**Egg activation:** No special environmental activation condition is specified. Place the egg and keep it loaded while incubation progresses.

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

The default entity loot table lists **1-3
terrible terror scales** as the base death drop, before Looting adjustments.

For renewable collection, **right-click your own tamed dragon with shears**
when its shearing cooldown is clear. The shared implementation drops **1-2
scales**, consumes substantial shear durability, and sets a **6,000-12,000-tick**
cooldown (about 5-10 minutes at 20 TPS). Keep the dragon ticking for that timer.
This harvesting interaction requires ownership/taming; it is not a wild-taming step.
Recipe uses and pack overrides should be checked in your recipe viewer.

## Sources and coverage

- [terrible_terror.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/fauna/species/terrible_terror.json)
- [terrible_terror.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/spawn/terrible_terror.json)
- [TerribleTerrorEntity.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/custom/dragon/TerribleTerrorEntity.java)
- [DragonType.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/data/DragonType.java)
- [EggHatchCondition.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/data/EggHatchCondition.java)
- [terrible_terror.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/loot_tables/entities/terrible_terror.json)
- [ShearingComponent.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/component/ShearingComponent.java)

These pages describe shipped data/code, not a claim that every natural taming
sequence and secondary ability has been exhaustively playtested. Central client,
riding, persistence and ownership checks have passed; see the verification docs.

[Shared taming mechanics](TAMING_AND_SPAWNING.md) | [Wiki index](README.md) | [Player controls/admin guide](../PLAYER_GUIDE.md)
