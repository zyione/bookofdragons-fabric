package net.magister.bookofdragons.mixin;

import net.magister.bookofdragons.effect.Paralysis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ParalysisSpawnMixin {
    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void bookofdragons$projectile(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Paralysis.sentByParalysed(entity)) cir.setReturnValue(false);
    }
}
