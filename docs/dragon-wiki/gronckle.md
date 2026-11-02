# Gronckle

## Quick profile

- **Class:** Boulder in this build's data.
- **Entity ID:** `bookofdragons:gronckle`.
- **Activity habit:** day; see the route-specific spawn requirements below.
- **Player mount:** yes, when mature, tamed and saddled.
- **Flight:** supported by the species profile.
- **Minimum natural-spawn world age:** no additional age gate in this species rule.

These are source defaults for the unofficial **Fabric 1.20.1** port, updated
**2 November 2026**. Individual genetics, datapacks and server settings matter.

## Where to find it

- Search the Overworld. No additional minimum world age is specified in this rule. Its activity habit is daytime, but the cave routes do **not** impose a time-of-day gate.
- Shallow caves: sheltered spots **10-30 blocks below the surface**; groups of 2-4.
- Ore caves: sheltered spots **10-40 blocks below the surface**, near at least 2 coal/iron/copper ore blocks within 4 blocks; groups of 2-3.
- Rockfields: hill-tagged biomes, stony shore or stony peaks during its active daytime, standing over overworld base stone or gravel.
- The cave routes have a local cap of 6 within 96 blocks; the base rule has 4 within 128. A cave roost within 128 blocks adds weight.

Spawn weights are selection inputs, not a guaranteed encounter rate. The general
spawn budget, local population and roost system can prevent an otherwise valid spawn.

## What to bring

- **Favorite food:** raw mutton.
- **Special food:** gold ore.
- **Avoid:** pufferfish.
- **Treasure preferences:** raw iron, iron ore, cobblestone.

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

**Available request pool:** touch/groom, share a meal, rest beside it, walk together, bring treasure, mount/bucking.

**Species-specific advice:** The species **likes grooming** and **dislikes live-prey offerings**. Its special food is gold ore, and its treasure preferences include raw iron, iron ore and cobblestone.

## Abilities and riding

**Primary:** Magma-ball ranged attack (`bookofdragons:magmaball`) and bite.

**Secondary:** No separate secondary combat ability is assigned in its species class. Its distinctive extra mechanic is **Gronckle Iron production**.

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
gronckle scales** as the base death drop, before Looting adjustments.

For renewable collection, **right-click your own tamed dragon with shears**
when its shearing cooldown is clear. The shared implementation drops **1-2
scales**, consumes substantial shear durability, and sets a **6,000-12,000-tick**
cooldown (about 5-10 minutes at 20 TPS). Keep the dragon ticking for that timer.
This harvesting interaction requires ownership/taming; it is not a wild-taming step.
Recipe uses and pack overrides should be checked in your recipe viewer.

## Gronckle Iron production

Once it is tamed, right-click it with these recipe ingredients, one item per
accepted interaction. The shipped recipe requires:

- **6 dripstone blocks**.
- **4 sandstone blocks**.
- **2 blocks of raw iron** (`minecraft:raw_iron_block`).
- **2 magma blocks**.

Watch its progress messages. Once every ingredient count is complete, it starts
a **6,000-tick timer** (about 5 minutes at 20 TPS), then drops **2-4 Gronckle
Iron** items and resets the recipe. Keep the dragon loaded and ticking.
The raw-iron **block** ingredient is different from the ordinary iron ore in
its treasure list. This recipe is a post-taming feature, not the taming recipe.
These are implementation defaults; this production path has not had an exhaustive
modpack playthrough.

## Sources and coverage

- [gronckle.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/fauna/species/gronckle.json)
- [gronckle.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/spawn/gronckle.json)
- [GronckleEntity.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/custom/dragon/GronckleEntity.java)
- [DragonType.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/data/DragonType.java)
- [EggHatchCondition.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/data/EggHatchCondition.java)
- [gronckle.json](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/resources/data/bookofdragons/loot_tables/entities/gronckle.json)
- [ShearingComponent.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/component/ShearingComponent.java)
- [GronckleIronRecipe.java](https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/bookofdragons/src/main/java/net/magister/bookofdragons/entity/custom/dragon/GronckleIronRecipe.java)

These pages describe shipped data/code, not a claim that every natural taming
sequence and secondary ability has been exhaustively playtested. Central client,
riding, persistence and ownership checks have passed; see the verification docs.

[Shared taming mechanics](TAMING_AND_SPAWNING.md) | [Wiki index](README.md) | [Player controls/admin guide](../PLAYER_GUIDE.md)
