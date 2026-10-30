package net.magister.liberanimalium.fauna.api;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;

public record SensedItem(UUID id, Vec3 position, boolean edible, float craving, @Nullable UUID holder, @Nullable UUID giver) {
   public SensedItem(UUID id, Vec3 position, boolean edible, float craving) {
      this(id, position, edible, craving, null, null);
   }

   public boolean offered() {
      return this.holder != null;
   }
}
