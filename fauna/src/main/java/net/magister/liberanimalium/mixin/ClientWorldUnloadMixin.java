package net.magister.liberanimalium.mixin;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientWorldUnloadMixin {
    @Inject(method = "disconnect", at = @At("HEAD"))
    private void liberanimalium$unload(CallbackInfo ci) {
        CommonEvents.LEVEL_UNLOAD.allEach(l -> l.accept((ClientLevel)(Object)this));
    }
}
