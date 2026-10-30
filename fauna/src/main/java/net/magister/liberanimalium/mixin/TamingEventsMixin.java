package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TamableAnimal.class)
public abstract class TamingEventsMixin {
    @Inject(method = "tame", at = @At("TAIL"))
    private void liberanimalium$tamed(Player player, CallbackInfo ci) {
        CommonEvents.ANIMAL_TAMED.allEach(l -> l.onTamed((TamableAnimal)(Object)this, player));
    }
}
