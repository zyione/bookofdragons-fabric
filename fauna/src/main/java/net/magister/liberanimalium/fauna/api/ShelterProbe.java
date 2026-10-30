package net.magister.liberanimalium.fauna.api;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ShelterProbe {
   public static final float MAX_SCORE = 4.0F;
   public static final int REACH = 10;
   public static final double FAN_NARROW_LEAN = 0.45;
   public static final double FAN_WIDE_LEAN = 2.5;
   public static final int FAN_RAYS = 9;
   public static final int RING_SAMPLES = 8;
   public static final double RING_BODIES = 2.0;
   public static final double RAY_STEP = 0.5;
   private static final int MAX_LIGHT = 15;
   private static final Vec3[] FOOTPRINT = new Vec3[]{new Vec3(1.0, 0.0, 0.0), new Vec3(-1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, 0.0, -1.0)};
   private static final Vec3[] FAN = buildFan();

   private ShelterProbe() {
   }

   public static boolean roofed(Terrain terrain, BlockPos at) {
      for (int up = 1; up <= 10; up++) {
         if (terrain.solid(at.above(up))) {
            return true;
         }
      }

      return false;
   }

   public static ShelterProbe.Reading score(Terrain terrain, ShelterProbe.Body body, Vec3 at) {
      float half = (float)Math.max(0.5, body.width() * 0.5);
      List<Vec3> samples = new ArrayList<>(1 + FOOTPRINT.length);
      samples.add(at);

      for (Vec3 offset : FOOTPRINT) {
         samples.add(at.add(offset.scale(half)));
      }

      float roof = 1.0F;
      float enclosure = 1.0F;
      float darkness = 1.0F;
      float dryness = 1.0F;
      float danger = 0.0F;

      for (Vec3 sample : samples) {
         BlockPos block = BlockPos.containing(sample);
         roof = Math.min(roof, fan(terrain, sample.add(0.0, body.eyeHeight(), 0.0)));
         enclosure = Math.min(enclosure, enclosure(terrain, body, sample));
         darkness = Math.min(darkness, 1.0F - terrain.skyLight(block) / 15.0F);
         dryness = Math.min(dryness, terrain.fluid(block) ? 0.0F : 1.0F);
         danger = Math.max(danger, Mth.clamp(terrain.danger(sample), 0.0F, 1.0F));
      }

      return new ShelterProbe.Reading(roof, enclosure, darkness, dryness, danger);
   }

   private static float fan(Terrain terrain, Vec3 from) {
      int blocked = 0;

      for (Vec3 direction : FAN) {
         if (obstructed(terrain, from, direction)) {
            blocked++;
         }
      }

      return (float)blocked / FAN.length;
   }

   private static boolean obstructed(Terrain terrain, Vec3 from, Vec3 direction) {
      for (double travelled = 0.5; travelled <= 10.0; travelled += 0.5) {
         if (terrain.solid(BlockPos.containing(from.add(direction.scale(travelled))))) {
            return true;
         }
      }

      return false;
   }

   private static float enclosure(Terrain terrain, ShelterProbe.Body body, Vec3 at) {
      double reach = Math.max(1.0, body.width() * 2.0);
      Vec3 head = at.add(0.0, body.eyeHeight(), 0.0);
      int walled = 0;

      for (int i = 0; i < 8; i++) {
         double angle = i * 2.0 * Math.PI / 8.0;
         Vec3 direction = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
         if (obstructedWithin(terrain, head, direction, reach)) {
            walled++;
         }
      }

      return walled / 8.0F;
   }

   private static boolean obstructedWithin(Terrain terrain, Vec3 from, Vec3 direction, double reach) {
      for (double travelled = 0.5; travelled <= reach; travelled += 0.5) {
         if (terrain.solid(BlockPos.containing(from.add(direction.scale(travelled))))) {
            return true;
         }
      }

      return false;
   }

   private static Vec3[] buildFan() {
      Vec3[] fan = new Vec3[9];
      fan[0] = new Vec3(0.0, 1.0, 0.0);
      double[] leans = new double[]{0.45, 2.5};
      int i = 1;

      for (double lean : leans) {
         for (int quadrant = 0; quadrant < 4; quadrant++) {
            double angle = quadrant * Math.PI / 2.0;
            fan[i++] = new Vec3(Math.cos(angle) * lean, 1.0, Math.sin(angle) * lean).normalize();
         }
      }

      return fan;
   }

   public record Body(double width, double eyeHeight) {
   }

   public record Reading(float roof, float enclosure, float darkness, float dryness, float danger) {
      public static final ShelterProbe.Reading NONE = new ShelterProbe.Reading(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

      public float total() {
         return Math.max(0.0F, this.roof + this.enclosure + this.darkness + this.dryness - this.danger);
      }
   }
}
