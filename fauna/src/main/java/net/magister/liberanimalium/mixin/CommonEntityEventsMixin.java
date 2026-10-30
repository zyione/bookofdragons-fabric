package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class CommonEntityEventsMixin {
    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at = @At("RETURN"))
    private void liberanimalium$mounted(Entity vehicle, boolean force, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        if (cir.getReturnValue()) CommonEvents.MOUNT.allEach(l -> l.onMount(self, vehicle, self.level(), true));
    }
    @Inject(method = "removeVehicle", at = @At("HEAD"))
    private void liberanimalium$dismounted(CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        Entity vehicle = self.getVehicle();
        if (vehicle != null) CommonEvents.MOUNT.allEach(l -> l.onMount(self, vehicle, self.level(), false));
    }
}
