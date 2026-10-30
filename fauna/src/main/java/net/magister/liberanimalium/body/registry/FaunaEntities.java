package net.magister.liberanimalium.body.registry;

import net.magister.liberanimalium.attack.entity.AttackProjectileEntity;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.magister.liberanimalium.platform.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;

public final class FaunaEntities {
   public static final Registrar<EntityType<?>> ENTITY_TYPES = Services.registries().create(Registries.ENTITY_TYPE, "liberanimalium");
   public static final RegEntry<EntityType<AttackProjectileEntity>> ATTACK_PROJECTILE = ENTITY_TYPES.register(
      "attack_projectile",
      () -> Builder.<AttackProjectileEntity>of(AttackProjectileEntity::new, MobCategory.MISC)
         .sized(0.5F, 0.5F)
         .updateInterval(1)
         .clientTrackingRange(8)
         .build(new ResourceLocation("liberanimalium", "attack_projectile").toString())
   );

   public static void register() {
   }
}
