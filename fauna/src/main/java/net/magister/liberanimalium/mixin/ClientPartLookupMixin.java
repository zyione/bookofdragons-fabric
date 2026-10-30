package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.body.PartIndexed;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientPartLookupMixin {
    @Inject(method = "getEntity(I)Lnet/minecraft/world/entity/Entity;", at = @At("RETURN"), cancellable = true)
    private void liberanimalium$part(int id, CallbackInfoReturnable<Entity> cir) {
        if (cir.getReturnValue() == null) cir.setReturnValue(((PartIndexed)this).liberanimalium$partIndex().byId(id));
    }
}
