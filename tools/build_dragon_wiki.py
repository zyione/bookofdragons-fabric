"""Build complete, source-backed species pages for the Markdown wiki.

Food, requests, activity, IDs and capabilities come from the checked-in game
data/code. The curated spawn/ability/egg descriptions must be reviewed against
their linked sources whenever those sources change. No gameplay files are edited.
"""
from pathlib import Path
import hashlib
import json
import re

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT/'bookofdragons/src/main/resources/data/bookofdragons'
JAVA = ROOT/'bookofdragons/src/main/java/net/magister/bookofdragons'
OUT = ROOT/'docs/dragon-wiki'
BASE = 'https://github.com/zyione/bookofdragons-fabric/blob/codex/fabric-1.20.1/'

PROFILES = {
    'nightfury': {
        'name':'Night Fury', 'java':'NightfuryEntity',
        'where':[
            'Natural spawning unlocks after **20 days of world ticking** (480,000 game ticks). The normal rule requires **nighttime** in the Overworld.',
            'Search cold mountains, snowy regions and taiga. The cold-biome tag also includes windswept hills/forests, frozen rivers, cold beaches and cold/frozen oceans.',
            'Sky route: **60-120 blocks above terrain** in a tagged cold biome. Peak route: ground at **Y 140 or higher**, with exposed sky, in a tagged cold biome.',
            'Sea-cliff route: snowy beach, stony shore, frozen ocean or cold ocean, with exposed sky and a steep slope (rise of at least 8 over a radius of 4).',
            'The normal rule places it at least **64 blocks from players**. New moon, thunder and an empty cliff roost within 160 blocks add spawn weight; they are not all mandatory.',
            'Normal roaming rules specify a world cap of 2 and a local cap of 1 within 384 blocks. Other population/roost rules also apply.',
            'The separate **albino** rule requires **40 ticking days**, a **full moon**, nighttime, and exposed frozen/jagged peaks at Y 140+. Its listed world cap is 1.'],
        'egg':'Activate in combined light **1 or below**. An enclosed dark chamber is more reliable than an exposed surface.',
        'primary':'Fury bolt ranged attack (`bookofdragons:fury_bolt`) and bite.',
        'secondary':'**Lightning Strike**: a close-range lightning-themed escape ability with temporary invisibility for the dragon and rider. It has its own cooldown and costs; it is not a requirement for taming.',
        'extra':'Its species matrix **dislikes live-prey offerings**. Use its actual meal, touch, movement or treasure requests instead of copying a Skrill taming routine.'},
    'deadly_nadder': {
        'name':'Deadly Nadder', 'java':'DeadlynadderEntity',
        'where':[
            'Natural spawning requires the Overworld and **daytime**. No additional minimum world age is specified in this species rule.',
            'Forest/jungle route: a biome tagged as forest or jungle, or cherry grove, with temperature at least **0.5** and exposed sky.',
            'Flower route: flower forest, meadow, sunflower plains or cherry grove, with at least **3 flower blocks within 4 blocks**.',
            'Typical groups are 1-2; the flower route uses 2-3. The base local cap is 4 within 128 blocks. Glade roosts within 160 blocks and clear weather add spawn weight.'],
        'egg':'Activate with **block light 10 or above** at the egg. Daylight alone is not the block-light condition; use suitable nearby light sources.',
        'primary':'Beam breath (`bookofdragons:precise_beam`) and bite.',
        'secondary':'**Spike Barrage** is its registered secondary ability. Availability follows the ability checks and cooldowns.',
        'extra':'Its shipped taming pool includes touch and movement activities, but **does not include sparring, hunting or a gift stage**. Do not attack it because another species asks for sparring.'},
    'gronckle': {
        'name':'Gronckle', 'java':'GronckleEntity',
        'where':[
            'Search the Overworld. No additional minimum world age is specified in this rule. Its activity habit is daytime, but the cave routes do **not** impose a time-of-day gate.',
            'Shallow caves: sheltered spots **10-30 blocks below the surface**; groups of 2-4.',
            'Ore caves: sheltered spots **10-40 blocks below the surface**, near at least 2 coal/iron/copper ore blocks within 4 blocks; groups of 2-3.',
            'Rockfields: hill-tagged biomes, stony shore or stony peaks during its active daytime, standing over overworld base stone or gravel.',
            'The cave routes have a local cap of 6 within 96 blocks; the base rule has 4 within 128. A cave roost within 128 blocks adds weight.'],
        'egg':'No special environmental activation condition is specified. Place the egg and keep it loaded while incubation progresses.',
        'primary':'Magma-ball ranged attack (`bookofdragons:magmaball`) and bite.',
        'secondary':'No separate secondary combat ability is assigned in its species class. Its distinctive extra mechanic is **Gronckle Iron production**.',
        'extra':'The species **likes grooming** and **dislikes live-prey offerings**. Its special food is gold ore, and its treasure preferences include raw iron, iron ore and cobblestone.',
        'special':'''## Gronckle Iron production

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
'''},
    'hideous_zippleback': {
        'name':'Hideous Zippleback', 'java':'HiddeousZipplebackEntity',
        'where':[
            'Natural spawning requires the Overworld, **nighttime**, **Y 50 or above**, and exposed sky. No additional world-age minimum is specified.',
            'Search swamp, mangrove swamp or mushroom fields. A separate route allows river-tagged biomes **while raining**.',
            'Typical groups are 1-2, with a local cap of 3 within 128 blocks. Wetland roosts within 160 blocks, rain and a new moon add weight.'],
        'egg':'Activate with **water at the egg position AND combined light 3 or below**. Both conditions must hold during activation.',
        'primary':'Gas breath (`bookofdragons:gas_breath`) and bite.',
        'secondary':'**Gas Ignite** interacts with its gas attack. Learn the gas/ignition combination in an open area before using it near builds.',
        'extra':'The species **likes grooming**. **Pufferfish is its special food** in this build; do not apply a Night Fury\'s disliked-food list to it. Its shipped taming pool does not include sparring or a gift stage.'},
    'monstrous_nightmare': {
        'name':'Monstrous Nightmare', 'java':'MonstrousnightmareEntity',
        'where':[
            'Natural spawning unlocks after **8 ticking days** (192,000 game ticks), in the Overworld, during its active **daytime**, with exposed sky.',
            'Arid route: temperature at least **1.5**, no precipitation, and no nearby creatures within the rule\'s 96-block check.',
            'Lava-pool route: temperature at least **0.8**, lava within 5 blocks, and no nearby creatures within its 64-block check.',
            'Barrens-roost route: airborne **16-40 blocks above terrain**, within 128 blocks of a barrens roost.',
            'The base roaming cap is 4, with 1 within 192 blocks locally (160 for the roost route). Very hot temperatures, clear weather and Hard difficulty add weight; Hard is not a universal requirement.'],
        'egg':'Activate with **fire or soul fire at the egg\'s block position**. A nearby campfire or lava pool does not satisfy this specific fire-block check.',
        'primary':'Flame breath (`bookofdragons:standard_flame`) and bite.',
        'secondary':'**Self-Immolation** is its registered secondary ability. Treat its fire effects as dangerous around other entities and builds.',
        'extra':'The species **hates grooming** and **likes live prey**. It has no TOUCH stage in its shipped taming pool. Follow invited sparring, meal, movement or treasure requests rather than repeatedly trying to pet it.'},
    'skrill': {
        'name':'Skrill', 'java':'SkrillEntity',
        'where':[
            'Natural spawning unlocks after **15 ticking days** (360,000 game ticks) in the Overworld. Its activity habit is **daytime**, but not every route requires daytime.',
            'Thunderstorm route: airborne **60-110 blocks above terrain** during thunder; this route has no separate biome restriction in the shipped file.',
            'Frozen-peak route: frozen peaks, jagged peaks, ice spikes or snowy slopes, during active **daytime**, **30-60 blocks above terrain**.',
            'Sea-squall route: ocean-tagged biomes during **rain**, **40-80 blocks above terrain**.',
            'Cliff roosts within 160 blocks and cold biomes add weight. The base roaming cap is 3, with a local cap of 1 within 256 blocks.'],
        'egg':'Activate with **fire or soul fire at the egg\'s block position** in this build. The spawn weather conditions are separate from the egg-activation check.',
        'primary':'Lightning beam (`bookofdragons:lightning_beam`) and bite.',
        'secondary':'**Lightning Shield** is its registered secondary ability. It is time-limited, can retaliate against attackers and has its own cooldown.',
        'extra':'The species **hates grooming** and **likes live prey**. It has no TOUCH stage in the shipped pool. A lightning rod is a treasure preference, not an instant-tame item or an instruction to strike it with lightning.'},
    'whispering_death': {
        'name':'Whispering Death', 'java':'WhisperingDeathEntity',
        'where':[
            'Natural spawning unlocks after **12 ticking days** (288,000 game ticks), in the Overworld on **Normal or Hard** difficulty, under sheltered sky.',
            'Deep route: at least **40 blocks below the surface**, **Y 0 or below**, block light **0**, and the rule\'s cave-space check of at least 60 air blocks.',
            'Dripstone/deep-dark route: dripstone caves or deep dark, at least **30 blocks below the surface**.',
            'Its activity habit is nighttime, but these cave spawn routes have no explicit time-of-day requirement. Cave roosts within 192 blocks and Hard difficulty add weight.',
            'The base roaming cap is 4, with a local cap of 1 within 192 blocks. The Normal-difficulty minimum means this natural rule does not qualify on Peaceful or Easy.'],
        'egg':'No special environmental activation condition is specified. Place the egg and keep it loaded while incubation progresses.',
        'primary':'Fire breath (`bookofdragons:whispering_fire`) and bite.',
        'secondary':'No separate secondary ability is assigned in its species class. Its default profile allows riding and flight. Use a clear space for riding and takeoff.',
        'extra':'Its TOUCH request specifically uses **a brush** (`minecraft:brush`, from `#bookofdragons:brushes`). Hold right-click with that item when asked to groom it. The species likes live-prey offerings.'},
    'terrible_terror': {
        'name':'Terrible Terror', 'java':'TerribleTerrorEntity',
        'where':[
            'Natural spawning requires the Overworld and active **daytime**. No additional world-age minimum is specified.',
            'Search beach-tagged biomes, stony shore, ocean-tagged sea-stack areas, or jungle-tagged biomes with water within 5 blocks.',
            'Typical groups are 2-4 (2-3 on the jungle route). Fisherman points of interest within 48 blocks add weight and use groups of 2-3.',
            'A full moon adds weight and can increase the group to 5-8. This does **not** remove the base daytime requirement. Base local cap: 8 within 64 blocks; special routes can change it.'],
        'egg':'No special environmental activation condition is specified. Place the egg and keep it loaded while incubation progresses.',
        'primary':'Small flame breath (`bookofdragons:terror_flame`) and bite.',
        'secondary':'No separate secondary ability is assigned in its species class. It is a flying **companion that supports perching**, not a player mount.',
        'extra':'The species **likes grooming** and **dislikes live prey**. Once you own it, an empty-main-hand right-click without crouching beckons it for its perching interaction. It has no MOUNT stage; do not wait for a riding ritual.'},
    'speed_stinger': {
        'name':'Speed Stinger', 'java':'SpeedStingerEntity',
        'where':[
            'Natural spawning unlocks after **6 ticking days** (144,000 game ticks), in the Overworld during active **nighttime**, with **block light 0**.',
            'Search taiga-tagged biomes, grove, dark forest, snowy plains or snowy slopes.',
            'Base groups are 2-4. A full moon adds weight and changes the group to 3-5. The local cap is 6 within 96 blocks.'],
        'egg':'No special environmental activation condition is specified. Place the egg and keep it loaded while incubation progresses.',
        'primary':'**Melee only**: sting and bite attacks. It has no ranged breath in its species attack definition.',
        'secondary':'A **Sting** ability is registered. This is a ground companion: its profile explicitly disables both player riding and flight, while allowing jumping and opening doors.',
        'extra':'The species **dislikes grooming** and **likes live prey**. It has neither TOUCH nor MOUNT in the shipped taming pool. Complete the social requests it actually gives; a saddle will not make it a mount.'},
}

