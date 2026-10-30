package net.magister.liberanimalium.fauna.api;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;

public record HeardSound(SoundKind kind, Vec3 position, @Nullable UUID sourceId, long tick, float distance, @Nullable Kind source, @Nullable UUID about) {
   public HeardSound(SoundKind kind, Vec3 position, @Nullable UUID sourceId, long tick, float distance, @Nullable Kind source) {
      this(kind, position, sourceId, tick, distance, source, null);
   }

   public boolean sameSpecies() {
      return this.source != null && this.source.sameSpecies();
   }

   public boolean announces() {
      if (this.source != null && this.sourceId != null) {
         return this.kind != SoundKind.CALL && this.kind != SoundKind.VOCAL ? this.kind == SoundKind.ROAR : this.source.sameSpecies();
      } else {
         return false;
      }
   }
}
