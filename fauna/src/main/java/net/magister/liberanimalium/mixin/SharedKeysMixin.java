package net.magister.liberanimalium.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.magister.liberanimalium.platform.fabric.FabricClientServices;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla's single key-to-binding map otherwise drops shared mouse/QTE bindings. */
@Mixin(KeyMapping.class)
public abstract class SharedKeysMixin {
    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private static void liberanimalium$set(InputConstants.Key input, boolean down, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) return;
        for (KeyMapping key : client.options.keyMappings) {
            if (((KeyMappingAccessor)key).liberanimalium$key().equals(input))
                key.setDown(down && FabricClientServices.active(key));
        }
        ci.cancel();
    }
    @Inject(method = "click", at = @At("HEAD"), cancellable = true)
    private static void liberanimalium$click(InputConstants.Key input, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) return;
        for (KeyMapping key : client.options.keyMappings) {
            KeyMappingAccessor access = (KeyMappingAccessor)key;
            if (access.liberanimalium$key().equals(input) && FabricClientServices.active(key))
                access.liberanimalium$clicks(access.liberanimalium$clicks() + 1);
        }
        ci.cancel();
    }
}
