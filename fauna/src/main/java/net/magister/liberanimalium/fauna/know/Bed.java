package net.magister.liberanimalium.fauna.know;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public record Bed(BlockPos at, float score, Bed.Source source, long seen) {
   private static final String KEY = "Bed";
   public static final float MAX_SCORE = 4.0F;

   public boolean isRoost() {
      return this.source == Bed.Source.ROOST;
   }

   public Vec3 centre() {
      return Vec3.atBottomCenterOf(this.at);
   }

   public void save(CompoundTag tag) {
      CompoundTag out = new CompoundTag();
      out.putInt("X", this.at.getX());
      out.putInt("Y", this.at.getY());
      out.putInt("Z", this.at.getZ());
      out.putFloat("Score", this.score);
      out.putString("Source", this.source.name());
      out.putLong("Seen", this.seen);
      tag.put("Bed", out);
   }

   @Nullable
   public static Bed load(CompoundTag tag) {
      if (!tag.contains("Bed")) {
         return null;
      }

      CompoundTag in = tag.getCompound("Bed");

      Bed.Source source;
      try {
         source = Bed.Source.valueOf(in.getString("Source"));
      } catch (IllegalArgumentException e) {
         return null;
      }

      return new Bed(new BlockPos(in.getInt("X"), in.getInt("Y"), in.getInt("Z")), in.getFloat("Score"), source, in.getLong("Seen"));
   }

   public enum Source {
      ROOST,
      FOUND;
   }
}
