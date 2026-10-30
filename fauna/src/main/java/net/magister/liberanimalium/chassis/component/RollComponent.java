package net.magister.liberanimalium.chassis.component;

import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.simulation.FaunaStaminaSystem;
import net.magister.liberanimalium.network.ControlMessageDodge;
import net.magister.liberanimalium.network.DodgeRefusedMessage;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

public class RollComponent implements IFaunaComponent {
   protected FaunaBase body;
   private float accumulatedRoll = 0.0F;
   private boolean isActivelyRolling = false;
   private int dodgeSide;
   private int dodgeAge;
   private int dodgeLength;
   private int dodgeWait;
   private Vec3 dodgeAxis = Vec3.ZERO;
   private int spinSide;
   private float spinAngle;
   private float spinRate;
   private float spinSettle;
   private static final float SPIN_FLOOR = 0.35F;
   private static final double SPIN_MIN_GAIN = 0.05;
   private static final int SPIN_TICKS_MOST = 6;
   private static final float SETTLE_STEP = 0.008333334F;
   private static final float SETTLED_DEGREES = 0.4F;
   private static final float SETTLED_RATE = 3.0F;
   private static final float WATCH_GAIN_MOST = 4.0F;
   private float watchGain;
   private float watchFrom;

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   public void tickRollAccumulation(float currentRollInput, boolean isFlying, boolean onGround, float throttleLevel) {
      if (isFlying && !onGround && Math.abs(currentRollInput) > 0.1F) {
         this.isActivelyRolling = true;
         PhysicsSheet.Rider2d r2 = PhysicsSheet.of(this.body).rider2d();
         float rollScale = 1.0F - Mth.clamp(throttleLevel, 0.0F, 2.0F) * r2.rollThrottleScale();
         float effectiveRollSpeed = r2.rollSpeed() * rollScale;
         this.accumulatedRoll += currentRollInput * effectiveRollSpeed;
         float PI_2 = (float) (Math.PI * 2);
         this.accumulatedRoll %= PI_2;
         if (this.accumulatedRoll > Math.PI) {
            this.accumulatedRoll -= PI_2;
         } else if (this.accumulatedRoll < -Math.PI) {
            this.accumulatedRoll += PI_2;
         }
      } else if (isFlying) {
         this.isActivelyRolling = false;
         if (Math.abs(this.accumulatedRoll) > 0.001F) {
            this.accumulatedRoll = this.accumulatedRoll * (1.0F - PhysicsSheet.of(this.body).rider2d().rollAutoCenter());
            if (Math.abs(this.accumulatedRoll) < 0.005F) {
               this.accumulatedRoll = 0.0F;
            }
         }
      } else {
         this.isActivelyRolling = false;
      }
   }

   public float getAccumulatedRoll() {
      return this.accumulatedRoll;
   }

   public void tickDodge(int ask) {
      PhysicsSheet.Ride ride = PhysicsSheet.of(this.body).ride();
      if (this.dodgeSide != 0) {
         boolean pushed = ++this.dodgeAge >= this.dodgeLength;
         boolean lost = !this.body.isFlying() || this.dodgeAge > 6 * this.dodgeLength;
         if (pushed && !this.rollingOver() || lost) {
            this.dodgeSide = 0;
            this.dodgeWait = ride.dodgeWaitTicks();
            if (lost) {
               this.spinSide = 0;
            }

            if (!this.body.level().isClientSide()) {
               this.body.setDodgeRoll(0.0F);
            }
         }
      } else {
         if (!this.body.isFlying()) {
            this.spinSide = 0;
         }

         if (this.dodgeWait > 0) {
            this.dodgeWait--;
         } else if (ask != 0 && this.begin(ask)) {
            FaunaFlightRotationComponent attitude = this.body.componentRegistry.get(FaunaFlightRotationComponent.class);
            float gain = this.rollingOver() && attitude != null ? (float)attitude.dodgeRollGain() : 0.0F;
            FaunaNetwork.sendToServer(new ControlMessageDodge(ask, gain));
         }
      }
   }

