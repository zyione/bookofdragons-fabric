package net.magister.liberanimalium.fauna.maneuver;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Armament;
import net.magister.liberanimalium.fauna.api.Arms;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.api.Wings;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.maneuver.pattern.AirFlankManeuver;
import net.magister.liberanimalium.fauna.maneuver.pattern.ArcManeuver;
import net.magister.liberanimalium.fauna.maneuver.pattern.BreakManeuver;
import net.magister.liberanimalium.fauna.maneuver.pattern.CloseManeuver;
import net.magister.liberanimalium.fauna.maneuver.pattern.CoverManeuver;
import net.magister.liberanimalium.fauna.maneuver.pattern.CreepManeuver;
import net.magister.liberanimalium.fauna.maneuver.pattern.FlankManeuver;
import net.magister.liberanimalium.fauna.maneuver.pattern.OrbitManeuver;
import net.magister.liberanimalium.fauna.maneuver.pattern.StillManeuver;
import net.magister.liberanimalium.fauna.maneuver.pattern.StoopManeuver;
import net.magister.liberanimalium.fauna.maneuver.pattern.TargetedManeuver;
import net.magister.liberanimalium.fauna.maneuver.pattern.WithdrawManeuver;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class MoveStyles {
   private static final float MAX_POSTURE_DISTANCE = (float)Math.sqrt(4.0);
   public static final float THROUGH_GRIP = 0.5F;

   private MoveStyles() {
   }

   private static Set<MoveStyles.Primitive> legal(Situation situation) {
      return switch (situation) {
         case FIGHT -> EnumSet.of(
            MoveStyles.Primitive.CLOSE,
            MoveStyles.Primitive.ARC,
            MoveStyles.Primitive.FLANK,
            MoveStyles.Primitive.WITHDRAW,
            MoveStyles.Primitive.COVER,
            MoveStyles.Primitive.CREEP
         );
         case FLEE -> EnumSet.of(MoveStyles.Primitive.WITHDRAW, MoveStyles.Primitive.COVER);
         case POSTURE -> EnumSet.of(MoveStyles.Primitive.ARC, MoveStyles.Primitive.WITHDRAW, MoveStyles.Primitive.CLOSE);
         case SPACE -> EnumSet.of(MoveStyles.Primitive.WITHDRAW, MoveStyles.Primitive.ARC);
         case COURT -> EnumSet.of(MoveStyles.Primitive.CLOSE, MoveStyles.Primitive.ARC, MoveStyles.Primitive.FLANK);
         case SOCIAL -> EnumSet.of(
            MoveStyles.Primitive.ARC, MoveStyles.Primitive.HOLD, MoveStyles.Primitive.FLANK, MoveStyles.Primitive.WITHDRAW, MoveStyles.Primitive.CLOSE
         );
         default -> EnumSet.allOf(MoveStyles.Primitive.class);
      };
   }

   public static boolean withinSpring(AnimalHandle handle, Subject subject, double springMargin) {
      return handle.bodyPosition().distanceTo(subject.estimate()) <= springRange(handle, springMargin);
   }

   public static double springRange(AnimalHandle handle, double springMargin) {
      return (Math.max(handle.bounds().getXsize() * 1.5, 1.0) + springMargin) * 1.0;
   }

   public static boolean withinSpring(AnimalHandle handle, Subject subject) {
      return withinSpring(handle, subject, 3.0);
   }

   public static MoveStyles.Choice select(
      AnimalHandle handle,
      Subject subject,
      Situation situation,
      Posture.TraitReader traits,
      Posture posture,
      @Nullable MoveStyles.Primitive incumbent,
      Function<MoveStyles.Primitive, Float> aptitude
   ) {
      double springMargin = ManeuverTuning.springMargin(traits.get(Trait.AGGRESSION));
      return select(situation, traits, posture, incumbent, Ground.airborne(handle), withinSpring(handle, subject, springMargin), aptitude);
   }

   public static MoveStyles.Choice select(
      Situation situation,
      Posture.TraitReader traits,
      Posture posture,
      @Nullable MoveStyles.Primitive incumbent,
      boolean airborne,
      boolean nearSubject,
      Function<MoveStyles.Primitive, Float> aptitude
   ) {
      Set<MoveStyles.Primitive> allowed = legal(situation);
      MoveStyles.Primitive best = null;
      float bestScore = 0.0F;
      MoveStyles.Primitive second = null;
      float secondScore = 0.0F;

      for (MoveStyles.Primitive candidate : MoveStyles.Primitive.values()) {
         if (allowed.contains(candidate)
            && (!airborne || candidate != MoveStyles.Primitive.HOLD && candidate != MoveStyles.Primitive.FREEZE)
            && (!airborne || candidate != MoveStyles.Primitive.COVER && candidate != MoveStyles.Primitive.CREEP)
            && (!nearSubject || candidate != MoveStyles.Primitive.COVER && candidate != MoveStyles.Primitive.CREEP && candidate != MoveStyles.Primitive.FREEZE)
            )
          {
            float able = aptitude.apply(candidate);
            if (!(able <= 0.0F)) {
               float score = able * prior(traits, candidate) * candidate.affinity(posture);
               if (candidate == incumbent) {
                  score *= 1.08F;
               }

               if (score > bestScore) {
                  second = best;
                  secondScore = bestScore;
                  best = candidate;
                  bestScore = score;
               } else if (score > secondScore) {
                  second = candidate;
                  secondScore = score;
               }
            }
         }
      }

      return best == null ? new MoveStyles.Choice(MoveStyles.Primitive.CLOSE, 0.0F, null, 0.0F) : new MoveStyles.Choice(best, bestScore, second, secondScore);
   }

   private static float prior(Posture.TraitReader traits, MoveStyles.Primitive primitive) {
      float bold = traits.get(Trait.BRAVERY);
      float tenacity = traits.get(Trait.AGGRESSION);
      float concern = traits.get(Trait.NERVOUSNESS);

      float bias = switch (primitive) {
         case CLOSE -> 0.7F + 0.4F * bold + 0.2F * tenacity;
         case HOLD -> 0.7F + 0.3F * bold + 0.3F * tenacity;
         case FLANK, ARC -> 1.1F - 0.2F * bold + 0.2F * concern;
         case COVER, CREEP, FREEZE -> 0.8F + 0.5F * concern - 0.2F * bold;
         case WITHDRAW -> 1.2F - 0.4F * tenacity;
      };
      return Mth.clamp(bias, 0.6F, 1.4F);
   }

   public static TargetedManeuver build(
      AnimalHandle handle, Subject subject, MoveStyles.Primitive primitive, double[] band, float speed, float grip, int hand, int dwellTicks
   ) {
      return build(handle, subject, primitive, band, speed, grip, hand, dwellTicks, t -> 0.5F);
   }

   public static TargetedManeuver build(
      AnimalHandle handle,
      Subject subject,
      MoveStyles.Primitive primitive,
      double[] band,
      float speed,
      float grip,
      int hand,
      int dwellTicks,
      Posture.TraitReader traits
   ) {
      double clearance = handle.clearanceTo(subject.girth());
      double min = band[0] + clearance;
      double max = band[1] + clearance;
      double width = handle.bounds().getXsize();
      double ring = ring(min, max, width);
      double springMargin = ManeuverTuning.springMargin(traits.get(Trait.AGGRESSION));
      double withdrawHopMax = ManeuverTuning.withdrawHopMax(traits.get(Trait.NERVOUSNESS));
      double arcOpening = ManeuverTuning.arcRetreatOpeningThreshold(traits.get(Trait.AGGRESSION));
      double arcExtra = ManeuverTuning.arcRetreatDistanceExtra(traits.get(Trait.AGGRESSION));
      if (Ground.airborne(handle)) {
         double pullOut = Math.max(4.0, handle.turnRadius(handle.topSpeed(MoveMode.AIR)));
         Armament arms = Arms.of(handle).armament();
         boolean stream = !arms.present() || arms.stream();
         double standoff = stream ? Math.max(4.0, Math.min(pullOut, max / ManeuverTuning.STREAM_PASS_SLANT)) : pullOut + max;
         switch (primitive) {
            case CLOSE:
               return new StoopManeuver(subject, min, max, speed, ring, standoff);
            case HOLD:
            case COVER:
            case CREEP:
            case FREEZE:
            default:
               break;
            case FLANK:
               return new AirFlankManeuver(subject, min, max, speed, hand, standoff);
            case ARC:
               return new OrbitManeuver(subject, min, max, speed * 1.0F, hand, standoff);
            case WITHDRAW:
               return new BreakManeuver(subject, ring, speed);
         }
      }
      return switch (primitive) {
         case CLOSE -> new CloseManeuver(subject, min, max, speed, ring, grip >= 0.5F, dwellTicks);
         case HOLD -> new StillManeuver(subject, dwellTicks, false, springMargin);
         case FLANK -> new FlankManeuver(subject, min, max, speed);
         case ARC -> new ArcManeuver(subject, min, max, speed * 1.0F, hand, arcOpening, arcExtra);
         case COVER -> new CoverManeuver(subject, min, max, speed, springMargin);
         case CREEP -> new CreepManeuver(subject, min, max, speed, springMargin);
         case FREEZE -> new StillManeuver(subject, dwellTicks, true, springMargin);
         case WITHDRAW -> new WithdrawManeuver(subject, ring, speed, withdrawHopMax);
      };
   }

   public static double ring(double min, double max, double width) {
      return Math.max(max, min + width);
   }

   public static Medium.Choice medium(
      AnimalHandle handle, Situation situation, Posture.TraitReader traits, @Nullable Subject subject, @Nullable Medium.Verdict incumbent, long heldFor
   ) {
      return medium(handle, situation, traits, subject, incumbent, heldFor, 0.0F);
   }

   public static Medium.Choice medium(
      AnimalHandle handle,
      Situation situation,
      Posture.TraitReader traits,
      @Nullable Subject subject,
      @Nullable Medium.Verdict incumbent,
      long heldFor,
      float fear
   ) {
      Medium.Quarry quarry = subject == null
         ? null
         : new Medium.Quarry(
            Wings.of(handle).airGapBelow(subject.estimate()),
            subject.estimate().y - handle.bodyPosition().y,
            handle.bodyPosition().distanceTo(subject.estimate()),
            subject.velocity().length(),
            closing(subject.velocity(), subject.estimate(), handle.bodyPosition()),
            Wings.of(handle).overWater(subject.estimate())
         );
      return Medium.elect(
         situation,
         handle.species().movement(),
         traits,
         quarry,
         Wings.of(handle).canTakeOff(),
         handle.currentMode() == MoveMode.WATER,
         handle.topSpeed(MoveMode.GROUND),
         incumbent,
         heldFor,
         mediumDwell(handle),
         Halter.of(handle).freeFor() < 600L,
         fear
      );
   }

   static double closing(Vec3 velocity, Vec3 from, Vec3 at) {
      Vec3 line = at.subtract(from);
      double length = line.length();
      return length < 1.0E-6 ? velocity.length() : velocity.dot(line) / length;
   }

   public static long mediumDwell(AnimalHandle handle) {
      return handle.modeChangeTicks() * 2L;
   }

   public record Choice(MoveStyles.Primitive primitive, float score, @Nullable MoveStyles.Primitive runnerUp, float runnerUpScore) {
      public float margin() {
         return this.score <= 0.0F ? 0.0F : (this.score - this.runnerUpScore) / this.score;
      }
   }

   public enum Primitive {
      CLOSE(MoveStylesTuning.CLOSE),
      ARC(MoveStylesTuning.ARC),
      FLANK(MoveStylesTuning.FLANK),
      WITHDRAW(MoveStylesTuning.WITHDRAW),
      COVER(MoveStylesTuning.COVER),
      CREEP(MoveStylesTuning.CREEP),
      FREEZE(MoveStylesTuning.FREEZE),
      HOLD(MoveStylesTuning.HOLD);

      private final Posture anchor;

      Primitive(Posture anchor) {
         this.anchor = anchor;
      }

      public Posture anchor() {
         return this.anchor;
      }

      public float affinity(Posture p) {
         float dr = p.radial() - this.anchor.radial();
         float dt = p.tangential() - this.anchor.tangential();
         float dc = p.covert() - this.anchor.covert();
         float dg = p.grip() - this.anchor.grip();
         float distance = (float)Math.sqrt(dr * dr + dt * dt + dc * dc + dg * dg);
         float linear = Math.max(0.0F, 1.0F - distance / MoveStyles.MAX_POSTURE_DISTANCE);
         return linear * linear;
      }

      public String id() {
         return this.name().toLowerCase(Locale.ROOT);
      }
   }
}
