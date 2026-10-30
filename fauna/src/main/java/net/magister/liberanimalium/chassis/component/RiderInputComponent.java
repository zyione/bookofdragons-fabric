package net.magister.liberanimalium.chassis.component;

import net.magister.liberanimalium.body.locomotion.RideTuning;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.world.entity.LivingEntity;

public class RiderInputComponent implements IFaunaComponent {
   private float forward;
   private float strafe;
   private boolean sprinting;
   private boolean wasForwardActive = false;
   private int ticksSinceForwardRelease = 100;
   private boolean cruiseRequested = false;
   protected FaunaBase body;
   private int strafeSide;
   private final int[] sinceStrafeRelease = new int[]{100, 100};
   private int dodgeAsk;
   private boolean boostHeld;
   private boolean sprintHeld;
   private int tameKey = -1;
   private int tameKeyPresses;

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   public void captureInput() {
      LivingEntity rider = this.body.ridingPlayer();
      if (rider != null) {
         this.forward = rider.zza;
         this.strafe = rider.xxa;
         this.sprinting = rider.isSprinting();
         boolean isForwardActive = this.forward > 0.1F;
         if (this.forward < -0.1F || this.isBraking() || !this.body.inThreeDMedium()) {
            this.cruiseRequested = false;
         }

         if (isForwardActive && !this.wasForwardActive && this.ticksSinceForwardRelease < RideTuning.doubleTapTicks(this.body)) {
            this.cruiseRequested = !this.cruiseRequested;
            this.ticksSinceForwardRelease = 100;
         }

         if (!isForwardActive && this.wasForwardActive) {
            this.ticksSinceForwardRelease = 0;
         } else if (!isForwardActive) {
            this.ticksSinceForwardRelease++;
         }

         this.wasForwardActive = isForwardActive;
         int side = this.strafe > 0.1F ? 1 : (this.strafe < -0.1F ? -1 : 0);
         if (side != 0 && side != this.strafeSide) {
            int slot = side > 0 ? 1 : 0;
            if (this.sinceStrafeRelease[slot] < RideTuning.doubleTapTicks(this.body)) {
               this.dodgeAsk = side;
               this.sinceStrafeRelease[slot] = 100;
            }
         }

         if (this.strafeSide != 0 && side != this.strafeSide) {
            this.sinceStrafeRelease[this.strafeSide > 0 ? 1 : 0] = 0;
         }

         for (int slot = 0; slot < 2; slot++) {
            boolean held = side != 0 && (side > 0 ? 1 : 0) == slot;
            if (!held && this.sinceStrafeRelease[slot] < 100) {
               this.sinceStrafeRelease[slot]++;
            }
         }

         this.strafeSide = side;
      } else {
         this.forward = 0.0F;
         this.strafe = 0.0F;
         this.sprinting = false;
         this.cruiseRequested = false;
         this.strafeSide = 0;
         this.dodgeAsk = 0;
      }
   }

   public int takeDodgeAsk() {
      int ask = this.dodgeAsk;
      this.dodgeAsk = 0;
      return ask;
   }

   public float getForward() {
      return this.forward;
   }

   public boolean isCruiseRequested() {
      return this.cruiseRequested;
   }

   public void setCruiseRequested(boolean requested) {
      this.cruiseRequested = requested;
   }

   public float getStrafe() {
      return this.strafe;
   }

   public boolean isBoosting() {
      return this.body.isBoosting();
   }

   public void setBoosting(boolean ascending) {
      this.boostHeld = ascending;
      if (this.body.canBeControlledBy()) {
         this.body.setBoosting(ascending);
      }
   }

   public boolean boostHeld() {
      return this.boostHeld;
   }

   public boolean sprintHeld() {
      return this.sprintHeld;
   }

   public void pressTameKey(int letter) {
      if (letter >= 0 && letter < "FGHJKL".length()) {
         if (letter != this.tameKey) {
            this.tameKey = letter;
            this.tameKeyPresses = 0;
         }

         this.tameKeyPresses++;
      }
   }

   public int tameKey() {
      return this.tameKey;
   }

   public int tameKeyPresses() {
      return this.tameKeyPresses;
   }

   public void drainTameKey() {
      this.tameKey = -1;
      this.tameKeyPresses = 0;
   }

   public boolean isBraking() {
      return this.body.isBraking();
   }

   public void setBraking(boolean v) {
      if (this.body.canBeControlledBy()) {
         this.body.setBraking(v);
      }
   }

   public boolean isFreeLooking() {
      return this.body.isFreeLooking();
   }

   public void setFreeLooking(boolean v) {
      if (this.body.canBeControlledBy()) {
         this.body.setFreeLooking(v);
      }
   }

   public boolean isSprinting() {
      return this.sprinting;
   }

   public void setSprinting(boolean sprinting) {
      this.sprintHeld = sprinting;
      this.sprinting = sprinting;
      if (this.body.canBeControlledBy()) {
         this.body.setSprinting(sprinting);
      }
   }

   @Override
   public void serverTick() {
   }
}
