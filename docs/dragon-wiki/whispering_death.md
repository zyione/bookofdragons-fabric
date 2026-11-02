# Whispering Death

## Quick profile

- **Class:** Boulder in this build's data.
- **Entity ID:** `bookofdragons:whispering_death`.
- **Activity habit:** night; see the route-specific spawn requirements below.
- **Player mount:** yes, when mature, tamed and saddled.
- **Flight:** supported by the species profile.
- **Minimum natural-spawn world age:** 12 ticking days.

These are source defaults for the unofficial **Fabric 1.20.1** port, updated
**2 November 2026**. Individual genetics, datapacks and server settings matter.

## Where to find it

- Natural spawning unlocks after **12 ticking days** (288,000 game ticks), in the Overworld on **Normal or Hard** difficulty, under sheltered sky.
- Deep route: at least **40 blocks below the surface**, **Y 0 or below**, block light **0**, and the rule's cave-space check of at least 60 air blocks.
- Dripstone/deep-dark route: dripstone caves or deep dark, at least **30 blocks below the surface**.
- Its activity habit is nighttime, but these cave spawn routes have no explicit time-of-day requirement. Cave roosts within 192 blocks and Hard difficulty add weight.
- The base roaming cap is 4, with a local cap of 1 within 192 blocks. The Normal-difficulty minimum means this natural rule does not qualify on Peaceful or Easy.

Spawn weights are selection inputs, not a guaranteed encounter rate. The general
spawn budget, local population and roost system can prevent an otherwise valid spawn.

## What to bring

- **Favorite food:** raw mutton.
- **Special food:** raw rabbit.
- **Avoid:** pufferfish.
- **Treasure preferences:** bone, flint.
- **Grooming tool:** a brush.

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
4. For a touch request, hold right-click with a **brush**, normally for
   about 3 seconds. Follow any extra crouch/tool prompt.
5. When it offers its back, right-click with an empty hand and follow the
   bucking/taming prompts. After it accepts you, equip a saddle to ride normally.

**Available request pool:** approach slowly, spar when invited, touch/groom, provide live prey, share a meal, rest beside it, walk together, hunt together, bring treasure, mount/bucking.

**Species-specific advice:** Its TOUCH request specifically uses **a brush** (`minecraft:brush`, from `#bookofdragons:brushes`). Hold right-click with that item when asked to groom it. The species likes live-prey offerings.

## Abilities and riding

**Primary:** Fire breath (`bookofdragons:whispering_fire`) and bite.

**Secondary:** No separate secondary ability is assigned in its species class. Its default profile allows riding and flight. Use a clear space for riding and takeoff.

Default rider controls: **G** inventory, **Space** jump/charged takeoff,
**left mouse** primary attack, **C** secondary where assigned, **Left Ctrl**
ground sprint/flight brake, **right mouse** free camera, **R** dismount
(confirm again in flight). Check your actual key bindings.

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

The default entity loot table lists **3-6
whisperingdeath scales** as the base death drop, before Looting adjustments.

For renewable collection, **right-click your own tamed dragon with shears**
when its shearing cooldown is clear. The shared implementation drops **1-2
scales**, consumes substantial shear durability, and sets a **6,000-12,000-tick**
cooldown (about 5-10 minutes at 20 TPS). Keep the dragon ticking for that timer.
This harvesting interaction requires ownership/taming; it is not a wild-taming step.
Recipe uses and pack overrides should be checked in your recipe viewer.

## Sources and coverage

- [whispering_death.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/fauna/species/whispering_death.json)
- [whispering_death.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/spawn/whispering_death.json)
- [WhisperingDeathEntity.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/custom/dragon/WhisperingDeathEntity.java)
- [DragonType.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/data/DragonType.java)
- [EggHatchCondition.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/data/EggHatchCondition.java)
- [whispering_death.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/loot_tables/entities/whispering_death.json)
- [ShearingComponent.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/component/ShearingComponent.java)

These pages describe shipped data/code, not a claim that every natural taming
sequence and secondary ability has been exhaustively playtested. Central client,
riding, persistence and ownership checks have passed; see the verification docs.

[Shared taming mechanics](TAMING_AND_SPAWNING.md) | [Wiki index](README.md) | [Player controls/admin guide](../PLAYER_GUIDE.md)
