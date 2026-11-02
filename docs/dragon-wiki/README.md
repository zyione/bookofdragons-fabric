# Book of Dragons - Dragon Wiki

Species guides for the **unofficial Fabric 1.20.1 port**. Updated **2 November
2026**. These pages describe this port's shipped data and implementation;
Dragons of Berk and other dragon mods can have different rules.

## Pick your dragon

- [Night Fury](nightfury.md) - cold skies and peaks, nighttime, 20 ticking days;
  salmon, tropical fish, empty-hand touch, amethyst/echo treasures.
- [Skrill](skrill.md) - storms, cold peaks and rainy oceans, 15 ticking days;
  mutton, tropical fish, invited combat/hunting and metal treasures; hates grooming.
- [Deadly Nadder](deadly_nadder.md) - warm forests, jungles and flower areas by
  day; chicken, tropical fish, touch and movement requests.
- [Gronckle](gronckle.md) - shallow/ore caves and rocky terrain; mutton, gold ore,
  grooming and rocky treasures; includes its Gronckle Iron production recipe.
- [Hideous Zippleback](hideous_zippleback.md) - swamps, mushroom fields and rainy
  rivers at night; salmon, pufferfish, grooming; gas breath and ignition.
- [Monstrous Nightmare](monstrous_nightmare.md) - hot/arid or lava areas by day,
  8 ticking days; beef, tropical fish, invited combat/hunting; hates grooming.
- [Whispering Death](whispering_death.md) - deep caves, 12 ticking days,
  Normal/Hard; mutton, rabbit, a brush for grooming, bones/flint for treasure.
- [Terrible Terror](terrible_terror.md) - coasts/oceans and waterside jungles by
  day; cod, salmon and grooming; a perching companion, not a player mount.
- [Speed Stinger](speed_stinger.md) - dark cold woods at night, 6 ticking days;
  salmon, cod and invited prey/combat activities; a ground companion, not a mount.

## What each page covers

Every species page has sections for its profile and exact entity ID, natural
spawn routes, world-age restrictions, food/treasure, taming request pool,
species-specific interaction advice, riding/abilities, egg activation, care and
scales/materials. The Gronckle page also covers its iron-production recipe.
The footer links to the relevant source data/code.

Read [shared taming and spawning mechanics](TAMING_AND_SPAWNING.md) for what
each request means and how real world age differs from changing the clock.
Use the [player guide](../PLAYER_GUIDE.md) for installation, general controls,
whistles, troubleshooting and the repaired admin force-taming commands.

## Using this on a site

Each dragon has one complete Markdown article with a title and section headings.
Use this index as the landing page and the shared mechanics page as general help.
The articles use ordinary Markdown without platform-specific components or
machine-local links. Preserve the relative links or adapt them to your site's
routes when importing the files.

The download bundle includes the player guide beside the `dragon-wiki` folder,
so the existing cross-links resolve within that layout. Keep the source links
at the bottom of articles if you want readers to check the implementation.

## Defaults and verification

Server settings, datapacks and individual genetics can change behavior. A
list of possible taming requests is **not a fixed sequence**, and there is no
universal feed count. A source-defined secondary ability is not a promise that
it is available at every age or relationship level.

The port has passed automated central gameplay checks, including a copied
BigChadGuys Plus setup. Natural taming of every species, every ability, every
Origins/class combination, long breeding sessions and multiplayer interactions
have not all been exhaustively playtested. See [verification](../VERIFICATION.md)
and [BCG compatibility](../BCG_COMPATIBILITY.md) for the actual coverage.

Health, damage and speed can depend on loaded definitions and genetic state;
this wiki does not invent a single fixed stat sheet for every individual.

## Maintaining the wiki

`tools/build_dragon_wiki.py` generates the species pages from checked-in food,
taming and activity data, plus reviewed spawn/ability/egg descriptions. Review
the curated descriptions when those underlying rules change. The JSON source
inventory records the file hashes used for the generated facts.
