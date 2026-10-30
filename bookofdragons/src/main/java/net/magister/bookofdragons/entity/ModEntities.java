package net.magister.bookofdragons.entity;

import net.magister.bookofdragons.entity.custom.HooliganDrifterEntity;
import net.magister.bookofdragons.entity.custom.dragon.DeadlynadderEntity;
import net.magister.bookofdragons.entity.custom.dragon.GronckleEntity;
import net.magister.bookofdragons.entity.custom.dragon.HiddeousZipplebackEntity;
import net.magister.bookofdragons.entity.custom.dragon.MonstrousnightmareEntity;
import net.magister.bookofdragons.entity.custom.dragon.NightfuryEntity;
import net.magister.bookofdragons.entity.custom.dragon.SkrillEntity;
import net.magister.bookofdragons.entity.custom.dragon.SpeedStingerEntity;
import net.magister.bookofdragons.entity.custom.dragon.TerribleTerrorEntity;
import net.magister.bookofdragons.entity.custom.dragon.WhisperingDeathEntity;
import net.magister.bookofdragons.entity.custom.egg.DragonEggEntity;
import net.magister.bookofdragons.entity.custom.toy.PlushEntity;
import net.magister.bookofdragons.entity.custom.toy.PlushKind;
import net.magister.bookofdragons.entity.projectile.SpikeProjectile;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.magister.liberanimalium.platform.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;

public class ModEntities {
   public static final Registrar<EntityType<?>> ENTITY_TYPES = Services.registries().create(Registries.ENTITY_TYPE, "bookofdragons");
   public static final RegEntry<EntityType<NightfuryEntity>> NIGHTFURY = ENTITY_TYPES.register(
      "nightfury",
      () -> Builder.of(NightfuryEntity::new, MobCategory.CREATURE)
         .sized(1.0F, 1.8F)
         .build(new ResourceLocation("bookofdragons", "nightfury").toString())
   );
   public static final RegEntry<EntityType<DeadlynadderEntity>> DEADLYNADDER = ENTITY_TYPES.register(
      "deadlynadder",
      () -> Builder.of(DeadlynadderEntity::new, MobCategory.CREATURE)
         .sized(1.8F, 2.8F)
         .build(new ResourceLocation("bookofdragons", "deadlynadder").toString())
   );
   public static final RegEntry<EntityType<SkrillEntity>> SKRILL = ENTITY_TYPES.register(
      "skrill",
      () -> Builder.of(SkrillEntity::new, MobCategory.CREATURE)
         .sized(1.8F, 2.5F)
         .build(new ResourceLocation("bookofdragons", "skrill").toString())
   );
   public static final RegEntry<EntityType<MonstrousnightmareEntity>> MONSTROUSNIGHTMARE = ENTITY_TYPES.register(
      "monstrousnightmare",
      () -> Builder.of(MonstrousnightmareEntity::new, MobCategory.CREATURE)
         .sized(2.0F, 2.0F)
         .build(new ResourceLocation("bookofdragons", "monstrousnightmare").toString())
   );
   public static final RegEntry<EntityType<GronckleEntity>> GRONCKLE = ENTITY_TYPES.register(
      "gronckle",
      () -> Builder.of(GronckleEntity::new, MobCategory.CREATURE)
         .sized(1.8F, 1.8F)
         .build(new ResourceLocation("bookofdragons", "gronckle").toString())
   );
   public static final RegEntry<EntityType<HiddeousZipplebackEntity>> HIDDEOUS_ZIPPLEBACK = ENTITY_TYPES.register(
      "hiddeous_zippleback",
      () -> Builder.of(HiddeousZipplebackEntity::new, MobCategory.CREATURE)
         .sized(1.8F, 1.8F)
         .build(new ResourceLocation("bookofdragons", "hiddeous_zippleback").toString())
   );
   public static final RegEntry<EntityType<WhisperingDeathEntity>> WHISPERING_DEATH = ENTITY_TYPES.register(
      "whispering_death",
      () -> Builder.of(WhisperingDeathEntity::new, MobCategory.CREATURE)
         .sized(0.9F, 1.3F)
         .build(new ResourceLocation("bookofdragons", "whispering_death").toString())
   );
   public static final RegEntry<EntityType<TerribleTerrorEntity>> TERRIBLE_TERROR = ENTITY_TYPES.register(
      "terrible_terror",
      () -> Builder.of(TerribleTerrorEntity::new, MobCategory.CREATURE)
         .sized(0.33F, 0.7F)
         .build(new ResourceLocation("bookofdragons", "terrible_terror").toString())
   );
   public static final RegEntry<EntityType<SpeedStingerEntity>> SPEED_STINGER = ENTITY_TYPES.register(
      "speed_stinger",
      () -> Builder.of(SpeedStingerEntity::new, MobCategory.CREATURE)
         .sized(0.6F, 1.5F)
         .build(new ResourceLocation("bookofdragons", "speed_stinger").toString())
   );
   public static final RegEntry<EntityType<DragonEggEntity>> DRAGON_EGG_ENTITY = ENTITY_TYPES.register(
      "dragon_egg",
      () -> Builder.<DragonEggEntity>of(DragonEggEntity::new, MobCategory.MISC)
         .sized(0.8F, 1.0F)
         .fireImmune()
         .clientTrackingRange(8)
         .updateInterval(3)
         .build(new ResourceLocation("bookofdragons", "dragon_egg").toString())
   );
   public static final RegEntry<EntityType<HooliganDrifterEntity>> HOOLIGAN_DRIFTER = ENTITY_TYPES.register(
      "hooligan_drifter",
      () -> Builder.of(HooliganDrifterEntity::new, MobCategory.CREATURE)
         .sized(0.6F, 1.95F)
         .build(new ResourceLocation("bookofdragons", "hooligan_drifter").toString())
   );
   public static final RegEntry<EntityType<SpikeProjectile>> SPIKE_PROJECTILE = ENTITY_TYPES.register(
      "spike_projectile",
      () -> Builder.<SpikeProjectile>of(SpikeProjectile::new, MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(1).build("spike_projectile")
   );
   public static final RegEntry<EntityType<PlushEntity>> SHEEP_PLUSH = plush(PlushKind.SHEEP);
   public static final RegEntry<EntityType<PlushEntity>> FURY_PLUSH = plush(PlushKind.FURY);
   public static final RegEntry<EntityType<PlushEntity>> FISH_PLUSH = plush(PlushKind.FISH);
   public static final RegEntry<EntityType<PlushEntity>> STEAK_PLUSH = plush(PlushKind.STEAK);
   public static final RegEntry<EntityType<PlushEntity>> BALL_PLUSH = plush(PlushKind.BALL);
   public static final RegEntry<EntityType<PlushEntity>> TERROR_PLUSH = plush(PlushKind.TERROR);
   public static final RegEntry<EntityType<PlushEntity>> NIGHTMARE_PLUSH = plush(PlushKind.NIGHTMARE);

   private static RegEntry<EntityType<PlushEntity>> plush(PlushKind kind) {
      return ENTITY_TYPES.register(
         kind.id(),
         () -> Builder.of(PlushEntity::new, MobCategory.MISC)
            .sized(kind.width(), kind.height())
            .fireImmune()
            .clientTrackingRange(8)
            .updateInterval(1)
            .build(new ResourceLocation("bookofdragons", kind.id()).toString())
      );
   }

   public static void register() {
   }
}
