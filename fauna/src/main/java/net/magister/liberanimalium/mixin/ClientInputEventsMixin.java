package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.platform.event.ClientEvents;
import net.magister.liberanimalium.qte.client.ClientQteState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class ClientInputEventsMixin {
    @Inject(method = "runTick", at = @At("HEAD"))
    private void liberanimalium$frameStart(boolean render, CallbackInfo ci) {
        if (render) ClientEvents.FRAME_START.allEach(Runnable::run);
    }
    @Inject(method = "runTick", at = @At("RETURN"))
    private void liberanimalium$frameEnd(boolean render, CallbackInfo ci) {
        if (render) ClientEvents.FRAME_END.allEach(Runnable::run);
    }
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void liberanimalium$screen(Screen screen, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.getVehicle() != null && ClientQteState.tameCircleActiveFor(player.getVehicle().getId()) && !(screen instanceof PauseScreen)) {
            ci.cancel();
        } else ClientEvents.SCREEN_OPENING.allEach(l -> l.accept(screen));
    }
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void liberanimalium$use(CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().screen != null) return;
        for (InteractionHand hand : InteractionHand.values()) {
            for (ClientEvents.UseKey listener : ClientEvents.USE_KEY.all()) {
                ClientEvents.UseKeyResult result = listener.onUseKey(hand);
                if (result != ClientEvents.UseKeyResult.PASS) {
                    if (result == ClientEvents.UseKeyResult.CANCEL) player.swing(hand);
                    ci.cancel();
                    return;
                }
            }
        }
    }
}
