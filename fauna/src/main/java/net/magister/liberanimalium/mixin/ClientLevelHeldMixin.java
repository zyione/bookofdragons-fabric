package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.body.grip.Grip;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelHeldMixin {
   @Inject(method = "tickNonPassenger", at = @At("TAIL"))
   private void liberanimalium$pinHeld(Entity entity, CallbackInfo ci) {
      Grip.pin(entity);
   }
}
