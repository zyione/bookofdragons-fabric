package net.magister.bookofdragons.mixin;

import net.magister.bookofdragons.effect.Paralysis;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ParalysisLivingMixin {
    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void bookofdragons$immune(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!self.level().isClientSide && Paralysis.is(effect) && !Paralysis.mayTake(self)) cir.setReturnValue(false);
    }
    @Inject(method = "onEffectAdded", at = @At("TAIL"))
    private void bookofdragons$take(MobEffectInstance effect, Entity cause, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!self.level().isClientSide && Paralysis.is(effect)) Paralysis.take(self, effect.getDuration());
    }
    @Inject(method = "onEffectRemoved", at = @At("TAIL"))
    private void bookofdragons$release(MobEffectInstance effect, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!self.level().isClientSide && Paralysis.is(effect)) Paralysis.release(self);
    }
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void bookofdragons$jump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (Paralysis.is(self)) Paralysis.stopJump(self);
    }
    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void bookofdragons$knockback(double strength, double x, double z, CallbackInfo ci) {
        if (Paralysis.is((LivingEntity)(Object)this)) ci.cancel();
    }
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void bookofdragons$attack(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (Paralysis.byParalysed(source)) cir.setReturnValue(false);
    }
}
