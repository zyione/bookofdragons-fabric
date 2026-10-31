package net.magister.liberanimalium.mixin;
import java.util.List;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Explosion.class)
public abstract class ExplosionEventsMixin {
    @Shadow @Final private ObjectArrayList<BlockPos> toBlow;
    @Shadow @Final private Level level;
    // Transform the collected list after other mods' query redirects have run.
    // Dragons of Berk also redirects the query; replacing it twice prevents boot.
    @ModifyVariable(method = "explode", at = @At("STORE"), ordinal = 0)
    private List<Entity> liberanimalium$detonate(List<Entity> entities) {
        CommonEvents.EXPLOSION.allEach(l -> l.onDetonate(level, (Explosion)(Object)this, toBlow, entities));
        return entities;
    }
}
