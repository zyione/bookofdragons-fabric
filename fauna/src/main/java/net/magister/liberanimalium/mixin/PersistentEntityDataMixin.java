package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.platform.fabric.EntityData;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PersistentEntityDataMixin implements EntityData {
    @Unique private CompoundTag liberanimalium$data = new CompoundTag();
    @Unique private boolean liberanimalium$loadedFromDisk;
    @Override public CompoundTag liberanimalium$data() { return liberanimalium$data; }
    @Override public boolean liberanimalium$loadedFromDisk() { return liberanimalium$loadedFromDisk; }
    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void liberanimalium$save(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (!liberanimalium$data.isEmpty()) tag.put("ForgeData", liberanimalium$data.copy());
    }
    @Inject(method = "load", at = @At("HEAD"))
    private void liberanimalium$load(CompoundTag tag, CallbackInfo ci) {
        liberanimalium$loadedFromDisk = true;
        liberanimalium$data = tag.getCompound("ForgeData").copy();
        CommonEvents.LEGACY_DATA.allEach(l -> l.onLoad((Entity)(Object)this, tag, liberanimalium$data));
    }
}
