package net.magister.liberanimalium.platform.registry;

import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;

public final class Regs {
   private Regs() {
   }

   public static ResourceLocation itemKey(Item item) {
      return BuiltInRegistries.ITEM.getKey(item);
   }

   public static Item item(@Nullable ResourceLocation id) {
      return (Item)BuiltInRegistries.ITEM.get(id);
   }

   public static ResourceLocation entityTypeKey(EntityType<?> type) {
      return BuiltInRegistries.ENTITY_TYPE.getKey(type);
   }

   public static EntityType<?> entityType(@Nullable ResourceLocation id) {
      return (EntityType<?>)BuiltInRegistries.ENTITY_TYPE.get(id);
   }

   public static boolean hasEntityType(ResourceLocation id) {
      return BuiltInRegistries.ENTITY_TYPE.containsKey(id);
   }

   public static Set<ResourceLocation> entityTypeKeys() {
      return BuiltInRegistries.ENTITY_TYPE.keySet();
   }

   public static Optional<Reference<EntityType<?>>> entityTypeHolder(EntityType<?> type) {
      return BuiltInRegistries.ENTITY_TYPE.getResourceKey(type).flatMap(BuiltInRegistries.ENTITY_TYPE::getHolder);
   }

   @Nullable
   public static SoundEvent sound(@Nullable ResourceLocation id) {
      return (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(id);
   }

   @Nullable
   public static ParticleType<?> particleType(@Nullable ResourceLocation id) {
      return (ParticleType<?>)BuiltInRegistries.PARTICLE_TYPE.get(id);
   }

   public static Iterable<Reference<Attribute>> attributes() {
      return () -> BuiltInRegistries.ATTRIBUTE.holders().iterator();
   }
}
