package net.magister.liberanimalium.fauna.behavior;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Armament;
import net.magister.liberanimalium.fauna.api.Arms;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.core.Seeds;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Engage {
   @Nullable
   private UUID locked;
   private Engage.Weapon weapon = Engage.Weapon.MELEE;
   private boolean spent;
   private boolean triggerHeld;
   private long pressedAt = -1L;
   private int senseLostTicks;
   private int dwell;
   private int dwellIndex;
   private boolean breaking;
   private int breakElapsed;
   private int heldPosition;
   private boolean struckLast;
   private float frustration;
   private float pressure;
   private int closureTicks;
   private double gapAtWindowStart = -1.0;
   private boolean beingOutrun;
   private float commit = Float.NaN;
   private long committedSince = Long.MIN_VALUE;
   private long committedAt = Long.MIN_VALUE;
   private long nextLeapAt = Long.MIN_VALUE;
   private int leaps;
   private long exchangedAt = Long.MIN_VALUE;
   private boolean closeBreath;
   private boolean justReleased;
   private boolean sawTarget;

   public Engage.Weapon electWeapon(
      AnimalHandle handle, UUID targetId, Vec3 estimate, double contact, boolean hunting, boolean rangedAllowed, float bravery, float nerves
   ) {
      double gap = estimate.distanceTo(handle.bodyPosition());
      double breathMin = EngageTuning.breathMin(contact);
      Armament arms = Arms.of(handle).armament();
      boolean sees = sees(handle, estimate);
      this.sawTarget = sees;
      if (!hunting && rangedAllowed) {
         this.closeBreath = close(arms, contact);
         if (this.triggerHeld) {
            this.senseLostTicks = sees ? 0 : this.senseLostTicks + 1;
            boolean closedOn = !this.closeBreath && gap < breathMin * 0.5;
            boolean gone = gap > (this.closeBreath ? arms.reach() : 20.0) + 3.0;
            boolean dry = !this.fuelOk(arms);
            boolean lost = this.senseLostTicks > 30;
            if (chargeAloft(handle, arms)) {
               return !dry && !lost ? Engage.Weapon.BREATH : Engage.Weapon.MELEE;
            } else {
               return !closedOn && !gone && !dry && !lost ? Engage.Weapon.BREATH : Engage.Weapon.MELEE;
            }
         } else {
            this.senseLostTicks = 0;
            if (!arms.present() || !this.fuelOk(arms)) {
               return Engage.Weapon.MELEE;
            }

            if (!this.closeBreath) {
               if (gap <= Math.max(contact * 1.6, breathMin)) {
                  return Engage.Weapon.MELEE;
               } else if (this.beingOutrun) {
                  return Engage.Weapon.BREATH;
               } else {
                  return Seeds.unitPair(handle.faunaId(), targetId, this.dwellIndex) < lean(bravery, nerves) ? Engage.Weapon.BREATH : Engage.Weapon.MELEE;
               }
            } else if (gap > arms.reach()) {
               return Engage.Weapon.MELEE;
            } else {
               return Seeds.unitPair(handle.faunaId(), targetId, this.dwellIndex) < lean(bravery, nerves) ? Engage.Weapon.BREATH : Engage.Weapon.MELEE;
            }
         }
      } else {
         return Engage.Weapon.MELEE;
      }
   }

   private static float lean(float bravery, float nerves) {
      return Mth.clamp((nerves + (1.0F - bravery)) * 0.5F, 0.0F, 1.0F);
   }

   static boolean close(Armament arms, double contact) {
      return arms.present() && arms.reach() <= Math.max(contact * 1.6, EngageTuning.breathMin(contact));
   }

   private boolean fuelOk(Armament arms) {
      if (!arms.present()) {
         return false;
      }

      int ticks = arms.fuelTicks();
      if (ticks == Integer.MAX_VALUE) {
         return true;
      }

      if (this.spent) {
         if (ticks >= 25) {
            this.spent = false;
         }

         return !this.spent;
      } else if (ticks <= 4) {
         this.spent = true;
         return false;
      } else {
         return true;
      }
   }

   private boolean mayFire(AnimalHandle handle, Vec3 estimate, double contact) {
      Armament arms = Arms.of(handle).armament();
      if (arms.present() && arms.ready()) {
         double gap = estimate.distanceTo(handle.bodyPosition());
         if (close(arms, contact) ? !(gap > arms.reach()) : !(gap < EngageTuning.breathMin(contact)) && !(gap > 20.0)) {
            Vec3 at = aimPoint(estimate);
            if (sees(handle, estimate) && Arms.of(handle).canAimAt(at)) {
               if (handle.currentMode() == MoveMode.AIR) {
                  Vec3 velocity = handle.velocity();
                  double speed = velocity.length();
                  if (speed > 0.001) {
                     Vec3 line = at.subtract(handle.bodyPosition());
                     double closing = line.lengthSqr() < 1.0E-6 ? 1.0 : velocity.dot(line.normalize()) / speed;
                     if (closing < 0.5) {
                        return false;
                     }
                  }
               }

               return arms.fuelTicks() >= 25;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean sees(AnimalHandle handle, Vec3 estimate) {
      return handle.lineOfSight(handle.eyePosition(), aimPoint(estimate));
   }

   private static Vec3 aimPoint(Vec3 estimate) {
      return estimate.add(0.0, 1.5, 0.0);
   }

   public float commitment(float aggression, float bravery, float nerves, float health, float observed) {
      float willing = (aggression + bravery) * 0.5F;
      willing *= 0.35F + 0.65F * Mth.clamp(health, 0.0F, 1.0F);
      float target = Mth.clamp(willing - observed * nerves * 1.4F, 0.0F, 1.0F);
      if (Float.isNaN(this.commit)) {
         this.commit = target;
         return this.commit;
      } else {
         float rise = Mth.lerp(bravery, 0.02F, 0.12F);
         float rate = target > this.commit ? rise : rise * 2.0F;
         this.commit = this.commit + (target - this.commit) * rate;
         return this.commit;
      }
   }

   public double[] band(Engage.Weapon elected, double contact, double halves, float commitment) {
      if (this.breaking) {
         double out = contact + Mth.lerp(commitment, 8.0, 2.8);
         return new double[]{out, out + 4.0};
      } else if (elected == Engage.Weapon.BREATH && !this.closeBreath) {
         double standMin = EngageTuning.breathStandMin(contact);
         double standMax = EngageTuning.breathStandMax(contact);
         double preferred = Mth.clamp(Mth.lerp(commitment, standMax, standMin), standMin, standMax);
         return new double[]{Math.max(EngageTuning.breathMin(contact), preferred), preferred + EngageTuning.breathReach(contact)};
      } else {
         double reach = meleeReach(contact, halves);
         double ring = Mth.lerp(commitment, 7.0, reach);
         return new double[]{reach, Math.max(ring, reach)};
      }
   }

   public static double meleeReach(double contact, double halves) {
      return clearReach(contact, halves) * 0.7;
   }

   public static double clearReach(double contact, double halves) {
      return Math.max(0.0, contact - halves);
   }

   public float pace(float base) {
      return this.triggerHeld ? base * 1.0F : base;
   }

   public boolean breakingOff() {
      return this.breaking;
   }

   public float takePressure() {
      float taken = this.pressure;
      this.pressure = 0.0F;
      return taken;
   }

   public void tick(
      AnimalHandle handle, UUID targetId, Vec3 estimate, double contact, Engage.Weapon elected, boolean hunting, Fighter self, Bout bout, long now
   ) {
      if (!targetId.equals(this.locked)) {
         this.reset(handle);
         this.locked = targetId;
      }

      if (this.committedAt == Long.MIN_VALUE || now - this.committedAt > 10L) {
         this.committedSince = now;
      }

      this.committedAt = now;
      this.weapon = elected;
      this.justReleased = false;
      Armament arms = Arms.of(handle).armament();
      double gap = estimate.distanceTo(handle.bodyPosition());
      if (this.triggerHeld) {
         boolean inside = !this.closeBreath && gap < EngageTuning.breathMin(contact);
         if (elected == Engage.Weapon.BREATH && arms.present() && arms.ready()) {
            if (chargeAloft(handle, arms)) {
               if (this.pressedAt < 0L) {
                  this.pressedAt = now;
               }

               boolean full = now - this.pressedAt >= arms.chargeTicks();
               if (full && this.mayFire(handle, estimate, contact)) {
                  this.release(handle);
               }
            } else if (!Arms.of(handle).canAimAt(estimate)) {
               if (inside) {
                  this.drop(handle);
               } else {
                  this.release(handle);
               }
            } else if (!arms.stream()) {
               if (this.pressedAt < 0L) {
                  this.pressedAt = now;
               }

               if (now - this.pressedAt >= arms.chargeTicks()) {
                  this.release(handle);
               }
            }
         } else if (!chargeAloft(handle, arms) && !inside) {
            this.release(handle);
         } else {
            this.drop(handle);
         }
      }

      if (gap <= contact * 1.6) {
         this.beingOutrun = false;
         this.gapAtWindowStart = gap;
         this.closureTicks = 0;
         this.heldPosition = this.breaking ? 0 : this.heldPosition + 1;
      } else {
         this.heldPosition = 0;
         if (this.gapAtWindowStart < 0.0) {
            this.gapAtWindowStart = gap;
         }

         if (++this.closureTicks >= 30) {
            this.beingOutrun = gap > this.gapAtWindowStart - 0.75;
            this.gapAtWindowStart = gap;
            this.closureTicks = 0;
         }
      }

      float press = 0.0F;
      if (this.heldPosition > 0) {
         press += 0.9F;
      }

      if (this.struckLast) {
         press += 0.35F;
      }

      if (bout.hurtRecently()) {
         press += 0.35F;
      }

      this.pressure = Mth.clamp(press, 0.0F, 1.0F);
      if (this.struckLast) {
         this.frustration = Math.max(0.0F, this.frustration - 0.39999998F);
      } else if (bout.hurtRecently()) {
         this.frustration = Math.min(1.0F, this.frustration + 0.02F);
      } else {
         this.frustration = Math.max(0.0F, this.frustration - 0.005F);
      }

      if (this.struckLast || bout.hurtRecently()) {
         this.exchangedAt = now;
      }

      this.struckLast = false;
      if (!this.breaking) {
         if (this.dwell > 0) {
            this.dwell--;
         }

         int rhythm = Math.max(1, dwellTicks(self.energy()));
         if (this.heldPosition > 0 && this.heldPosition % rhythm == 0) {
            this.dwellIndex++;
         }

         if (!hunting && bout.annoyance() >= breakPatience(self.aggression(), self.nervousness())) {
            this.breaking = true;
            this.breakElapsed = 0;
            this.heldPosition = 0;
         }
      } else {
         this.breakElapsed += 2;
         if (bout.annoyance() <= resumeAt(self.aggression(), self.nervousness()) || this.breakElapsed >= EngageTuning.BREAK_ABSURDITY_TICKS) {
            this.breaking = false;
            this.breakElapsed = 0;
         }
      }
   }

   public static int dwellTicks(float energy) {
      return Math.round(Mth.lerp(Mth.clamp(energy, 0.0F, 1.0F), 30.0F, 10.0F));
   }

   public static int leapWaitTicks(float energy, float draw) {
      float rhythms = Mth.lerp(Mth.clamp(draw, 0.0F, 1.0F), 3.0F, 8.0F);
      return Math.round(dwellTicks(energy) * rhythms);
   }

   public static float breakPatience(float aggression, float nerves) {
      float tenacity = Mth.clamp(0.5F + 0.5F * (Mth.clamp(aggression, 0.0F, 1.0F) - Mth.clamp(nerves, 0.0F, 1.0F)), 0.0F, 1.0F);
      return 0.75F * (1.0F - 0.5F * (1.0F - tenacity));
   }

   public static float resumeAt(float aggression, float nerves) {
      return breakPatience(aggression, nerves) * 0.55F;
   }

   public boolean strike(AnimalHandle handle, UUID targetId, Vec3 estimate, Engage.Weapon elected, double contact, float energy) {
      if (this.breaking) {
         return false;
      }

      if (elected == Engage.Weapon.BREATH) {
         if (this.justReleased) {
            return false;
         }

         if (!this.triggerHeld && !this.mayFire(handle, estimate, contact) && !this.mayCharge(handle, estimate)) {
            return false;
         }

         if (!this.triggerHeld) {
            this.exchangedAt = this.committedAt;
         }

         Arms.of(handle).holdTrigger(true);
         this.triggerHeld = true;
         return false;
      } else {
         if (this.dwell > 0) {
            return false;
         }

         double gap = estimate.distanceTo(handle.bodyPosition());
         if (gap > contact) {
            if (handle.bodyOnGround() && this.committedAt >= this.nextLeapAt && handle.leap(targetId, estimate)) {
               this.leaps++;
               this.nextLeapAt = this.committedAt + leapWaitTicks(energy, Seeds.unitPair(handle.faunaId(), targetId, this.leaps));
            }

            return false;
         } else {
            if (!sees(handle, estimate)) {
               return false;
            }

            if (!Arms.of(handle).strike(targetId)) {
               return false;
            }

            this.dwell = dwellTicks(energy);
            this.struckLast = true;
            return true;
         }
      }
   }

   private void release(AnimalHandle handle) {
      Arms.of(handle).holdTrigger(false);
      this.triggerHeld = false;
      this.pressedAt = -1L;
      this.justReleased = true;
      this.exchangedAt = this.committedAt;
   }

   public void lose(AnimalHandle handle) {
      if (this.triggerHeld) {
         this.drop(handle);
      }
   }

   private void drop(AnimalHandle handle) {
      Arms.of(handle).dropTrigger();
      this.triggerHeld = false;
      this.pressedAt = -1L;
   }

   private boolean mayCharge(AnimalHandle handle, Vec3 estimate) {
      Armament arms = Arms.of(handle).armament();
      return chargeAloft(handle, arms) && arms.ready() && sees(handle, estimate);
   }

   private static boolean chargeAloft(AnimalHandle handle, Armament arms) {
      return handle.currentMode() == MoveMode.AIR && arms.present() && !arms.stream() && arms.chargeTicks() > 0;
   }

   public boolean sawTarget() {
      return this.sawTarget;
   }

   public Engage.Weapon weapon() {
      return this.weapon;
   }

   public boolean firing() {
      return this.triggerHeld;
   }

   public float frustration() {
      return this.frustration;
   }

   public void commitFor(long now, int ticks) {
      if (this.committedAt != Long.MIN_VALUE && now - this.committedAt <= 10L) {
         this.committedSince = Math.min(this.committedSince, now - ticks);
      }
   }

   public void frustrateTo(float level) {
      this.frustration = Mth.clamp(Math.max(this.frustration, level), 0.0F, 1.0F);
   }

   public int committedTicks(long now) {
      return this.committedSince != Long.MIN_VALUE && now - this.committedAt <= 10L ? (int)Math.min(2147483647L, now - this.committedSince) : 0;
   }

   public int quietTicks(long now) {
      if (this.committedSince != Long.MIN_VALUE && now - this.committedAt <= 10L) {
         long since = Math.max(this.committedSince, this.exchangedAt);
         return (int)Math.min(2147483647L, now - since);
      } else {
         return 0;
      }
   }

   public boolean tankSpent() {
      return this.spent;
   }

   public boolean beingOutrun() {
      return this.beingOutrun;
   }

   public void reset(AnimalHandle handle) {
      if (this.triggerHeld) {
         if (chargeAloft(handle, Arms.of(handle).armament())) {
            this.drop(handle);
         } else {
            this.release(handle);
         }
      }

      this.locked = null;
      this.weapon = Engage.Weapon.MELEE;
      this.spent = false;
      this.dwell = 0;
      this.nextLeapAt = Long.MIN_VALUE;
      this.leaps = 0;
      this.breaking = false;
      this.breakElapsed = 0;
      this.heldPosition = 0;
      this.struckLast = false;
      this.pressure = 0.0F;
      this.frustration = 0.0F;
      this.closureTicks = 0;
      this.gapAtWindowStart = -1.0;
      this.beingOutrun = false;
      this.committedSince = Long.MIN_VALUE;
      this.committedAt = Long.MIN_VALUE;
      this.exchangedAt = Long.MIN_VALUE;
      this.commit = Float.NaN;
   }

   public enum Weapon {
      MELEE,
      BREATH;
   }
}
