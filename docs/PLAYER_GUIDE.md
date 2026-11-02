# Book of Dragons - Player Guide

For the unofficial Fabric 1.20.1 development port. Updated 2 November 2026.
Start with section 1, then follow the sections you need. Server datapacks,
configuration changes and remapped controls can change your experience.

## 01. Install the right files

Use **Minecraft Java 1.20.1**, **Java 17**, and **Fabric**.
The standalone setup was tested with Loader **0.18.2**; the tested
BigChadGuys Plus instance uses **0.19.5**. Keep a modpack's required newer
loader rather than downgrading it.

Close Minecraft and back up your world before installing or updating mods.
Get compiled jars from your pack/server organizer or the prepared release bundle.
Put these two normal gameplay jars in your instance's `mods` folder:

- `bookofdragons-fabric-1.4+1.20.1.fabric.dev.1.jar`
- `fauna-engine-fabric-1.0+1.20.1.fabric.dev.1.jar`

Required dependencies:

- **Fabric API 0.92.12+1.20.1**.
- **AzureLib 3.2.0 for Fabric 1.20.1**; tested version ID `VDXznzXQ`.
- **Forge Config API Port 8.0.1**; tested version ID `Qs8LaZLo`.
- **Mod Menu 7.2.2** is optional.

Forge Config API Port is a Fabric dependency. It does not mean you should
install Forge itself.

Use one copy of each required mod. Replace older copies when updating.
Keep Book of Dragons and FAUNA from the same current delivery. Do not mix in
their Forge versions or install `-sources.jar`, `-dev.jar`, or
`-verification.jar` files. The `.fabric.dev.1` inside the two filenames above
is part of this development release's name.

Start the game. If it reports missing dependencies, fix those before opening
a world. Existing Forge worlds have not been verified for migration.

## 02. Get started and open the book

**Quick start:** get the book, bring suitable food, approach a dragon slowly,
and follow its taming prompts. Once tamed, equip a saddle before trying to ride
a species that supports riding.

Getting the Book of Dragons:

- In Creative, look in the mod's Creative tab.
- The Hooligan Drifter has a book trade costing **1 iron ingot** in this build.
- With cheats or operator permission, use:

```text
/give @s bookofdragons:book_of_dragons
```

Hold the book and **right-click** to open it. Use its species pages, rider
manual, notes and **My Dragons** section to learn about your animals.
Information can depend on discovery and ownership.

To open an owned, tamed dragon's inventory while standing beside it,
**hold the book and Shift + right-click the dragon**.
While mounted, the default inventory key is **G**.

For crafting recipes, check the recipe viewer supplied by your pack. Recipe
changes made by a modpack may differ from this guide.

To get the alternate book appearance, rename the item exactly
**How to Train your Dragon** in an anvil.

## 03. How normal taming works

Taming is a **trust-building interaction system**. There is no universal
number of fish that instantly tames every dragon, and the requests are not a
fixed checklist in the order shown here.

1. Approach slowly. Avoid chasing, injuring or cornering the dragon.
2. Watch its messages and the book's guidance for its current request.
3. Respond to that request, then give it time to react.
4. Continue building trust until it accepts you. Rideable species can ask for
   a final mounting/bucking interaction.

Common requests:

- **Approach:** move toward it slowly rather than rushing in.
- **Meal:** offer suitable food when it is asking for it.
- **Touch:** use an empty hand and hold right-click, normally about **3 seconds**.
  Some requests can specify an item or crouching; follow that request.
- **Follow/tag or walk:** take part in the movement activity it requests.
- **Rest:** stay beside it while it rests.
- **Gift:** drop an item that species values nearby where it can reach it,
  and let the dragon pick it up when responding to the request.
- **Defend, spar or hunt:** only attempt the activity the dragon is actually
  requesting. These are not instructions to attack every wild dragon.
- **Mount:** right-click with an empty hand when it offers its back; follow the
  displayed taming prompts.

**If it refuses food:** a wild dragon normally accepts hand-feeding only while
begging/asking that player for food. Repeated clicking is not a way to bypass
that requirement. Avoid its disliked food.

Some species do not have a mounting stage. Follow their own requests rather
than waiting for every dragon to offer a ride.

## 04. Foods for the first five species

These are the shipped species preferences. **Raw** means the uncooked Minecraft
item. Modpack configuration or datapacks can change preferences.

