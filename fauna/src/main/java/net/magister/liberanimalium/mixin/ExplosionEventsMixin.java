package net.magister.liberanimalium.mixin;
import java.util.List;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Explosion.class)
public abstract class ExplosionEventsMixin {
    @Shadow @Final private ObjectArrayList<BlockPos> toBlow;
    @Redirect(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<Entity> liberanimalium$detonate(Level level, Entity except, AABB bounds) {
        List<Entity> entities = level.getEntities(except, bounds);
        CommonEvents.EXPLOSION.allEach(l -> l.onDetonate(level, (Explosion)(Object)this, toBlow, entities));
        return entities;
    }
}
