package net.magister.liberanimalium.mixin;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.magister.liberanimalium.chassis.component.GripComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision", remap = false)
public abstract class SableHeldCollisionMixin {
   @Redirect(
      method = "collide",
      require = 0,
      at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectSet;iterator()Lit/unimi/dsi/fastutil/objects/ObjectIterator;")
   )
   private static ObjectIterator<Object> liberanimalium$passHeld(ObjectSet<Object> met) {
      Object held = GripComponent.passingThrough();
      if (held != null) {
         met.remove(held);
      }

      return met.iterator();
   }
}
