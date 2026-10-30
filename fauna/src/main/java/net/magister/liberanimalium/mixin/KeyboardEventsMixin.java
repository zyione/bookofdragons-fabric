package net.magister.liberanimalium.mixin;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardEventsMixin {
    @Inject(method = "keyPress", at = @At("TAIL"))
    private void liberanimalium$key(long window, int key, int scan, int action, int modifiers, CallbackInfo ci) {
        if (window == net.minecraft.client.Minecraft.getInstance().getWindow().getWindow())
            ClientEvents.KEY.allEach(l -> l.onKey(key, scan, action, modifiers));
    }
}
