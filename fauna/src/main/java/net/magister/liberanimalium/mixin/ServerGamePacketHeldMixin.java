package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.body.grip.Grip;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketHeldMixin {
   @Shadow
   public ServerPlayer player;
   @Shadow
   private int aboveGroundTickCount;

   @Inject(method = "tick", at = @At("HEAD"))
   private void liberanimalium$heldIsNotFloating(CallbackInfo ci) {
      if (this.player != null && Grip.held(this.player)) {
         this.aboveGroundTickCount = 0;
      }
   }
}
