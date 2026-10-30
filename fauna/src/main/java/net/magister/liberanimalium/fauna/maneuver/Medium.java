package net.magister.liberanimalium.fauna.maneuver;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.minecraft.util.Mth;

public final class Medium {
   private static final Medium.Choice PLAIN = new Medium.Choice(Medium.Verdict.GROUND, Map.of(), "ground");
   private static final Set<Situation> URGENT = EnumSet.of(Situation.FLEE, Situation.FIGHT);
   private static final Set<Situation> GROUNDED = EnumSet.of(Situation.POSTURE, Situation.SPACE, Situation.COURT, Situation.SOCIAL, Situation.HUNT);

   private Medium() {
   }

   public static Medium.Choice elect(
      Situation situation,
      SpeciesProfile.Movement movement,
      Posture.TraitReader traits,
      @Nullable Medium.Quarry quarry,
      boolean canTakeOff,
      boolean afloat,
      double runSpeed,
      @Nullable Medium.Verdict incumbent,
      long heldFor,
      long dwell
   ) {
      return elect(situation, movement, traits, quarry, canTakeOff, afloat, runSpeed, incumbent, heldFor, dwell, false);
   }

   public static Medium.Choice elect(
      Situation situation,
      SpeciesProfile.Movement movement,
      Posture.TraitReader traits,
      @Nullable Medium.Quarry quarry,
      boolean canTakeOff,
      boolean afloat,
      double runSpeed,
      @Nullable Medium.Verdict incumbent,
      long heldFor,
      long dwell,
      boolean settling
   ) {
      return elect(situation, movement, traits, quarry, canTakeOff, afloat, runSpeed, incumbent, heldFor, dwell, settling, 0.0F);
   }

   public static Medium.Choice elect(
      Situation situation,
      SpeciesProfile.Movement movement,
      Posture.TraitReader traits,
      @Nullable Medium.Quarry quarry,
      boolean canTakeOff,
      boolean afloat,
      double runSpeed,
      @Nullable Medium.Verdict incumbent,
      long heldFor,
      long dwell,
      boolean settling,
      float fear
   ) {
      if (quarry != null && quarry.afloat() && situation != Situation.FLEE) {
         Map<MoveMode, Float> bias = swimInBias(movement);
         if (!bias.isEmpty()) {
            return new Medium.Choice(Medium.Verdict.GROUND, bias, "quarry afloat");
         }
      }

      if (afloat && canTakeOff && movement.allows(MoveMode.AIR) && !URGENT.contains(situation)) {
         Map<MoveMode, Float> bias = swimOutBias(movement);
         if (!bias.isEmpty()) {
            return new Medium.Choice(Medium.Verdict.GROUND, bias, "swims out");
         }
      }

      Medium.Choice fresh = decide(situation, movement, traits, quarry, canTakeOff, runSpeed, settling, fear);
      return incumbent != null && incumbent != fresh.verdict() && heldFor < dwell
         ? new Medium.Choice(incumbent, biasFor(incumbent), "held (" + heldFor + "/" + dwell + ")")
         : fresh;
   }

   private static Medium.Choice decide(
      Situation situation,
      SpeciesProfile.Movement movement,
      Posture.TraitReader traits,
      @Nullable Medium.Quarry quarry,
      boolean canTakeOff,
      double runSpeed,
      boolean settling,
      float fear
   ) {
      if (!movement.allows(MoveMode.AIR)) {
         return PLAIN;
      }

      if (!canTakeOff) {
         return new Medium.Choice(Medium.Verdict.GROUND, Map.of(), "grounded body");
      }

      if (GROUNDED.contains(situation)) {
         return new Medium.Choice(Medium.Verdict.GROUND, biasFor(Medium.Verdict.GROUND), "grounded act");
      }

      if (quarry != null && situation != Situation.FLEE) {
         if (quarry.gapBelow() > 4.0) {
            return new Medium.Choice(Medium.Verdict.AIR, biasFor(Medium.Verdict.AIR), "quarry flies");
         }

         if (quarry.heightAbove() > 16.0) {
            return new Medium.Choice(Medium.Verdict.AIR, biasFor(Medium.Verdict.AIR), "quarry above");
         }

         if (quarry.distance() > 20.0 && quarry.speed() > runSpeed * 1.3) {
            return new Medium.Choice(Medium.Verdict.AIR, biasFor(Medium.Verdict.AIR), "quarry outruns");
         }
      }

      if (settling) {
         return new Medium.Choice(Medium.Verdict.GROUND, biasFor(Medium.Verdict.GROUND), "settling");
      }

      float willing = willingness(situation, traits, fear, quarry, runSpeed) * affinity(movement);
      return willing >= 0.5F
         ? new Medium.Choice(Medium.Verdict.AIR, biasFor(Medium.Verdict.AIR), "elective " + round(willing))
         : new Medium.Choice(Medium.Verdict.GROUND, Map.of(), "ground " + round(willing));
   }

