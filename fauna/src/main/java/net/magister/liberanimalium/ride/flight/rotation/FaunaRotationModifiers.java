package net.magister.liberanimalium.ride.flight.rotation;

import com.mojang.logging.LogUtils;
import net.magister.liberanimalium.body.locomotion.WaterEngine;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.body.stats.FaunaStatSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.util.Mth;
import net.minecraft.util.SmoothDouble;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.slf4j.Logger;

public class FaunaRotationModifiers {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final double REFERENCE_HZ = 960.0;

   public static FaunaRotationContext.RotationModifier fixNaN(String label) {
      return (rotationInstant, context) -> {
         if (Double.isNaN(rotationInstant.pitch())) {
            rotationInstant = new FaunaRotationInstant(0.0, rotationInstant.yaw(), rotationInstant.roll());
            LOGGER.warn("NaN found in pitch for " + label + ", setting to 0 as fallback");
         }

         if (Double.isNaN(rotationInstant.yaw())) {
            rotationInstant = new FaunaRotationInstant(rotationInstant.pitch(), 0.0, rotationInstant.roll());
            LOGGER.warn("NaN found in yaw for " + label + ", setting to 0 as fallback");
         }

         if (Double.isNaN(rotationInstant.roll())) {
            rotationInstant = new FaunaRotationInstant(rotationInstant.pitch(), rotationInstant.yaw(), 0.0);
            LOGGER.warn("NaN found in roll for " + label + ", setting to 0 as fallback");
         }

         return rotationInstant;
      };
   }

   public static FaunaRotationContext.RotationModifier configureAxes(boolean invertPitch) {
      return (rotationInstant, context) -> new FaunaRotationInstant(
         invertPitch ? -rotationInstant.pitch() : rotationInstant.pitch(), rotationInstant.roll(), rotationInstant.yaw()
      );
   }

   public static FaunaRotationContext.RotationModifier applySensitivity(double pSens, double ySens, double rSens) {
      return (rotationInstant, context) -> rotationInstant.multiply(pSens, ySens, rSens);
   }

   public static double computeAirspeedEnvelope(FaunaBase body) {
      return body == null ? 1.0 : envelopeAt(body, airspeedShare(body));
   }

   public static double envelopeAt(FaunaBase body, double share) {
      PhysicsSheet.Attitude a = body.attitudeNumbers();
      double t = Mth.clamp((share - a.envelopeLow()) / (a.envelopeHigh() - a.envelopeLow()), 0.0, 1.0);
      return t * t * (3.0 - 2.0 * t);
   }

   private static double airspeedShare(FaunaBase body) {
      if (body.isAquatic()) {
         FaunaStatSheet stats = body.getStatSheet();
         double top = stats == null ? 1.0 : WaterEngine.topSpeed(body, stats);
         return body.getDeltaMovement().length() / Math.max(0.001, top);
      } else {
         double topSpeed = body.getAttributeValue(Attributes.FLYING_SPEED);
         return body.getThrottleLevel() * topSpeed / PhysicsSheet.of(body).flight().rollReferenceSpeed();
      }
   }

   public static FaunaRotationContext.RotationModifier applyTurnRateLimitNormalised(FaunaBase body, float frameDelta) {
      return (rotationInstant, context) -> {
         double throttle = body.getThrottleLevel();
         PhysicsSheet.Flight flight = body.flightNumbers();
         float agility = body.getStatSheet().agility();
         float maxTurn = (float)(flight.turnRate() * agility / (1.0 + throttle * flight.turnSpeedPenalty()));
         float maxRoll = maxTurn * flight.rollRate();
         float normFactor = (float)(frameDelta * 960.0);
         float normMaxTurn = maxTurn * normFactor;
         float normMaxRoll = maxRoll * normFactor;
         return new FaunaRotationInstant(
            Mth.clamp(rotationInstant.pitch(), -normMaxTurn, normMaxTurn),
            Mth.clamp(rotationInstant.yaw(), -normMaxTurn, normMaxTurn),
            Mth.clamp(rotationInstant.roll(), -normMaxRoll, normMaxRoll)
         );
      };
   }

