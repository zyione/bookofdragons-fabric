package net.magister.liberanimalium.fauna.steer;

import net.magister.liberanimalium.fauna.identity.TemperTuning;
import net.minecraft.util.Mth;

public final class SteerTuning {
   public static final int SLOTS = 16;
   public static final int SLOTS_FAR = 8;
   public static final double PROBE_BASE = 1.5;
   public static final double PROBE_SPEED_GAIN = 1.0;
   public static final double PROBE_MAX_REACH = 2.5;
   public static final double CLIFF_DROP_STEPS = 3.0;
   public static final double PROBE_SIDE_BODIES = 0.75;
   public static final double PROBE_FRONT_MIN_RATIO = 2.0;
   public static final int PROBE_SAMPLES = 3;
   public static final float DANGER_FALLOFF = 0.35F;
   public static final float DANGER_BLOCKED = 1.0F;
   public static final float DANGER_CLIFF = 0.8F;
   public static final float MAX_INTEREST = 1.0F;
   public static final float TEMPORAL_BLEND = 0.35F;
   public static final float BLUR = 0.25F;
   public static final double SUBSLOT_LEAN = 0.5;
   public static final float TURN_SLOWDOWN_ANGLE = 90.0F;
   public static final float TURN_SPEED_FLOOR = 0.35F;
   public static final float GROUND_TURN_SPEED_FLOOR = 0.5F;
   public static final float GROUND_TURN_STOP_ANGLE = 135.0F;
   public static final float GROUND_TURN_CREEP = 0.05F;
   public static final float GROUND_TURN_DEAD_BAND_DEGREES = 25.0F;
   public static final double DECEL_RUNWAY_SECONDS = 1.0;
   public static final double TICKS_PER_SECOND = 20.0;
   public static final float DECEL_FLOOR = 0.3F;
   public static final double VERTICAL_REACH = 4.0;
   public static final double VERTICAL_DEADZONE = 0.5;
   private static final double VERTICAL_RELIEF_BODIES = 0.5;
   private static final double VERTICAL_RELIEF_MIN = 1.0;
   public static final double VERTICAL_AVOID_GAIN = 1.0;
   public static final double FLIGHT_FLOOR_COMFORT = 2.0;
   public static final double FLIGHT_FLOOR = 8.0;
   public static final double FLIGHT_FLOOR_FADE = 4.0;
   public static final double FLIGHT_FLOOR_GAIN = 1.0;
   public static final float VERTICAL_RELEASE = 0.25F;
   public static final float VERTICAL_FLIP_MARGIN = 0.5F;
   public static final int VERTICAL_RELIEF_STEPS = 4;
   public static final int GOAL_SPREAD_FRACTION = 4;
   public static final float GROUND_BREAK_FORWARD = 0.3F;
   public static final double MAX_FLIGHT_ANGLE_DEGREES = 65.0;
   public static final double MAX_FLIGHT_ANGLE = Math.toRadians(65.0);
   public static final float THREAT_DANGER_RANGE = 12.0F;
   public static final float THREAT_DANGER_WEIGHT = 0.48000002F;
   public static final int THREAT_SKIRT_RING_FRACTION = 8;
   public static final float THREAT_SKIRT_SPREAD = 0.5F;
   public static final int THREAT_SKIRT_MIN = 1;
   public static final int THREAT_SKIRT_MAX_RING_FRACTION = 4;
   public static final double SEPARATION_BODIES = 2.5;
   public static final double ALIGNMENT_MULTIPLE = 2.0;
   public static final double COHESION_MULTIPLE = 2.0;
   public static final float SEPARATION_WEIGHT = 0.9F;
   public static final float ALIGNMENT_WEIGHT = 0.45F;
   public static final float COHESION_WEIGHT = 0.3F;
   public static final int FLOCK_SPREAD_FRACTION = 4;
   public static final double SEPARATION_MIN_SHARE = 0.75;
   public static final double AFT_ATTENTION = 0.35;

   private SteerTuning() {
   }

   public static double verticalRelief(double bodyHeight) {
      return Math.max(1.0, bodyHeight * 0.5);
   }

   public static int threatSkirt(int slots, float nervousness) {
      float middle = slots / 8.0F;
      float scaled = middle * Mth.lerp(Mth.clamp(nervousness, 0.0F, 1.0F), 0.5F, 1.5F);
      int widest = Math.max(1, slots / 4);
      return Mth.clamp(Math.round(scaled), 1, widest);
   }

   public static float pacedByEnergy(float urgency, float energy) {
      if (urgency <= 0.0F) {
         return 0.0F;
      }

      float bias = TemperTuning.paceMultiplier(energy);
      float ceiling = 1.0F;
      float floor = 0.0F;
      float room = bias >= 1.0F ? ceiling - urgency : urgency - floor;
      float moved = urgency + (bias - 1.0F) * room;
      return Mth.clamp(moved, floor, ceiling);
   }
}