   public void serverDodge(int side, float gain) {
      if (side != 0) {
         if (this.dodgeSide == 0 && this.begin(side > 0 ? 1 : -1)) {
            float claimed = Float.isFinite(gain) ? Mth.clamp(gain, 0.0F, 4.0F) : 0.0F;
            this.body.setDodgeRoll(this.dodgeSide * claimed);
         } else {
            if (this.body.getControllingPassenger() instanceof ServerPlayer rider) {
               FaunaNetwork.sendToPlayer(new DodgeRefusedMessage(this.body.getId()), rider);
            }
         }
      }
   }

   public void refused() {
      if (this.dodgeSide != 0) {
         this.dodgeSide = 0;
         this.spinSide = 0;
         this.dodgeWait = PhysicsSheet.of(this.body).ride().dodgeWaitTicks();
      }
   }

   public void watchDodge(int side, float gain) {
      this.beginSpin(side);
      this.watchGain = gain;
   }

   public float watched(float synced) {
      if (this.spinSide == 0) {
         this.watchFrom = synced;
         return synced;
      } else {
         this.spinStep(0.05F, this.watchGain);
         return this.spinSide == 0 ? synced : this.watchFrom + this.spinDegrees();
      }
   }

   private void beginSpin(int side) {
      this.spinAngle = this.spinSide == 0 ? 0.0F : (this.spinAngle - 360.0F) * this.spinSide * side;
      this.spinSide = side;
      this.spinRate = 0.0F;
      this.spinSettle = 0.0F;
   }

   private boolean begin(int side) {
      PhysicsSheet.Ride ride = PhysicsSheet.of(this.body).ride();
      if (this.body.isFlying() && !this.body.onGround() && this.body.steeredByRider() && !this.body.getLocomotionOrchestrator().isTransitionActive()) {
         boolean flies = this.body.level().isClientSide() && this.body.isControlledByLocalInstance();
         if (flies) {
            double top = this.body.getAttributeValue(Attributes.FLYING_SPEED);
            double share = Math.max(this.body.getThrottleLevel(), top <= 0.0 ? 0.0 : this.body.getDeltaMovement().length() / top);
            if (share < ride.dodgeMinSpeed()) {
               return false;
            }
         }

         FaunaStaminaSystem stamina = this.body.getStaminaSystem();
         if (stamina != null && !stamina.tryConsume(ride.dodgeStamina())) {
            return false;
         }

         this.dodgeSide = side;
         this.dodgeAge = 0;
         this.dodgeAxis = new Vec3(side, 0.0, 0.0).yRot(-this.body.getYRot() * (float) (Math.PI / 180.0));
         FaunaFlightRotationComponent attitude = this.body.componentRegistry.get(FaunaFlightRotationComponent.class);
         boolean rolls = flies && attitude != null && attitude.dodgeAuthority() >= ride.dodgeRollGain();
         if (rolls) {
            this.beginSpin(side);
            this.dodgeLength = dodgeTicks(ride.dodgeTicks(), rollSeconds(attitude.dodgeRollGain(), ride.dodgeRollRate()));
         } else {
            this.dodgeLength = ride.dodgeTicks();
         }

         return true;
      } else {
         return false;
      }
   }

   public static double rollSeconds(double gain, float rollRate) {
      float angle = 0.0F;

      for (int frame = 1; frame <= 1200; frame++) {
         float[] next = spinNext(angle, 0.0F, 0.0F, 0.016666668F, gain, rollRate, 1.0F, 0.5F);
         if (next[2] > 0.0F || next[3] > 0.0F) {
            return frame / 60.0;
         }

         angle = next[0];
      }

      return 20.0;
   }

   public static int dodgeTicks(int plain, double rollSeconds) {
      return Mth.clamp((int)Math.round(rollSeconds * 20.0), plain, 3 * plain);
   }

   public boolean dodging() {
      return this.dodgeSide != 0;
   }

   public int dodgeSide() {
      return this.dodgeSide;
   }

   public static double dodgeShare(int age, int ticks) {
      double t = Mth.clamp((double)age / Math.max(1, ticks), 0.0, 1.0);
      return t * t * (3.0 - 2.0 * t);
   }

   public Vec3 dodgeStep() {
      PhysicsSheet.Ride ride = PhysicsSheet.of(this.body).ride();
      double step = 0.0;
      if (this.dodgeSide != 0 && this.dodgeAge < this.dodgeLength) {
         step = ride.dodgeDistance() * (dodgeShare(this.dodgeAge + 1, this.dodgeLength) - dodgeShare(this.dodgeAge, this.dodgeLength));
      }

      if (this.spinSide != 0 && this.spinSettle > 0.0F) {
         step += drift(this.spinAngle - 360.0F, ride.dodgeOvershoot(), ride.dodgeDrift(), ride.dodgeDistance(), this.dodgeLength);
      }

      return step == 0.0 ? Vec3.ZERO : this.dodgeAxis.scale(step);
   }

