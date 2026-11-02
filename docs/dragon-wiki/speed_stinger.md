# Speed Stinger

## Quick profile

- **Class:** Sharp in this build's data.
- **Entity ID:** `bookofdragons:speed_stinger`.
- **Activity habit:** night; see the route-specific spawn requirements below.
- **Player mount:** no.
- **Flight:** disabled by the species profile.
- **Minimum natural-spawn world age:** 6 ticking days.

These are source defaults for the unofficial **Fabric 1.20.1** port, updated
**2 November 2026**. Individual genetics, datapacks and server settings matter.

## Where to find it

- Natural spawning unlocks after **6 ticking days** (144,000 game ticks), in the Overworld during active **nighttime**, with **block light 0**.
- Search taiga-tagged biomes, grove, dark forest, snowy plains or snowy slopes.
- Base groups are 2-4. A full moon adds weight and changes the group to 3-5. The local cap is 6 within 96 blocks.

Spawn weights are selection inputs, not a guaranteed encounter rate. The general
spawn budget, local population and roost system can prevent an otherwise valid spawn.

## What to bring

- **Favorite food:** raw salmon.
- **Special food:** raw cod.
- **Avoid:** rotten flesh.

Bring several accepted food items, an empty-hand slot, and survival equipment.
A wild dragon can refuse hand-feeding until it is begging/asking you for a meal.

## How to tame it

1. Approach calmly and read its current message/book guidance.
2. Respond to the requested activity using the supplies above. Feed only when
   it accepts an offer; a refused meal does not mean you should attack it.
3. Build the relationship through the activities it actually requests.
   **Do not treat the list below as a fixed sequence or a fixed number of feeds.**
4. This species does not have a TOUCH step in its shipped pool.
5. It has no mounting trial in this pool. Finish the relationship interactions
   it requests; do not try to force a ride as the final step.

**Available request pool:** approach slowly, spar when invited, play tag, provide live prey, share a meal, rest beside it, walk together, hunt together.

**Species-specific advice:** The species **dislikes grooming** and **likes live prey**. It has neither TOUCH nor MOUNT in the shipped taming pool. Complete the social requests it actually gives; a saddle will not make it a mount.

## Abilities and riding

**Primary:** **Melee only**: sting and bite attacks. It has no ranged breath in its species attack definition.

**Secondary:** A **Sting** ability is registered. This is a ground companion: its profile explicitly disables both player riding and flight, while allowing jumping and opening doors.

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

The default entity loot table lists **2-4
speed stinger scales** as the base death drop, before Looting adjustments.

For renewable collection, **right-click your own tamed dragon with shears**
when its shearing cooldown is clear. The shared implementation drops **1-2
scales**, consumes substantial shear durability, and sets a **6,000-12,000-tick**
cooldown (about 5-10 minutes at 20 TPS). Keep the dragon ticking for that timer.
This harvesting interaction requires ownership/taming; it is not a wild-taming step.
Recipe uses and pack overrides should be checked in your recipe viewer.

## Sources and coverage

- [speed_stinger.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/fauna/species/speed_stinger.json)
- [speed_stinger.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/spawn/speed_stinger.json)
- [SpeedStingerEntity.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/custom/dragon/SpeedStingerEntity.java)
- [DragonType.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/data/DragonType.java)
- [EggHatchCondition.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/data/EggHatchCondition.java)
- [speed_stinger.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/loot_tables/entities/speed_stinger.json)
- [ShearingComponent.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/component/ShearingComponent.java)

These pages describe shipped data/code, not a claim that every natural taming
sequence and secondary ability has been exhaustively playtested. Central client,
riding, persistence and ownership checks have passed; see the verification docs.

[Shared taming mechanics](TAMING_AND_SPAWNING.md) | [Wiki index](README.md) | [Player controls/admin guide](../PLAYER_GUIDE.md)
