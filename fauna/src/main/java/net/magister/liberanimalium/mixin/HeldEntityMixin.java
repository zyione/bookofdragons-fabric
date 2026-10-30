package net.magister.liberanimalium.mixin;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.grip.Held;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class HeldEntityMixin implements Held {
   @Unique
   @Nullable
   private FaunaBase liberanimalium$heldBy;
   @Unique
   private final Held.Pin liberanimalium$pinned = new Held.Pin();

   @Nullable
   @Override
   public FaunaBase liberanimalium$holder() {
      return this.liberanimalium$heldBy;
   }

   @Override
   public void liberanimalium$setHolder(@Nullable FaunaBase holder) {
      this.liberanimalium$heldBy = holder;
   }

   @Override
   public Held.Pin liberanimalium$pin() {
      return this.liberanimalium$pinned;
   }

   @Inject(method = "move", at = @At("HEAD"), cancellable = true)
   private void liberanimalium$heldDoesNotMoveItself(MoverType type, Vec3 movement, CallbackInfo ci) {
      if (type == MoverType.SELF && this.liberanimalium$heldBy != null) {
         ci.cancel();
      }
   }
}
