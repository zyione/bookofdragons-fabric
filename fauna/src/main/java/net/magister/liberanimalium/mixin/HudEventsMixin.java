package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.platform.event.ClientEvents;
import net.magister.liberanimalium.platform.event.HudLayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class HudEventsMixin {
    @Unique private static boolean liberanimalium$hidden(HudLayer layer) {
        for (ClientEvents.HudLayerPre listener : ClientEvents.HUD_LAYER_PRE.all()) if (listener.cancel(layer)) return true;
        return false;
    }
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void liberanimalium$hotbar(float partial, GuiGraphics graphics, CallbackInfo ci) {
        if (liberanimalium$hidden(HudLayer.HOTBAR)) ci.cancel();
    }
    @Inject(method = "renderHotbar", at = @At("TAIL"))
    private void liberanimalium$afterHotbar(float partial, GuiGraphics graphics, CallbackInfo ci) {
        ClientEvents.HUD_AFTER_HOTBAR.allEach(l -> l.draw(graphics, partial));
    }
    @Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
    private void liberanimalium$hearts(GuiGraphics graphics, Player player, int x, int y, int row, int regen, float max, int current, int display, int absorption, boolean blink, CallbackInfo ci) {
        if (liberanimalium$hidden(HudLayer.PLAYER_HEALTH)) ci.cancel();
    }
    @Redirect(method = "renderPlayerHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"))
    private void liberanimalium$armorFood(GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        if (v == 9 && liberanimalium$hidden(HudLayer.ARMOR_LEVEL) || v == 27 && liberanimalium$hidden(HudLayer.FOOD_LEVEL)) return;
        graphics.blit(texture, x, y, u, v, width, height);
    }
    @Inject(method = "renderVehicleHealth", at = @At("HEAD"), cancellable = true)
    private void liberanimalium$mount(GuiGraphics graphics, CallbackInfo ci) {
        if (liberanimalium$hidden(HudLayer.MOUNT_HEALTH)) ci.cancel();
    }
    @Inject(method = "renderJumpMeter", at = @At("HEAD"), cancellable = true)
    private void liberanimalium$jump(PlayerRideableJumping mount, GuiGraphics graphics, int x, CallbackInfo ci) {
        if (liberanimalium$hidden(HudLayer.JUMP_METER)) ci.cancel();
    }
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void liberanimalium$experience(GuiGraphics graphics, int x, CallbackInfo ci) {
        if (liberanimalium$hidden(HudLayer.EXPERIENCE_BAR)) ci.cancel();
    }
}