   public static FaunaRotationContext.RotationModifier smoothing(SmoothDouble pS, SmoothDouble yS, SmoothDouble rS, double smoothness) {
      return smoothing(pS, yS, rS, smoothness, smoothness);
   }

   public static FaunaRotationContext.RotationModifier smoothing(SmoothDouble pS, SmoothDouble yS, SmoothDouble rS, double smoothness, double rollSmoothness) {
      return (rotationInstant, context) -> new FaunaRotationInstant(
         smoothness == 0.0 ? rotationInstant.pitch() : pS.getNewDeltaValue(rotationInstant.pitch(), 1.0 / smoothness * context.getRenderDelta()),
         smoothness == 0.0 ? rotationInstant.yaw() : yS.getNewDeltaValue(rotationInstant.yaw(), 1.0 / smoothness * context.getRenderDelta()),
         rollSmoothness == 0.0 ? rotationInstant.roll() : rS.getNewDeltaValue(rotationInstant.roll(), 1.0 / rollSmoothness * context.getRenderDelta())
      );
   }

   public static FaunaRotationContext.RotationModifier banking(double bankingStrength, FaunaBase body) {
      return (rotationInstant, context) -> {
         if (body != null && body.getFlightModeTransitionTimer() > 0) {
            return rotationInstant;
         }

         double delta = context.getRenderDelta();
         FaunaRotationInstant currentRotation = context.getCurrentRotation();
         double currentRollRad = currentRotation.roll() * (Math.PI / 180.0);
         double currentPitchRad = currentRotation.pitch() * (Math.PI / 180.0);
         double speedEnvelope = computeAirspeedEnvelope(body);
         double pitchDampening = Math.max(0.0, Math.cos(currentPitchRad));
         double aeroYaw = bankingStrength * pitchDampening * speedEnvelope;
         double conversion = 1.0;
         if (body != null) {
            PhysicsSheet.Flight flight = body.flightNumbers();
            conversion = Mth.lerp(speedEnvelope, flight.bankYawLow(), flight.bankYawHigh());
         }

         double totalYawStrength = bankingStrength * pitchDampening * conversion;
         PhysicsSheet.Attitude a = body == null ? PhysicsSheet.of(null).attitude() : body.attitudeNumbers();
         double dX = Math.sin(currentRollRad) * Math.cos(currentPitchRad) * a.bankYawGain() * totalYawStrength;
         double dY = (-1.0 + Math.cos(currentRollRad)) * Math.cos(currentPitchRad) * a.bankPitchDrop() * aeroYaw;
         return rotationInstant.addAbsolute(dX * delta, dY * delta, currentRollRad);
      };
   }

   public static double rollLevelFactor(PhysicsSheet.Attitude a, double speedEnvelope) {
      double aeroFactor = a.levelAero() * Math.max(a.levelAeroFloor(), a.levelAeroFade() - speedEnvelope);
      return aeroFactor + a.levelHoverRoll() * Math.max(0.0, 1.0 - speedEnvelope * a.levelHoverFade());
   }

   public static FaunaRotationContext.RotationModifier autoLevel(double strengthPitch, double strengthRoll) {
      return (rotationInstant, context) -> {
         double delta = context.getRenderDelta();
         FaunaRotationInstant currentRotation = context.getCurrentRotation();
         double wrappedPitch = Mth.wrapDegrees(currentRotation.pitch());
         double wrappedRoll = Mth.wrapDegrees(currentRotation.roll());
         double speedEnvelope = computeAirspeedEnvelope(context.getBody());
         PhysicsSheet.Attitude a = PhysicsSheet.of(context.getBody()).attitude();
         double aeroFactor = a.levelAero() * Math.max(a.levelAeroFloor(), a.levelAeroFade() - speedEnvelope);
         double hoverBoostPitch = a.levelHoverPitch() * Math.max(0.0, 1.0 - speedEnvelope * a.levelHoverFade());
         double rollFactor = rollLevelFactor(a, speedEnvelope);
         double pitchFactor = aeroFactor + hoverBoostPitch;
         double restorativePitchVel = -wrappedPitch * strengthPitch * delta * pitchFactor;
         double restorativeRollVel = -wrappedRoll * strengthRoll * delta * rollFactor;
         return rotationInstant.add(restorativePitchVel, 0.0, restorativeRollVel);
      };
   }
}
