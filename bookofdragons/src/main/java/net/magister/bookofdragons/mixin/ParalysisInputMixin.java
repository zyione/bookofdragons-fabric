package net.magister.bookofdragons.mixin;
import net.magister.bookofdragons.client.ParalysisInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class ParalysisInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void bookofdragons$still(boolean slow, float factor, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null) ParalysisInput.still(player, (Input)(Object)this);
    }
}
