package net.magister.liberanimalium.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Preserve Forge's entity-independent ray queries used by water wake effects. */
@Mixin(ClipContext.class)
public abstract class ClipContextMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/shapes/CollisionContext;of(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/shapes/CollisionContext;"))
    private CollisionContext liberanimalium$nullableEntity(Entity entity) {
        return entity == null ? CollisionContext.empty() : CollisionContext.of(entity);
    }
}
