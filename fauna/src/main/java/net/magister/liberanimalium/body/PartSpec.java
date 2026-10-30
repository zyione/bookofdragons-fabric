package net.magister.liberanimalium.body;

import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;

public record PartSpec(
   String name, String bone, @Nullable String toward, float width, float height, float damage, boolean head, float stretch, Vec3 rest, boolean walkable
) {
   public boolean elongated() {
      return this.toward != null && !this.toward.isEmpty();
   }
}