RAW = {name:'raw '+name for name in ['salmon','cod','chicken','beef','mutton','rabbit']}
LABELS = {'APPROACH':'approach slowly','DEFEND':'defend it','FOLLOW':'play tag',
          'TOUCH':'touch/groom','MEAL':'share a meal','REST':'rest beside it',
          'WALK':'walk together','GIFT':'bring treasure','MOUNT':'mount/bucking',
          'SPAR':'spar when invited','HUNT':'provide live prey','HUNT_TOGETHER':'hunt together'}

def item_name(item):
    key = item.split(':',1)[-1]
    return RAW.get(key, key.replace('_',' '))

def listed(items):
    return ', '.join(item_name(item) for item in items)

def bullets(items):
    return '\n'.join('- '+line for line in items)

def link(path, label):
    return f'[{label}]({BASE}{path.as_posix()})'

OUT.mkdir(parents=True, exist_ok=True)
inventory = []
for key, info in PROFILES.items():
    species_path = DATA/'fauna/species'/(key+'.json')
    spawn_path = DATA/'spawn'/(key+'.json')
    entity_path = JAVA/'entity/custom/dragon'/(info['java']+'.java')
    loot_path = DATA/'loot_tables/entities'/(json.loads(spawn_path.read_text())['entity']+'.json')
    species = json.loads(species_path.read_text())
    spawn = json.loads(spawn_path.read_text())
    java = entity_path.read_text()
    identity = species['identity']
    stages = species['taming']['stages']
    checks = [stage['check'] for stage in stages]
    rideable = '.canBeRidden(false)' not in java
    flying = '.canFly(false)' not in java
    entity_id = 'bookofdragons:'+spawn['entity']
    name = info['name']
    activity = species['habit']['active'].lower()
    age = next((c['world_age']['min_days'] for c in spawn['when'] if 'world_age' in c), 0)
    profile = f'''## Quick profile

- **Class:** {identity['class'].title()} in this build's data.
- **Entity ID:** `{entity_id}`.
- **Activity habit:** {activity}; see the route-specific spawn requirements below.
- **Player mount:** {'yes, when mature, tamed and saddled' if rideable else 'no'}.
- **Flight:** {'supported by the species profile' if flying else 'disabled by the species profile'}.
- **Minimum natural-spawn world age:** {str(age)+' ticking days' if age else 'no additional age gate in this species rule'}.

These are source defaults for the unofficial **Fabric 1.20.1** port, updated
**2 November 2026**. Individual genetics, datapacks and server settings matter.
'''
    where = '## Where to find it\n\n'+bullets(info['where'])+'\n\nSpawn weights are selection inputs, not a guaranteed encounter rate. The general\nspawn budget, local population and roost system can prevent an otherwise valid spawn.\n'
    food = f'''## What to bring

- **Favorite food:** {listed(identity['favorite_food'])}.
- **Special food:** {listed(identity['special_food'])}.
- **Avoid:** {listed(identity['disliked_food'])}.
'''
    if identity.get('hoard_item'): food += '- **Treasure preferences:** '+listed(identity['hoard_item'])+'.\n'
    if any(s.get('item') == '#bookofdragons:brushes' for s in stages): food += '- **Grooming tool:** a brush.\n'
    food += '\nBring several accepted food items, an empty-hand slot, and survival equipment.\nA wild dragon can refuse hand-feeding until it is begging/asking you for a meal.\n'
    if 'GIFT' in checks:
        food += 'Drop treasure within reach when it requests a gift, then let it collect the item.\n'
    if rideable: food += 'Bring a saddle for after taming; it does not skip the taming process.\n'
    pool = ', '.join(LABELS[c] for c in checks)
    taming = f'''## How to tame it

1. Approach calmly and read its current message/book guidance.
2. Respond to the requested activity using the supplies above. Feed only when
   it accepts an offer; a refused meal does not mean you should attack it.
3. Build the relationship through the activities it actually requests.
   **Do not treat the list below as a fixed sequence or a fixed number of feeds.**
'''
    if 'TOUCH' in checks:
        tool = 'a **brush**' if any(s.get('item') for s in stages if s['check']=='TOUCH') else 'an **empty hand**'
        taming += f'4. For a touch request, hold right-click with {tool}, normally for\n   about 3 seconds. Follow any extra crouch/tool prompt.\n'
    else: taming += '4. This species does not have a TOUCH step in its shipped pool.\n'
    if rideable:
        taming += '5. When it offers its back, right-click with an empty hand and follow the\n   bucking/taming prompts. After it accepts you, equip a saddle to ride normally.\n'
    else:
        taming += '5. It has no mounting trial in this pool. Finish the relationship interactions\n   it requests; do not try to force a ride as the final step.\n'
    taming += f'\n**Available request pool:** {pool}.\n\n**Species-specific advice:** {info["extra"]}\n'
    abilities = f'''## Abilities and riding

**Primary:** {info['primary']}

**Secondary:** {info['secondary']}
'''
    if rideable:
        abilities += '\nDefault rider controls: **G** inventory, **Space** jump/charged takeoff,\n**left mouse** primary attack, **C** secondary where assigned, **Left Ctrl**\nground sprint/flight brake, **right mouse** free camera, **R** dismount\n(confirm again in flight). Check your actual key bindings.\n'
    else:
        abilities += '\nRider key instructions do not make a non-mount rideable. Its attacks belong\nto the companion/AI system; do not expect a mounted C-key interface.\n'
    abilities += '\nAge, injuries, stamina, relationship state and cooldowns can prevent an action.\nUse the book/on-screen feedback for the individual dragon rather than assuming\nfixed health, damage or speed values apply to every genetic variant.\n'
    eggs = f'''## Eggs, growth and care

**Egg activation:** {info['egg']}

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
'''
    loot = json.loads(loot_path.read_text())
    entry = loot['pools'][0]['entries'][0]
    count = next(f['count'] for f in entry['functions'] if f['function']=='minecraft:set_count')
    materials = f'''## Scales and materials

The default entity loot table lists **{int(count['min'])}-{int(count['max'])}
{item_name(entry['name'])}** as the base death drop, before Looting adjustments.

For renewable collection, **right-click your own tamed dragon with shears**
when its shearing cooldown is clear. The shared implementation drops **1-2
scales**, consumes substantial shear durability, and sets a **6,000-12,000-tick**
cooldown (about 5-10 minutes at 20 TPS). Keep the dragon ticking for that timer.
This harvesting interaction requires ownership/taming; it is not a wild-taming step.
Recipe uses and pack overrides should be checked in your recipe viewer.
'''
    reference_paths = [species_path, spawn_path, entity_path,
                       JAVA/'entity/data/DragonType.java', JAVA/'entity/data/EggHatchCondition.java',
                       loot_path, JAVA/'entity/component/ShearingComponent.java']
    if key=='gronckle': reference_paths.append(JAVA/'entity/custom/dragon/GronckleIronRecipe.java')
    sources = '## Sources and coverage\n\n'+bullets([link(p.relative_to(ROOT),p.name) for p in reference_paths])
    sources += '\n\nThese pages describe shipped data/code, not a claim that every natural taming\nsequence and secondary ability has been exhaustively playtested. Central client,\nriding, persistence and ownership checks have passed; see the verification docs.\n'
    sources += '\n[Shared taming mechanics](TAMING_AND_SPAWNING.md) | [Wiki index](README.md) | [Player controls/admin guide](../PLAYER_GUIDE.md)\n'
    page = '# '+name+'\n\n'+ '\n'.join(part for part in [profile,where,food,taming,abilities,eggs,materials,info.get('special',''),sources] if part)
    (OUT/(key+'.md')).write_text(page, encoding='utf-8', newline='\n')
    inventory.append({'species':key,'entityId':entity_id,'name':name,'requestPool':checks,
                      'favoriteFood':identity['favorite_food'],'specialFood':identity['special_food'],
                      'dislikedFood':identity['disliked_food'],'rideable':rideable,'flying':flying,
                      'sourceHashes':{p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in reference_paths}})

(OUT/'source-inventory.json').write_text(json.dumps(inventory,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'speciesPages':len(inventory),'output':str(OUT)}))
