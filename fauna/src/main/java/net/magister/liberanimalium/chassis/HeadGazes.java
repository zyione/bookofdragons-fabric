package net.magister.liberanimalium.chassis;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.state.AttackMotion;
import net.magister.liberanimalium.render.BonePalettes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class HeadGazes {
   private static final double LEAD_REACH = 8.0;
   private static final double CREATURE_REACH = 12.0;
   private static final double ABOUT_REACH = 6.0;
   private static final int[] PICK_TICKS = new int[]{40, 120};
   private final FaunaBase body;
   private final FaunaHeadTrackingSystem lead;
   private final RandomSource random;
   private List<String> heads = List.of();
   private String head = "";
   private final List<HeadGazes.Gaze> gazes = new ArrayList<>();

   HeadGazes(FaunaBase body, FaunaHeadTrackingSystem lead) {
      this.body = body;
      this.lead = lead;
      this.random = RandomSource.create(body.getId() * 31L + 7L);
   }

   public void heads(List<String> prefixes, String head) {
      if (!prefixes.equals(this.heads) || !head.equals(this.head)) {
         this.heads = List.copyOf(prefixes);
         this.head = head;
         this.gazes.clear();

         for (int i = 1; i < this.heads.size(); i++) {
            this.gazes.add(new HeadGazes.Gaze());
         }
      }
   }

   @Nullable
   public float[] angles(String prefix, float partial) {
      int index = this.heads.indexOf(prefix);
      if (index <= 0) {
         return null;
      }

      HeadGazes.Gaze gaze = this.gazes.get(index - 1);
      return new float[]{gaze.yaw(partial), gaze.pitch(partial)};
   }

   void tick() {
      if (this.heads.size() >= 2 && this.body.level().isClientSide()) {
         boolean ridden = this.body.getControllingPassenger() instanceof Player;
         boolean attacking = AttackMotion.fromOrdinal(this.body.getLayerAttackOrdinal()) != AttackMotion.NONE;
         FaunaHeadTrackingSystem.LookPriority rung = this.body.getNetworkLookRung();
         boolean aims = attacking || !ridden && rung.level >= FaunaHeadTrackingSystem.LookPriority.ATTACK_TARGET.level;
         boolean still = this.body.asleep() || this.body.getSyncedIsIncapacitated();
         int now = this.body.tickCount;
         Vec3 leadHead = this.point(this.heads.get(0) + this.head);

         for (int i = 0; i < this.gazes.size(); i++) {
            HeadGazes.Gaze gaze = this.gazes.get(i);
            String prefix = this.heads.get(i + 1);
            if (still) {
               gaze.follow(this.lead.getSmoothedHeadYaw(), this.lead.getSmoothedHeadPitch(), 5.0F);
               gaze.lastFrame = Float.NaN;
            } else {
               if (now >= gaze.until || !gaze.valid()) {
                  gaze.pick(now, aims, prefix, leadHead);
               }

               gaze.aim(this.point(prefix + this.head), gaze.at(leadHead));
            }
         }
      }
   }

   private Vec3 point(String bone) {
      Vec3 at = BonePalettes.worldPosition(this.body.getId(), bone, this.body.position());
      return at != null ? at : this.body.getEyePosition();
   }

   private final class Gaze {
      HeadGazes.Want want = HeadGazes.Want.LEAD;
      int until;
      @Nullable
      Entity creature;
      @Nullable
      Vec3 about;
      float yaw;
      float pitch;
      float yawO;
      float pitchO;
      float yawVelocity;
      float pitchVelocity;
      float lastFrame = Float.NaN;

      float yaw(float partial) {
         return Mth.lerp(partial, this.yawO, this.yaw);
      }

      float pitch(float partial) {
         return Mth.lerp(partial, this.pitchO, this.pitch);
      }

      boolean valid() {
         return this.want != HeadGazes.Want.CREATURE
            || this.creature != null && this.creature.isAlive() && this.creature.distanceToSqr(HeadGazes.this.body) <= 144.0;
      }

      void pick(int now, boolean aims, String prefix, Vec3 leadHead) {
         LivingEntity seen = this.creature(prefix);
         float toLead = aims ? 0.85F : 0.15F;
         float toCreature = seen == null ? 0.0F : (aims ? 0.1F : 0.4F);
         float toAbout = aims ? 0.05F : 0.3F;
         float roll = HeadGazes.this.random.nextFloat() * (toLead + toCreature + toAbout);
         this.creature = null;
         if ((roll = roll - toLead) < 0.0F) {
            this.want = HeadGazes.Want.LEAD;
         } else if ((roll = roll - toCreature) < 0.0F) {
            this.want = HeadGazes.Want.CREATURE;
            this.creature = seen;
         } else {
            this.want = HeadGazes.Want.ABOUT;
            float reach = HeadGazes.this.lead.getMaxHeadYaw() * 0.8F;
            float yaw = HeadGazes.this.lead.bodyFrameYaw() + (HeadGazes.this.random.nextFloat() * 2.0F - 1.0F) * reach;
            float pitch = -10.0F + HeadGazes.this.random.nextFloat() * 30.0F;
            this.about = HeadGazes.this.point(prefix + HeadGazes.this.head).add(Vec3.directionFromRotation(pitch, yaw).scale(6.0));
         }

         this.until = now + HeadGazes.PICK_TICKS[0] + HeadGazes.this.random.nextInt(HeadGazes.PICK_TICKS[1] - HeadGazes.PICK_TICKS[0] + 1);
      }

      @Nullable
      LivingEntity creature(String prefix) {
         Vec3 from = HeadGazes.this.point(prefix + HeadGazes.this.head);
         float maxYaw = HeadGazes.this.lead.getMaxHeadYaw();
         List<LivingEntity> seen = HeadGazes.this.body
            .level()
            .getEntitiesOfClass(
               LivingEntity.class,
               HeadGazes.this.body.getBoundingBox().inflate(12.0),
               e -> e != HeadGazes.this.body && e != HeadGazes.this.body.getVehicle() && e.isAlive()
            );
         float total = 0.0F;
         List<Float> weights = new ArrayList<>();

         for (LivingEntity e : seen) {
            float[] yp = HeadLookSolver.solve(from, HeadGazes.this.lead.bodyFrameYaw(), e.getEyePosition());
            float weight = Math.abs(yp[0]) <= maxYaw ? (e instanceof Player ? 2.0F : 1.0F) : 0.0F;
            weights.add(weight);
            total += weight;
         }

         if (total <= 0.0F) {
            return null;
         }

         float roll = HeadGazes.this.random.nextFloat() * total;

         for (int i = 0; i < seen.size(); i++) {
            if ((roll -= weights.get(i)) < 0.0F) {
               return seen.get(i);
            }
         }

         return null;
      }

      Vec3 at(Vec3 leadHead) {
         return switch (this.want) {
            case CREATURE -> this.creature != null ? this.creature.getEyePosition() : leadHead;
            case ABOUT -> this.about != null ? this.about : leadHead;
            case LEAD -> leadHead.add(HeadGazes.this.lead.headLookDirection().scale(8.0));
         };
      }

      void aim(Vec3 from, Vec3 at) {
         float frame = HeadGazes.this.lead.bodyFrameYaw();
         float[] yp = HeadLookSolver.solve(from, frame, at);
         float maxYaw = HeadGazes.this.lead.getMaxHeadYaw();
         float yawTarget = Mth.clamp(yp[0], -maxYaw, maxYaw);
         float pitchTarget = Mth.clamp(yp[1], -HeadGazes.this.lead.maxPitchUp(), HeadGazes.this.lead.maxPitchDown());
         if (!Float.isNaN(this.lastFrame)) {
            this.yaw = FaunaHeadTrackingSystem.stabilise(this.yaw, Mth.wrapDegrees(frame - this.lastFrame), maxYaw);
         }

         this.lastFrame = frame;
         this.follow(yawTarget, pitchTarget, this.want == HeadGazes.Want.ABOUT ? 9.0F : 5.0F);
      }

      void follow(float yawTarget, float pitchTarget, float easeTicks) {
         this.yawO = this.yaw;
         this.pitchO = this.pitch;
         float[] y = FaunaHeadTrackingSystem.ease(this.yaw, yawTarget, this.yawVelocity, easeTicks);
         float[] p = FaunaHeadTrackingSystem.ease(this.pitch, pitchTarget, this.pitchVelocity, easeTicks);
         this.yaw = y[0];
         this.yawVelocity = y[1];
         this.pitch = p[0];
         this.pitchVelocity = p[1];
      }
   }

   private enum Want {
      LEAD,
      CREATURE,
      ABOUT;
   }
}
