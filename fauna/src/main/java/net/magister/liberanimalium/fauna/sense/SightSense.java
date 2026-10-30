package net.magister.liberanimalium.fauna.sense;

import net.magister.liberanimalium.fauna.api.SensedBody;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class SightSense {
   private SightSense() {
   }

   public static float score(SpeciesProfile.Senses senses, Vec3 eye, Vec3 facing, SensedBody body) {
      if (!body.losClear()) {
         return 0.0F;
      }

      Vec3 toBody = body.eyePosition().subtract(eye);
      double distance = toBody.length();
      if (distance >= senses.sightRadius()) {
         return 0.0F;
      }

      float visual = base(distance, senses.sightRadius())
         - anglePenalty(senses, facing, toBody, distance)
         - muzzlePenalty(senses, facing, toBody, distance)
         - darknessPenalty(senses, body.lightLevel())
         - (body.inWater() ? 0.35F : 0.0F)
         - (body.sneaking() ? 0.45F : 0.0F)
         - smallnessPenalty(body);
      return Math.max(0.0F, visual);
   }

   public static boolean sees(SpeciesProfile.Senses senses, Vec3 eye, Vec3 facing, SensedBody body) {
      return score(senses, eye, facing, body) > 0.0F;
   }

   private static float base(double distance, float sightRadius) {
      return (float)(1.0 - distance / sightRadius);
   }

   private static float anglePenalty(SpeciesProfile.Senses senses, Vec3 facing, Vec3 toBody, double distance) {
      if (!(distance <= 0.0) && !(facing.lengthSqr() <= 0.0)) {
         double cos = facing.normalize().dot(toBody.scale(1.0 / distance));
         float offAxisDeg = (float)Math.toDegrees(Math.acos(Mth.clamp(cos, -1.0, 1.0)));
         float perfect = senses.perfectVisionDeg();
         float peripheral = peripheralOf(senses);
         if (offAxisDeg <= perfect) {
            return 0.0F;
         } else {
            return offAxisDeg >= peripheral ? 1.0F : 1.0F * (offAxisDeg - perfect) / (peripheral - perfect);
         }
      } else {
         return 0.0F;
      }
   }

   public static float peripheralOf(SpeciesProfile.Senses senses) {
      return Math.max(senses.peripheralDeg(), senses.perfectVisionDeg() * 1.25F);
   }

   private static float muzzlePenalty(SpeciesProfile.Senses senses, Vec3 facing, Vec3 toBody, double distance) {
      float blind = blindOf(senses);
      if (blind <= 0.0F) {
         return 0.0F;
      } else if (!(distance <= 0.0) && !(facing.lengthSqr() <= 0.0)) {
         double cos = facing.normalize().dot(toBody.scale(1.0 / distance));
         float offAxisDeg = (float)Math.toDegrees(Math.acos(Mth.clamp(cos, -1.0, 1.0)));
         return offAxisDeg >= blind ? 0.0F : 0.9F * (1.0F - offAxisDeg / blind);
      } else {
         return 0.0F;
      }
   }

   public static float blindOf(SpeciesProfile.Senses senses) {
      return Math.min(senses.blindSpotDeg(), senses.perfectVisionDeg() * 0.6F);
   }

   private static float darknessPenalty(SpeciesProfile.Senses senses, int lightLevel) {
      if (senses.nightVision()) {
         return 0.0F;
      }

      float lit = Mth.clamp(lightLevel / 15.0F, 0.0F, 1.0F);
      return 0.8F * (1.0F - lit);
   }

   private static float smallnessPenalty(SensedBody body) {
      float relative = Mth.clamp(body.visualSize() / 1.08F, 0.0F, 1.0F);
      return 0.3F * (1.0F - relative);
   }
}
