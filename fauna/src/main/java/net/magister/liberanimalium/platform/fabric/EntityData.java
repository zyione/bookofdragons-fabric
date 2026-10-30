package net.magister.liberanimalium.platform.fabric;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public interface EntityData {
    CompoundTag liberanimalium$data();
    boolean liberanimalium$loadedFromDisk();
    static CompoundTag get(Entity entity) { return ((EntityData)entity).liberanimalium$data(); }
}
