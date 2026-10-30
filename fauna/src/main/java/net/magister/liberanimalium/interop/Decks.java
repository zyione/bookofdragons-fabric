package net.magister.liberanimalium.interop;

import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class Decks {
   private static final int HOLD_DEPTH = 3;

   private Decks() {
   }

   @Nullable
   public static Object of(Entity body) {
      if (!body.level().isClientSide() && !body.isPassenger() && body.isControlledByLocalInstance()) {
         Object under = SubLevels.under(body);
         if (under == null && body instanceof Decks.Standing standing) {
            Vec3 place = standing.subLevelPlace();
            if (place != null) {
               return SubLevels.at(body.level(), place);
            } else {
               return body.tickCount <= 1 ? SubLevels.beneath(body) : null;
            }
         } else {
            return under;
         }
      } else {
         return null;
      }
   }

   @Nullable
   public static Vec3 place(Object deck, Vec3 world) {
      return SubLevels.plot(deck, world);
   }

   @Nullable
   public static Vec3 world(Object deck, Vec3 place) {
      return SubLevels.world(deck, place);
   }

   @Nullable
   public static Vec3 now(Level level, Vec3 kept) {
      if (!SubLevels.inPlot(level, kept)) {
         return kept;
      }

      Object deck = SubLevels.at(level, kept);
      return deck == null ? null : SubLevels.world(deck, kept);
   }

   public static boolean holds(Level level, Object deck, Vec3 world) {
      Vec3 place = place(deck, world);
      if (place == null) {
         return false;
      }

      MutableBlockPos at = BlockPos.containing(place).mutable();

      for (int down = 0; down <= 3; down++) {
         if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty()) {
            return true;
         }

         at.move(0, -1, 0);
      }

      return false;
   }

   public static Vec3 keep(Entity body, Vec3 world) {
      if (SubLevels.inPlot(body.level(), world)) {
         return world;
      } else {
         Object deck = of(body);
         if (deck != null && holds(body.level(), deck, world)) {
            Vec3 place = place(deck, world);
            return place == null ? world : place;
         } else {
            return world;
         }
      }
   }

   public interface Standing {
      @Nullable
      Vec3 subLevelPlace();
   }
}
