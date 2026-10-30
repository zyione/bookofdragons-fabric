package net.magister.liberanimalium.fauna.move;

import java.util.Map;
import net.magister.liberanimalium.fauna.api.MoveMode;

public final class MoveTuning {
   public static final float AIR_GROUND_PENALTY = 1.25F;
   private static final float HAZARD_AIR_MARGIN = 2.0F;
   public static final float TAKEOFF_COST = 8.0F;
   public static final float LAND_COST = 8.0F;
   public static final float LAND_DROP_STEEPNESS = (float)(2.0 * Math.sqrt(2.0));
   public static final int SKY_CLEARANCE_STEPS = 4;
   public static final int CRUISE_CEILING_MULTIPLE = 2;
   public static final float WATER_ENTER_COST = 4.0F;
   public static final float WATER_EXIT_COST = 4.0F;
   public static final float SPLASH_DOWN_COST = 6.0F;
   public static final float CLIMB_ON_COST = 5.0F;
   public static final float CLIMB_OFF_COST = 5.0F;
   public static final int NODE_CAP = 5000;
   public static final int SNAPSHOT_PAD = 24;
   public static final float MIN_RANGE = 64.0F;
   private static final float DETOUR_FACTOR = 2.5F;
   public static final float UNSUPPORTED_CENTRE_MALUS = 2.5F;
   public static final double MIN_SEGMENT = 5.0;
   public static final int MAX_SEGMENTS = 6;
   public static final double ARRIVAL_MARGIN = 0.5;
   public static final int AIR_LATTICE_MIN_SPACING = 2;
   private static final double AERIAL_ARRIVAL_FRACTION = 0.6;
   public static final double SURFACE_ARRIVAL_MARGIN = 2.0;
   public static final double STEP_DOWN_TOLERANCE = 2.0;
   private static final double LOOKAHEAD_ARRIVALS = 2.0;
   public static final double SHARP_CORNER_DOT = 0.35;
   public static final double STRAIGHT_SEGMENT_DOT = 0.85;
   public static final double LOS_RAY_ELEVATION = 0.5;
   public static final int SOLVE_BUDGET_TICKS = 60;
   public static final int STUCK_WINDOW_TICKS = 80;
   public static final double STUCK_MIN_DISPLACEMENT = 2.5;
   public static final int STUCK_REPLANS = 2;
   public static final int DOOR_LOOKAHEAD_NODES = 4;
   public static final double DOOR_THREAD_BLOCKS = 3.0;
   public static final double DOOR_THREAD_LATERAL = 1.0;
   public static final double DOOR_LINE_TOLERANCE = 0.05;
   public static final double HOP_REACH = 1.0;
   public static final double INVALIDATE_RANGE = 8.0;
   public static final int GROUND_BREAK_BUDGET_TICKS = 40;
   public static final int LANDING_ATTEMPTS = 3;

   private MoveTuning() {
   }

   public static float minHazardMalus() {
      return 0.5F;
   }

   public static float hazardMalus(float authored) {
      return authored < 0.0F ? authored : Math.max(authored, minHazardMalus());
   }

   public static void orderModeCosts(Map<MoveMode, Float> costs) {
      Float ground = costs.get(MoveMode.GROUND);
      Float air = costs.get(MoveMode.AIR);
      if (ground != null && air != null) {
         if (Float.isFinite(ground) && Float.isFinite(air)) {
            costs.put(MoveMode.AIR, Math.max(air, ground * 1.25F));
         }
      }
   }

   public static float lowAirFactor(float groundFactor) {
      return groundFactor * 1.25F;
   }

   public static int skyClearance(double bodyWidth) {
      return airLatticeStep(bodyWidth) * 4;
   }

   public static int cruiseCeiling(double bodyWidth) {
      return skyClearance(bodyWidth) * 2;
   }

   public static float detourAllowance(float crowFlightRange) {
      return crowFlightRange * 2.5F;
   }

   public static int airLatticeStep(double bodyWidth) {
      return Math.max(2, (int)Math.ceil(bodyWidth));
   }

   public static double aerialArrivalReach(double bodyWidth) {
      return airLatticeStep(bodyWidth) * 0.6;
   }

   public static double lookahead(double bodyWidth) {
      return (bodyWidth * 0.5 + 0.5) * 2.0;
   }

   public static double cornerClearance(double bodyWidth) {
      return bodyWidth * 0.5;
   }

   public static double doorAlignLookahead(double bodyWidth) {
      return bodyWidth * 0.5 + 0.2;
   }

   public static double airCrossingDistance(float airCost, float bias) {
      double denominator = 1.0 - airCost * bias;
      return denominator <= 0.0 ? Double.MAX_VALUE : 16.0 / denominator;
   }

   public static float transitionTravel(ModeTransition transition, float distance, float airFactor, float intoFactor) {
      return transitionTravel(transition, distance, airFactor, intoFactor, 0.0F, 0);
   }

   public static float transitionTravel(ModeTransition transition, float distance, float airFactor, float intoFactor, float lowAirFactor, int band) {
      return switch (transition) {
         case TAKEOFF -> distance * Math.max(intoFactor, lowAirFactor);
         case LAND -> Math.min(distance, band) * Math.max(airFactor, lowAirFactor) + Math.max(0.0F, distance - band) * airFactor * LAND_DROP_STEEPNESS;
         default -> distance * intoFactor;
      };
   }

   public static float transitionCost(ModeTransition transition) {
      return switch (transition) {
         case TAKEOFF -> 8.0F;
         case LAND -> 8.0F;
         case WATER_ENTER -> 4.0F;
         case WATER_EXIT -> 4.0F;
         case SPLASH_DOWN -> 6.0F;
         case CLIMB_ON -> 5.0F;
         case CLIMB_OFF -> 5.0F;
      };
   }

   public static int accuracy(MoveMode mode, int arrivalCells) {
      int body = Math.max(0, arrivalCells);
      return mode == MoveMode.GROUND ? body : body + 1;
   }
}
