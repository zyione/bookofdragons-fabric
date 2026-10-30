package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public abstract class AdvancementEventsMixin {
    @Shadow private ServerPlayer player;
    @Shadow public abstract net.minecraft.advancements.AdvancementProgress getOrStartProgress(Advancement advancement);
    @Inject(method = "award", at = @At("RETURN"))
    private void liberanimalium$earned(Advancement advancement, String criterion, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && getOrStartProgress(advancement).isDone())
            CommonEvents.ADVANCEMENT_EARNED.allEach(l -> l.onEarned(player, advancement.getId()));
    }
}