   public static double drift(float off, float overshoot, float share, float distance, int ticks) {
      return !(overshoot <= 0.0F) && ticks > 0 ? share * (1.5 * distance / ticks) * (off / overshoot) : 0.0;
   }

   public float dodgeSpeedShare() {
      if (this.dodgeSide != 0 && this.dodgeAge < this.dodgeLength) {
         double step = dodgeShare(this.dodgeAge + 1, this.dodgeLength) - dodgeShare(this.dodgeAge, this.dodgeLength);
         return (float)Mth.clamp(step * this.dodgeLength / 1.5, 0.0, 1.0);
      } else {
         return 0.0F;
      }
   }

   public boolean spinning() {
      return this.spinSide != 0;
   }

   public boolean rollingOver() {
      return this.spinSide != 0 && this.spinSettle <= 0.0F;
   }

   public float spinDegrees() {
      return this.spinSide * this.spinAngle;
   }

   public float spinStep(float seconds, double gain) {
      if (this.spinSide == 0) {
         return 0.0F;
      }

      PhysicsSheet.Ride ride = PhysicsSheet.of(this.body).ride();
      float[] next = spinNext(this.spinAngle, this.spinRate, this.spinSettle, seconds, gain, ride.dodgeRollRate(), ride.dodgeOvershoot(), ride.dodgeBounce());
      float step = next[0] - this.spinAngle;
      this.spinAngle = next[0];
      this.spinRate = next[1];
      this.spinSettle = next[2];
      int side = this.spinSide;
      if (next[3] > 0.0F) {
         this.spinSide = 0;
         this.spinAngle = 0.0F;
         this.spinRate = 0.0F;
         this.spinSettle = 0.0F;
      }

      return side * step;
   }

   public static float[] spinNext(float angle, float rate, float settle, float seconds, double gain, float rollRate, float overshoot, float bounce) {
      if (settle <= 0.0F) {
         float full = (float)(rollRate * Math.max(0.05, gain));
         float left = 360.0F - angle;
         rate = full * Math.max(0.35F, (float)Math.sqrt(Math.max(0.0F, left) / 360.0F));
         angle += rate * seconds;
         if (angle < 360.0F) {
            return new float[]{angle, rate, 0.0F, 0.0F};
         }

         settle = settleFrequency(rate, overshoot, bounce);
         return settle <= 0.0F ? new float[]{360.0F, 0.0F, 0.0F, 1.0F} : new float[]{angle, rate, settle, 0.0F};
      } else {
         float off = angle - 360.0F;
         float damping = 2.0F * Mth.clamp(bounce, 0.05F, 0.95F) * settle;
         float left = seconds;

         while (left > 1.0E-6F) {
            float step = Math.min(left, 0.008333334F);
            left -= step;
            rate += (-settle * settle * off - damping * rate) * step;
            off += rate * step;
         }

         boolean done = Math.abs(off) < 0.4F && Math.abs(rate) < 3.0F;
         return new float[]{done ? 360.0F : 360.0F + off, done ? 0.0F : rate, settle, done ? 1.0F : 0.0F};
      }
   }

   public static float settleFrequency(float rate, float overshoot, float bounce) {
      if (!(overshoot <= 0.0F) && !(rate <= 0.0F)) {
         double z = Mth.clamp(bounce, 0.05F, 0.95F);
         double firstSwing = Math.exp(-z * Math.acos(z) / Math.sqrt(1.0 - z * z));
         return (float)(rate * firstSwing / overshoot);
      } else {
         return 0.0F;
      }
   }

   public boolean isActivelyRolling() {
      return this.isActivelyRolling;
   }

   @Override
   public void serverTick() {
   }

   @Override
   public void save(CompoundTag tag) {
      tag.putFloat("AccumulatedRoll", this.accumulatedRoll);
   }

   @Override
   public void load(CompoundTag tag) {
      this.accumulatedRoll = tag.getFloat("AccumulatedRoll");
   }
}