**Night Fury**
- Favorite: raw salmon.
- Special food: tropical fish.
- Dislikes: pufferfish.
- Treasure: amethyst shards or echo shards.

**Deadly Nadder**
- Favorite: raw chicken.
- Special food: tropical fish.
- Dislikes: spider eyes.

**Monstrous Nightmare**
- Favorite: raw beef.
- Special food: tropical fish.
- Dislikes: rotten flesh.
- Treasure: coal, charcoal or blaze rods.

**Gronckle**
- Favorite: raw mutton.
- Special food: gold ore. This unusual preference is intentional in this build.
- Dislikes: pufferfish.
- Treasure: raw iron, iron ore or cobblestone.

**Hideous Zippleback**
- Favorite: raw salmon.
- Special food: pufferfish. Its preferences differ from a Night Fury's.
- Dislikes: rotten flesh.

Food and treasure are separate preferences. Bringing treasure does not mean
every dragon will eat it or accept it at every stage.

## 05. Foods for the other four species

**Skrill**
- Favorite: raw mutton.
- Special food: tropical fish.
- Dislikes: pufferfish.
- Treasure: iron ingots, copper ingots or a lightning rod.

**Whispering Death**
- Favorite: raw mutton.
- Special food: raw rabbit.
- Dislikes: pufferfish.
- Treasure: bones or flint.

**Terrible Terror**
- Favorite: raw cod.
- Special food: raw salmon.
- Dislikes: spider eyes.

**Speed Stinger**
- Favorite: raw salmon.
- Special food: raw cod.
- Dislikes: rotten flesh.

Other general meats and fish may be accepted, but the named favorite and
special foods are the exact species-specific entries in the shipped data.
Do not assume every dragon likes the same food or supports the same riding
and flight abilities.

## 06. Night Fury taming walkthrough

Bring **raw salmon**, some **tropical fish**, and optionally **amethyst or echo
shards** for a treasure request. Keep an empty hotbar slot for petting.

1. Locate a Night Fury and approach calmly. Do not hit it to start taming.
2. Follow its current request. If it wants food, right-click with an accepted
   food item. If it refuses, wait for a meal/begging request.
3. When it wants touch, switch to an empty hand and hold right-click for about
   3 seconds. Respond to its other requests as they appear.
4. When it offers its back, right-click with an empty hand and follow the
   bucking/taming prompts. Default letter keys are **F, G, H, J, K, L**, but
   the keys shown on your screen take priority.
5. After ownership is established, equip a **saddle** and mount it again.
6. Use a mature, healthy Night Fury for flight. Ownership alone does not make
   a baby ready to fly.

**"It has not offered me its back"** means the dragon is still going through
wild taming. Continue the requested activity, or use the separate admin
instructions in section 11 if you intentionally want to skip normal taming.

For this port, a Night Fury's entity ID is `bookofdragons:nightfury`.
Dragons supplied by Dragons of Berk are separate entities; do not assume the
two mods have identical taming rules or commands.

## 07. Saddles, inventory and care

For a tame dragon you own:

- Hold a **saddle** and right-click it to equip one.
- Empty your hand, release Shift, then right-click to mount a rideable dragon.
- Hold the book and **Shift + right-click** to open its inventory from outside.
- Press **G** while mounted to open the dragon inventory.
- A **chest** can be equipped by right-clicking with it once the required
  **Bonded** relationship stage is reached.

Species, age and relationship state affect what the dragon will allow. A
saddle does not give a ground-only species flight or turn a small species into
a full-size mount.

Feed accepted foods, allow rest and avoid hurting the dragon. Feeding young
dragons also advances growth. Favorite food gives a larger growth bonus than
other accepted food in the shipped implementation.

If abilities stop working, read the message: it may be tired, injured, too
young, on cooldown or not sufficiently bonded. Give it appropriate care rather
than repeatedly pressing the ability key.

**"It wants nothing to do with me"** indicates estrangement. The dragon may
refuse tack and commands even though it was previously tamed. Work on restoring
the relationship; forced ownership is not a substitute for every social state.

## 08. Default riding and ability controls

Check **Options > Controls > Key Binds**, under **Book of Dragons** and
**Book of Dragons: Taming**. Your saved bindings override these defaults.

- **W/A/S/D:** normal movement/steering inputs.
- **Space:** mounted jump/takeoff input; hold and release to charge takeoff
  when the mount is eligible. In flight it also supplies the boost input.
