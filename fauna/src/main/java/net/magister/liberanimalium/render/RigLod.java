package net.magister.liberanimalium.render;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.config.RideSettings;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class RigLod {
   private static volatile RigLod.Rung pinned;
   private static final int MEMO_FRAME = 0;
   private static final int MEMO_RUNG = 1;
   private static final Map<Integer, long[]> MEMO = new ConcurrentHashMap<>();
   private static final int[] COUNTS = new int[RigLod.Rung.values().length];

   private RigLod() {
   }

   public static void pin(RigLod.Rung rung) {
      pinned = rung;
   }

   public static RigLod.Rung pinned() {
      return pinned;
   }

   private static long[] memo(Entity entity) {
      return MEMO.computeIfAbsent(entity.getId(), id -> new long[]{Long.MIN_VALUE, 0L});
   }

   public static RigLod.Rung of(FaunaBase body) {
      return of((Entity)body);
   }

   public static RigLod.Rung of(Entity entity) {
      RigLod.Rung pin = pinned;
      if (pin != null) {
         return pin;
      }

      if (!RideSettings.isProceduralRigEnabled()) {
         return RigLod.Rung.FAR;
      }

      int frame = PerfClock.frameId();
      long[] memo = memo(entity);
      if (memo[0] == frame) {
         return RigLod.Rung.values()[(int)memo[1]];
      }

      RigLod.Rung was = memo[0] == Long.MIN_VALUE ? null : RigLod.Rung.values()[(int)memo[1]];
      RigLod.Rung rung = compute(entity, was);
      memo[0] = frame;
      memo[1] = rung.ordinal();
      COUNTS[rung.ordinal()]++;
      return rung;
   }

   private static RigLod.Rung compute(Entity entity, RigLod.Rung was) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && entity.hasPassenger(mc.player)) {
         return RigLod.Rung.FULL;
      }

      Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
      double d2 = entity.getBoundingBox().getCenter().distanceToSqr(camera);
      return rungFor(Math.sqrt(d2), was);
   }

   public static RigLod.Rung rungFor(double blocks, RigLod.Rung was) {
      if (was == null) {
         return rungFor(blocks);
      }

      RigLod.Rung farther = rungFor(blocks - 4.0);
      if (farther.ordinal() > was.ordinal()) {
         return farther;
      }

      RigLod.Rung nearer = rungFor(blocks + 4.0);
      return nearer.ordinal() < was.ordinal() ? nearer : was;
   }

   public static RigLod.Rung rungFor(double blocks) {
      if (blocks < 32.0) {
         return RigLod.Rung.FULL;
      } else if (blocks < 64.0) {
         return RigLod.Rung.HALF;
      } else {
         return blocks < 128.0 ? RigLod.Rung.QUARTER : RigLod.Rung.FAR;
      }
   }

   public static boolean due(int counter, int every, int id) {
      return every <= 1 ? true : Math.floorMod(counter, every) == Math.floorMod(id, every);
   }

   public static int[] countsAndReset() {
      int[] out = (int[])COUNTS.clone();
      Arrays.fill(COUNTS, 0);
      return out;
   }

   public static void forget(int entityId) {
      MEMO.remove(entityId);
   }

   public enum Rung {
      FULL,
      HALF,
      QUARTER,
      FAR;

      public int chainCadence() {
         return switch (this) {
            case FULL -> 1;
            case HALF -> 2;
            case QUARTER -> 4;
            case FAR -> 0;
         };
      }

      public boolean chainActive() {
         return this != FAR;
      }

      public boolean legsActive() {
         return this == FULL || this == HALF;
      }
   }
}
