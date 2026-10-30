package net.magister.bookofdragons.entity.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;
import net.magister.bookofdragons.entity.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

public enum DragonType {
   NIGHTFURY(
      () -> (EntityType<? extends Mob>)ModEntities.NIGHTFURY.get(),
      EggSize.MEDIUM,
      26000,
      ParticleTypes.DRAGON_BREATH,
      new EggHatchCondition[]{EggHatchCondition.DARKNESS},
      new int[]{1}
   ),
   DEADLY_NADDER(
      () -> (EntityType<? extends Mob>)ModEntities.DEADLYNADDER.get(),
      EggSize.MEDIUM,
      26000,
      ParticleTypes.FLAME,
      new EggHatchCondition[]{EggHatchCondition.LIGHT_LEVEL},
      new int[]{10}
   ),
   MONSTROUS_NIGHTMARE(
      () -> (EntityType<? extends Mob>)ModEntities.MONSTROUSNIGHTMARE.get(),
      EggSize.LARGE,
      26000,
      ParticleTypes.FLAME,
      new EggHatchCondition[]{EggHatchCondition.ON_FIRE},
      new int[]{0}
   ),
   GRONCKLE(() -> (EntityType<? extends Mob>)ModEntities.GRONCKLE.get(), EggSize.SMALL, 26000, ParticleTypes.CAMPFIRE_COSY_SMOKE, null, null),
   HIDEOUS_ZIPPLEBACK(
      () -> (EntityType<? extends Mob>)ModEntities.HIDDEOUS_ZIPPLEBACK.get(),
      EggSize.LARGE,
      26000,
      ParticleTypes.SMOKE,
      new EggHatchCondition[]{EggHatchCondition.IN_WATER, EggHatchCondition.DARKNESS},
      new int[]{0, 3}
   ),
   SKRILL(
      () -> (EntityType<? extends Mob>)ModEntities.SKRILL.get(),
      EggSize.MEDIUM,
      26000,
      ParticleTypes.ELECTRIC_SPARK,
      new EggHatchCondition[]{EggHatchCondition.ON_FIRE},
      new int[]{0}
   ),
   WHISPERING_DEATH(() -> (EntityType<? extends Mob>)ModEntities.WHISPERING_DEATH.get(), EggSize.LARGE, 26000, ParticleTypes.CAMPFIRE_COSY_SMOKE, null, null),
   TERRIBLE_TERROR(() -> (EntityType<? extends Mob>)ModEntities.TERRIBLE_TERROR.get(), EggSize.SMALL, 20000, ParticleTypes.FLAME, null, null),
   HOBGOBBLER(() -> registered("hobgobbler"), EggSize.SMALL, 16000, ParticleTypes.FLAME, null, null),
   SPEED_STINGER(() -> (EntityType<? extends Mob>)ModEntities.SPEED_STINGER.get(), EggSize.SMALL, 22000, ParticleTypes.ELECTRIC_SPARK, null, null);

   private final DragonType.EntityTypeSupplier entityTypeSupplier;
   private final EggSize eggSize;
   private final int baseHatchTime;
   private final ParticleOptions hatchParticle;
   private final EggHatchCondition[] hatchConditions;
   private final int[] conditionParameters;
   private final String serializedName = this.name().toLowerCase(Locale.ROOT);

   DragonType(
      DragonType.EntityTypeSupplier entityTypeSupplier,
      EggSize eggSize,
      int baseHatchTime,
      ParticleOptions hatchParticle,
      EggHatchCondition[] hatchConditions,
      int[] conditionParameters
   ) {
      this.entityTypeSupplier = entityTypeSupplier;
      this.eggSize = eggSize;
      this.baseHatchTime = baseHatchTime;
      this.hatchParticle = hatchParticle;
      this.hatchConditions = hatchConditions;
      this.conditionParameters = conditionParameters;
   }

   public EntityType<? extends Mob> getEntityType() {
      return this.entityTypeSupplier.get();
   }

   public boolean inGame() {
      return this.entityTypeSupplier.get() != null;
   }

   public static List<DragonType> present() {
      List<DragonType> out = new ArrayList<>();

      for (DragonType type : values()) {
         if (type.inGame()) {
            out.add(type);
         }
      }

      return out;
   }

   @Nullable
   private static EntityType<? extends Mob> registered(String path) {
      return (EntityType<? extends Mob>)BuiltInRegistries.ENTITY_TYPE.getOptional(new ResourceLocation("bookofdragons", path)).orElse(null);
   }

   public EggSize getEggSize() {
      return this.eggSize;
   }

   public int getBaseHatchTime() {
      return this.baseHatchTime;
   }

   public ParticleOptions getHatchParticle() {
      return this.hatchParticle;
   }

   @Nullable
   public EggHatchCondition[] getHatchConditions() {
      return this.hatchConditions;
   }

   @Nullable
   public int[] getConditionParameters() {
      return this.conditionParameters;
   }

   public boolean requiresHatchConditions() {
      return this.hatchConditions != null && this.hatchConditions.length > 0;
   }

   public Component getConditionsDescription() {
      if (!this.requiresHatchConditions()) {
         return Component.translatable("entity.bookofdragons.dragon_egg.condition.none");
      }

      MutableComponent desc = Component.empty();

      for (int i = 0; i < this.hatchConditions.length; i++) {
         if (i > 0) {
            desc.append(Component.translatable("entity.bookofdragons.dragon_egg.condition.and"));
         }

         desc.append(this.hatchConditions[i].getDescription(this.conditionParameters[i]));
      }

      return desc;
   }

   public String getSerializedName() {
      return this.serializedName;
   }

   public ResourceLocation getEggTexture() {
      return new ResourceLocation("bookofdragons", "textures/entity/egg/" + this.getSerializedName() + "_egg.png");
   }

   public ResourceLocation getEggModel() {
      return new ResourceLocation("bookofdragons", "geo/eggs/generic_dragon_egg.geo.json");
   }

   public ResourceLocation getEggAnimations() {
      return new ResourceLocation("bookofdragons", "animations/eggs/generic_dragon_egg.animation.json");
   }

   public static DragonType fromString(String name) {
      for (DragonType type : values()) {
         if (type.getSerializedName().equalsIgnoreCase(name)) {
            return type;
         }
      }

      return NIGHTFURY;
   }

   public float getRenderScale() {
      return switch (this.eggSize) {
         case SMALL -> 0.5F;
         case MEDIUM -> 1.0F;
         case LARGE -> 1.35F;
      };
   }

   public float getCollisionScale() {
      return switch (this.eggSize) {
         case SMALL -> 0.6F;
         case MEDIUM -> 0.8F;
         case LARGE -> 1.0F;
      };
   }

   @FunctionalInterface
   public interface EntityTypeSupplier {
      EntityType<? extends Mob> get();
   }
}
