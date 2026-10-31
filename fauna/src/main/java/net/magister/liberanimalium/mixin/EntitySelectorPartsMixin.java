package net.magister.liberanimalium.mixin;

import java.util.function.Predicate;
import net.magister.liberanimalium.platform.part.PartBody;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Command targets are persistent entities; physical queries still include hitboxes. */
@Mixin(EntitySelector.class)
public abstract class EntitySelectorPartsMixin {
    @Inject(method = "getPredicate", at = @At("RETURN"), cancellable = true)
    private void liberanimalium$selectParents(Vec3 origin, CallbackInfoReturnable<Predicate<Entity>> cir) {
        cir.setReturnValue(cir.getReturnValue().and(entity -> !(entity instanceof PartBody<?>)));
    }
}
