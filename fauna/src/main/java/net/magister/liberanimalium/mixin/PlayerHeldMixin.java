package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.body.grip.Grip;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerHeldMixin {
   @Inject(method = "tick", at = @At("TAIL"))
   private void liberanimalium$heldPassesThrough(CallbackInfo ci) {
      Player self = (Player)(Object)this;
      if (Grip.held(self)) {
         self.noPhysics = true;
      }
   }
}
