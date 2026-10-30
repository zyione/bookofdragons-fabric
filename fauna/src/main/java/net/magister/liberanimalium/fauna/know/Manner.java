package net.magister.liberanimalium.fauna.know;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Manner {
   public static final float NEUTRAL = bearing(false, 0.0F);

   private Manner() {
   }

   public static float closingRate(Vec3 subjectPos, Vec3 subjectVel, Vec3 observerPos) {
      Vec3 toObserver = observerPos.subtract(subjectPos);
      double length = toObserver.length();
      return length < 1.0E-4 ? 0.0F : (float)subjectVel.dot(toObserver.scale(1.0 / length));
   }

   public static float approachFraction(float closingRate) {
      return Mth.clamp(closingRate / 0.36F, 0.0F, 1.0F);
   }

   public static float bearing(boolean sneaking, float approach) {
      float a = Mth.clamp(approach, 0.0F, 1.0F);
      float raw = 0.15F + 0.85F * a * a;
      return Mth.clamp(sneaking ? raw * 0.35F : raw, 0.0F, 1.0F);
   }

   public static float of(float attribution, float armament, float armorFraction, boolean sneaking) {
      return compose(bearing(sneaking, attribution), Mth.clamp(armament, 0.0F, 1.0F), armour(armorFraction));
   }

   public static float armour(float armorFraction) {
      return Mth.clamp(armorFraction, 0.0F, 1.0F) * 0.3F;
   }

   public static float compose(float bearing, float armament, float armour) {
      float loudest = Math.max(bearing, Math.max(armament, armour));
      float rest = bearing + armament + armour - loudest;
      return Mth.clamp(loudest + 0.4F * rest, 0.0F, 1.0F);
   }

   public static float carriage(float armament, float armorFraction) {
      return compose(NEUTRAL, Mth.clamp(armament, 0.0F, 1.0F), armour(armorFraction));
   }

   public static float hold(float occupancy, float carriage) {
      return Math.max(Mth.clamp(occupancy, 0.0F, 1.0F), pressureFactor(carriage));
   }

   public static float occupancy(double distance, double reach) {
      return reach <= 0.0 ? 0.0F : (float)Mth.clamp(1.0 - Math.max(0.0, distance) / reach, 0.0, 1.0);
   }

   public static float advance(float current, float approachFraction, float intrusion, float hold, double retention, long elapsed) {
      float fraction = Mth.clamp(approachFraction, 0.0F, 1.0F);
      float inside = Mth.clamp(intrusion, 0.0F, 1.0F);
      long ticks = Math.max(0L, elapsed);
      if (fraction > 0.0F) {
         double press = fraction * Math.max(fraction, inside);
         double accrued = Math.max(current, fraction) + 0.00625 * press * ticks;
         return Mth.clamp((float)accrued, 0.0F, 1.0F);
      }

      double kept = Mth.lerp(Mth.clamp(hold, 0.0F, 1.0F), (float)Mth.clamp(retention, 0.0, 1.0), 1.0F);
      float decayed = current;
      if (ticks > 0L && current > 0.0F) {
         decayed = current * (float)Math.pow(kept, ticks);
      }

      return Mth.clamp(decayed, 0.0F, 1.0F);
   }

   public static float spiked(float current, float amount) {
      return Mth.clamp(Mth.clamp(current, 0.0F, 1.0F) + Mth.clamp(amount, 0.0F, 1.0F), 0.0F, 1.0F);
   }

   public static float gainFactor(float manner) {
      return Mth.lerp(pressureFactor(manner), 1.0F, 1.8F);
   }

   public static float reach(float manner) {
      return Mth.lerp(pressureFactor(manner), 1.0F, 2.0F);
   }

   public static float pressureFactor(float manner) {
      return Mth.clamp((Mth.clamp(manner, 0.0F, 1.0F) - NEUTRAL) / (1.0F - NEUTRAL), 0.0F, 1.0F);
   }
}
