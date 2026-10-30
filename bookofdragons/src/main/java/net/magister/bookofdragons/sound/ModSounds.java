package net.magister.bookofdragons.sound;

import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.magister.liberanimalium.platform.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
   public static final Registrar<SoundEvent> SOUND_EVENTS = Services.registries().create(Registries.SOUND_EVENT, "bookofdragons");
   public static final RegEntry<SoundEvent> ANIMAL_SQUEAK = registerSoundEvent("animal_squeak");
   public static final RegEntry<SoundEvent> BALL_SQUEAK = registerSoundEvent("ball_squeak");
   public static final RegEntry<SoundEvent> DEADLYNADDER_CURIOUS = registerSoundEvent("deadlynadder_curious");
   public static final RegEntry<SoundEvent> DEADLYNADDER_GROWL = registerSoundEvent("deadlynadder_growl");
   public static final RegEntry<SoundEvent> DEADLYNADDER_HURT = registerSoundEvent("deadlynadder_hurt");
   public static final RegEntry<SoundEvent> DEADLYNADDER_IDLE = registerSoundEvent("deadlynadder_idle");
   public static final RegEntry<SoundEvent> DEADLYNADDER_ROAR = registerSoundEvent("deadlynadder_roar");
   public static final RegEntry<SoundEvent> DRAGON_WHISTLE = registerSoundEvent("dragon_whistle");
   public static final RegEntry<SoundEvent> GRONCKLE_FIRE = registerSoundEvent("gronckle_fire");
   public static final RegEntry<SoundEvent> GRONCKLE_IDLE = registerSoundEvent("gronckle_idle");
   public static final RegEntry<SoundEvent> HEAVY_BITE = registerSoundEvent("heavy_bite");
   public static final RegEntry<SoundEvent> HIDEOUSZIPPLEBACK_BREATH_END = registerSoundEvent("hideouszippleback_breath_end");
   public static final RegEntry<SoundEvent> HIDEOUSZIPPLEBACK_BREATH_LOOP = registerSoundEvent("hideouszippleback_breath_loop");
   public static final RegEntry<SoundEvent> HIDEOUSZIPPLEBACK_BREATH_START = registerSoundEvent("hideouszippleback_breath_start");
   public static final RegEntry<SoundEvent> HIDEOUSZIPPLEBACK_DEATH = registerSoundEvent("hideouszippleback_death");
   public static final RegEntry<SoundEvent> HIDEOUSZIPPLEBACK_HURT = registerSoundEvent("hideouszippleback_hurt");
   public static final RegEntry<SoundEvent> HIDEOUSZIPPLEBACK_IDLE = registerSoundEvent("hideouszippleback_idle");
   public static final RegEntry<SoundEvent> HIDEOUSZIPPLEBACK_IGNITION = registerSoundEvent("hideouszippleback_ignition");
   public static final RegEntry<SoundEvent> HIDEOUSZIPPLEBACK_ROAR = registerSoundEvent("hideouszippleback_roar");
   public static final RegEntry<SoundEvent> HIDEOUSZIPPLEBACK_SLEEP = registerSoundEvent("hideouszippleback_sleep");
   public static final RegEntry<SoundEvent> LARGE_FLAP = registerSoundEvent("large_flap");
   public static final RegEntry<SoundEvent> LIGHT_BITE = registerSoundEvent("light_bite");
   public static final RegEntry<SoundEvent> MEDIUM_FLAP = registerSoundEvent("medium_flap");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_ABILITY_END = registerSoundEvent("monstrousnightmare_ability_end");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_ABILITY_LOOP = registerSoundEvent("monstrousnightmare_ability_loop");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_ABILITY_START = registerSoundEvent("monstrousnightmare_ability_start");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_BREATH_END = registerSoundEvent("monstrousnightmare_breath_end");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_BREATH_LOOP = registerSoundEvent("monstrousnightmare_breath_loop");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_BREATH_START = registerSoundEvent("monstrousnightmare_breath_start");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_BUCK = registerSoundEvent("monstrousnightmare_buck");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_CURIOUS = registerSoundEvent("monstrousnightmare_curious");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_DEATH = registerSoundEvent("monstrousnightmare_death");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_FRIENDLY = registerSoundEvent("monstrousnightmare_friendly");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_GROWL = registerSoundEvent("monstrousnightmare_growl");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_HURT = registerSoundEvent("monstrousnightmare_hurt");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_IDLE = registerSoundEvent("monstrousnightmare_idle");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_PASSIVE = registerSoundEvent("monstrousnightmare_passive");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_PET = registerSoundEvent("monstrousnightmare_pet");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_ROAR = registerSoundEvent("monstrousnightmare_roar");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_SLEEP = registerSoundEvent("monstrousnightmare_sleep");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_WARY_FIGHT = registerSoundEvent("monstrousnightmare_wary_fight");
   public static final RegEntry<SoundEvent> MONSTROUSNIGHTMARE_WARY_FLEE = registerSoundEvent("monstrousnightmare_wary_flee");
   public static final RegEntry<SoundEvent> NIGHTFURY_DEATH = registerSoundEvent("nightfury_death");
   public static final RegEntry<SoundEvent> NIGHTFURY_DIVE_END = registerSoundEvent("nightfury_dive_end");
   public static final RegEntry<SoundEvent> NIGHTFURY_DIVE_LOOP = registerSoundEvent("nightfury_dive_loop");
   public static final RegEntry<SoundEvent> NIGHTFURY_DIVE_START = registerSoundEvent("nightfury_dive_start");
   public static final RegEntry<SoundEvent> NIGHTFURY_FIRE = registerSoundEvent("nightfury_fire");
   public static final RegEntry<SoundEvent> NIGHTFURY_GROWL = registerSoundEvent("nightfury_growl");
   public static final RegEntry<SoundEvent> NIGHTFURY_HURT = registerSoundEvent("nightfury_hurt");
   public static final RegEntry<SoundEvent> NIGHTFURY_IDLE = registerSoundEvent("nightfury_idle");
   public static final RegEntry<SoundEvent> NIGHTFURY_PET = registerSoundEvent("nightfury_pet");
   public static final RegEntry<SoundEvent> NIGHTFURY_ROAR = registerSoundEvent("nightfury_roar");
   public static final RegEntry<SoundEvent> NIGHTFURY_SLEEP = registerSoundEvent("nightfury_sleep");
   public static final RegEntry<SoundEvent> PLUSH_LOW_SQUEAK = registerSoundEvent("plush_low_squeak");
   public static final RegEntry<SoundEvent> PLUSH_SQUEAK = registerSoundEvent("plush_squeak");
   public static final RegEntry<SoundEvent> RATTLESTAFF_USE = registerSoundEvent("rattlestaff_use");
   public static final RegEntry<SoundEvent> SKRILL_FIRE = registerSoundEvent("skrill_fire");
   public static final RegEntry<SoundEvent> SMALL_FLAP = registerSoundEvent("small_flap");
   public static final RegEntry<SoundEvent> TERRIBLETERROR_HURT = registerSoundEvent("terribleterror_hurt");
   public static final RegEntry<SoundEvent> TERRIBLETERROR_IDLE = registerSoundEvent("terribleterror_idle");
   public static final RegEntry<SoundEvent> TERRIBLETERROR_SLEEP = registerSoundEvent("terribleterror_sleep");
   public static final RegEntry<SoundEvent> TERRIBLETERROR_WARY_FIGHT = registerSoundEvent("terribleterror_wary_fight");
   public static final RegEntry<SoundEvent> TERRIBLETERROR_WARY_FLEE = registerSoundEvent("terribleterror_wary_flee");

   public static RegEntry<SoundEvent> registerSoundEvent(String name) {
      return SOUND_EVENTS.register(name, () -> SoundEvent.createFixedRangeEvent(new ResourceLocation("bookofdragons", name), SoundRanges.of(name)));
   }

   public static void register() {
   }
}
