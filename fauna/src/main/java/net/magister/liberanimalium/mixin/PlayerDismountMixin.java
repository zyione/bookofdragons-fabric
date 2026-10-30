package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDismountMixin {
   @Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
   private void liberanimalium$shiftIsNotADismountOnAMount(CallbackInfoReturnable<Boolean> cir) {
      Player self = (Player)(Object)this;
      if (self.getVehicle() instanceof FaunaBase) {
         cir.setReturnValue(false);
      }
   }
}
