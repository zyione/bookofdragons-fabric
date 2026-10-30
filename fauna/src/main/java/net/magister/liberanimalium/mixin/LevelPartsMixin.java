package net.magister.liberanimalium.mixin;

import java.util.List;
import java.util.function.Predicate;
import net.magister.liberanimalium.body.PartIndex;
import net.magister.liberanimalium.body.PartIndexed;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.magister.liberanimalium.platform.part.PartBody;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelPartsMixin implements PartIndexed {
   @Unique
   private final PartIndex liberanimalium$parts = new PartIndex();

   @Override
   public PartIndex liberanimalium$partIndex() {
      return this.liberanimalium$parts;
   }

   @Inject(
      method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
      at = @At("RETURN")
   )
   private void liberanimalium$partsNear(Entity entity, AABB box, Predicate<? super Entity> predicate, CallbackInfoReturnable<List<Entity>> cir) {
      for (PartBody<?> part : this.liberanimalium$parts.nearby(box)) {
         if (part != entity && part.getParent() != entity && predicate.test(part)) cir.getReturnValue().add(part);
      }
   }

   @Inject(
      method = "getEntities(Lnet/minecraft/world/level/entity/EntityTypeTest;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;Ljava/util/List;I)V",
      at = @At("TAIL")
   )
   private <T extends Entity> void liberanimalium$partsNearTyped(
      EntityTypeTest<Entity, T> test, AABB box, Predicate<? super T> predicate, List<? super T> out, int max, CallbackInfo ci
   ) {
      for (PartBody<?> part : this.liberanimalium$parts.nearby(box)) {
         if (out.size() >= max) break;
         T value = test.tryCast(part);
         if (value != null && predicate.test(value)) out.add(value);
      }
   }
}
