package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class CommonLivingEventsMixin {
    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void liberanimalium$hurt(DamageSource source, float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        CommonEvents.LIVING_DAMAGED.allEach(l -> l.onDamaged(self, source, amount));
    }
}