- **Left Ctrl:** ground sprint; the same bound key supplies braking in flight.
- **Left mouse:** primary attack/breath. Charge/release behavior varies by attack.
- **C:** secondary ability, when that species can use one.
- **Right mouse:** free-camera input while riding.
- **G:** dragon inventory while mounted.
- **R:** dismount. In flight, press again promptly when asked to confirm.
- **X:** grab/reach; availability depends on the dragon and situation.
- **F/G/H/J/K/L:** default taming minigame letter keys; follow the displayed keys.

For takeoff, use an eligible mature flying species, a saddle, sufficient health
and an open area. Movement/camera behavior also depends on the flight settings.
Practice takeoff, steering, braking and landing near a safe open area first.

In large packs, another mod can share a key. Rebind conflicting actions in
Controls. **Shift is not the dedicated dismount key listed by this mod**;
follow the displayed R-key confirmation when applicable.

## 09. Commands, whistles and finding your dragon

To cycle an owned dragon's stance, **empty both hands**, stand off the mount,
look at the dragon and **Shift + right-click**. This cycles its stance; read
the feedback message for the new state.

Dragon Whistle basics:

1. Right-click your tamed dragon while holding the whistle to bind it.
2. Use the bound whistle while standing outside in the **Overworld**, with
   a clear view of the sky, to request a call.
3. Read any refusal/status message. Calling is subject to ownership and the
   dragon's state; it is not a guarantee of unlimited instant teleportation.
4. **Shift + use the whistle in the air** to clear its binding.

The book's **My Dragons** pages help manage known owned dragons. Keep track
of where you leave them and the stance you selected.

Ownership matters in multiplayer: another player's dragon is not automatically
yours to command, equip or release. Client and server must both have compatible
mod versions installed for multiplayer play.

## 10. Eggs and hatching

Use an egg item on a block to place it. Right-click the placed egg for progress
and condition messages. **Crouch + right-click** picks it up; the item retains
its incubation data, and placing it again resumes that data.

Some eggs need an environment to **activate** before incubation:

- **Night Fury:** combined light level **1 or below**. Use an enclosed dark spot.
- **Deadly Nadder:** **block light 10 or above**; daylight alone is not the
  specific light measurement this condition checks.
- **Monstrous Nightmare:** fire or soul fire at the egg's block position.
- **Skrill:** fire or soul fire at the egg's block position in this build.
- **Hideous Zippleback:** water at its position **and** combined light **3 or below**.
- **Gronckle, Whispering Death, Terrible Terror, Speed Stinger:** no special
  activation environment is specified in the shipped type definitions.

Keep the required environment until the egg reports **Activated**. The current
implementation then counts down incubation while the egg is loaded and ticking.
Hatching duration varies; lag, configuration and an unloaded area can affect
the real-world wait. Use the egg's progress message rather than assuming a
fixed number of minutes.

Babies inherit genetic data. A newly hatched dragon still needs care and growth.
Do not assume every hatchling is automatically owned: lineage and taming state
matter. Full natural breeding and incubation playthroughs remain less thoroughly
tested than the automated hatch/persistence checks.

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

## 13. BigChadGuys and verification notes

On **31 October 2026**, the current port passed **10 client checks** in an
isolated copy of a specific BigChadGuys Plus instance:

- Minecraft **1.20.1**, Fabric Loader **0.19.5**, Java **17**.
- **409 original top-level mod jars**, copied configs and KubeJS scripts.
- The original **34 selected resource-pack entries**.
- Iris **1.7.6** with **ComplementaryReimagined_r5.9.3** enabled.
- Book/item textures, nine species' genetic appearances, ownership commands,
  saddle/mounting, menus, note synchronization, flight, breath and dismounting.

That test used a neutral **Human/Nitwit** character. On first joining a BCG
world, complete the normal Origins/class choices before expecting normal play.
The source instance's mod jars and existing saves were not changed by testing.

This is evidence for that configuration, not a guarantee for every pack update,
Origins/class combination, existing world or multiplayer server. Natural taming,
long breeding sessions and every cross-mod interaction have not all been
exhaustively playtested. The compatibility run was not a performance benchmark.

For a different pack, keep the versions compatible, back up saves and try a
new test world before committing a long-running world to this development port.

Project and source:
https://github.com/zyione/bookofdragons-fabric/tree/codex/fabric-1.20.1