   public static Map<MoveMode, Float> swimOutBias(SpeciesProfile.Movement movement) {
      Float water = movement.cost().get(MoveMode.WATER);
      Float air = movement.cost().get(MoveMode.AIR);
      if (water != null && air != null && Float.isFinite(water) && Float.isFinite(air) && !(water <= 0.0F)) {
         float toll = 12.0F;
         float swum = air + toll / 24.0F;
         if (swum >= water) {
            return Map.of();
         }

         EnumMap<MoveMode, Float> bias = new EnumMap<>(MoveMode.class);
         bias.put(MoveMode.WATER, swum / water);
         return bias;
      } else {
         return Map.of();
      }
   }

   public static Map<MoveMode, Float> swimInBias(SpeciesProfile.Movement movement) {
      Float water = movement.cost().get(MoveMode.WATER);
      Float ground = movement.cost().get(MoveMode.GROUND);
      if (water != null && ground != null && Float.isFinite(water) && !(water <= 0.0F)) {
         EnumMap<MoveMode, Float> bias = new EnumMap<>(biasFor(Medium.Verdict.GROUND));
         bias.put(MoveMode.WATER, Math.min(1.0F, ground / water));
         return bias;
      } else {
         return Map.of();
      }
   }

   public static float affinity(SpeciesProfile.Movement movement) {
      Float cost = movement.cost().get(MoveMode.AIR);
      return cost != null && Float.isFinite(cost) && !(cost <= 0.0F) ? 1.0F / cost : 0.0F;
   }

   public static float willingness(Posture.TraitReader traits) {
      return (traits.get(Trait.BRAVERY) + traits.get(Trait.ENERGY)) * 0.5F;
   }

   public static float willingness(Situation situation, Posture.TraitReader traits, float fear) {
      return willingness(situation, traits, fear, null, 0.0);
   }

   public static float willingness(Situation situation, Posture.TraitReader traits, float fear, @Nullable Medium.Quarry quarry, double runSpeed) {
      return switch (situation) {
         case FLEE -> {
            float felt = Mth.clamp(fear, 0.0F, 1.0F);
            yield 1.0F - (1.0F - felt) * (1.0F - groundLost(quarry, runSpeed));
         }
         default -> willingness(traits);
      };
   }

   public static float groundLost(@Nullable Medium.Quarry quarry, double runSpeed) {
      if (quarry == null || quarry.gapBelow() > 4.0 || quarry.closing() <= 0.0) {
         return 0.0F;
      } else {
         return runSpeed <= 0.0 ? 1.0F : (float)Mth.clamp(quarry.closing() / runSpeed, 0.0, 1.0);
      }
   }

   public static Map<MoveMode, Float> biasFor(Medium.Verdict verdict) {
      EnumMap<MoveMode, Float> bias = new EnumMap<>(MoveMode.class);
      bias.put(MoveMode.AIR, verdict == Medium.Verdict.AIR ? 0.5F : 2.0F);
      return bias;
   }

   public static boolean grounded(Situation situation) {
      return GROUNDED.contains(situation);
   }

   public static boolean landsToward(Situation situation) {
      return situation != Situation.FLEE;
   }

   private static String round(double v) {
      return String.format(Locale.ROOT, "%.2f", v);
   }

   public record Choice(Medium.Verdict verdict, Map<MoveMode, Float> bias, String why) {
      public boolean biased() {
         return !this.bias.isEmpty();
      }
   }

   public record Quarry(double gapBelow, double heightAbove, double distance, double speed, double closing, boolean afloat) {
      public Quarry(double gapBelow, double heightAbove, double distance, double speed) {
         this(gapBelow, heightAbove, distance, speed, 0.0);
      }

      public Quarry(double gapBelow, double heightAbove, double distance, double speed, double closing) {
         this(gapBelow, heightAbove, distance, speed, closing, false);
      }
   }

   public enum Verdict {
      GROUND,
      AIR;
   }
}
