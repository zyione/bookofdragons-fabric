package net.magister.liberanimalium.ride.render;

import net.magister.liberanimalium.anim.api.SignalView;
import net.magister.liberanimalium.anim.molang.AnimQueries;
import net.magister.liberanimalium.anim.molang.SpringChannels;
import net.magister.liberanimalium.body.anim.binding.AnimSignals;
import net.magister.liberanimalium.body.anim.binding.StateSnapshot;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.compat.ClientFrame;
import net.magister.liberanimalium.render.BonePalettes;
import net.magister.liberanimalium.render.FaunaRendererAz;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class RiderAnimator {
   public static volatile boolean staticPreview = false;
   private static boolean handPass;
   private static final int HEAD_PART = 1;
   private static final float[][] HAND = new float[][]{{-1.0F, 10.0F, 0.0F}, {1.0F, 10.0F, 0.0F}};
   private static final int[] ARM_PART = new int[]{2, 3};
   private static final float MODEL_SCALE = 0.9375F;
   private static final float MODEL_TOP = 1.501F;

   private RiderAnimator() {
   }

   public static void beginHandPass() {
      handPass = true;
   }

   public static void endHandPass() {
      handPass = false;
   }

   public static boolean wants(LivingEntity entity) {
      return !handPass && profileFor(entity) != null;
   }

   public static boolean poses(LivingEntity entity) {
      return profileFor(entity) != null;
   }

   private static RiderPoseProfile profileFor(LivingEntity entity) {
      if (entity instanceof AbstractClientPlayer player) {
         return player.getVehicle() instanceof FaunaBase body ? RiderPoses.get(species(body)) : null;
      } else {
         return null;
      }
   }

   public static String species(FaunaBase body) {
      return body.speciesId();
   }

   public static RiderPoseProfile.Seat seat(FaunaBase body) {
      return RiderPoses.seat(species(body));
   }

   public static boolean apply(PlayerModel<?> model, LivingEntity entity, float netHeadYaw, float headPitch) {
      if (handPass) {
         return false;
      } else if (!(entity instanceof AbstractClientPlayer player)) {
         return false;
      } else {
         RiderAnimator.State s = step(player, headPitch);
         if (s == null) {
            return false;
         }

         ModelPart[] parts = parts(model);

         for (int p = 0; p < 6; p++) {
            read(parts[p], s.vanilla[p]);
         }

         s.vanilla[1][3] = s.lookPitch * (float) (Math.PI / 180.0);
         float follow = Mth.clamp(s.profile.headFollow, 0.0F, 1.0F);
         s.vanilla[1][4] = (s.lookYaw + (1.0F - follow) * s.twist) * (float) (Math.PI / 180.0);
         s.rig.solve(s.vanilla, s.delta, s.pose, s.profile.headFollow, s.weight, s.out);
         holdHands(s, (FaunaBase)player.getVehicle(), ClientFrame.partialTick());

         for (int p = 0; p < 6; p++) {
            write(s.out[p], parts[p]);
         }

         model.hat.copyFrom(model.head);
         model.jacket.copyFrom(model.body);
         model.rightSleeve.copyFrom(model.rightArm);
         model.leftSleeve.copyFrom(model.leftArm);
         model.rightPants.copyFrom(model.rightLeg);
         model.leftPants.copyFrom(model.leftLeg);
         return true;
      }
   }

   private static RiderAnimator.State step(AbstractClientPlayer player, float headPitch) {
      if (!(player.getVehicle() instanceof FaunaBase body)) {
         return null;
      } else {
         RiderPoseProfile profile = RiderPoses.get(species(body));
         if (profile == null) {
            return null;
         }

         RiderComponent riders = RiderComponent.of(body);
         if (riders == null) {
            return null;
         }

         float partial = ClientFrame.partialTick();
         double t = body.tickCount + partial;
         long now = System.nanoTime();
         RiderAnimator.State s = riders.state(player.getId());
         boolean fresh = s.clock.frame(t, now);
         if (fresh || s.bodyId != body.getId() || s.profile != profile) {
            start(s, body, profile);
            s.clock.restart(t, now);
         }

         double seconds = s.clock.dtTicks() / 20.0;
         double[] seat = seatAndAxes(s, body, partial);
         s.load.update(t, seat[0], seat[1], seat[2], s.offset[0], s.offset[1], s.offset[2], s.fwd, s.right, s.up, profile.loadSmoothingTicks);
         RiderLook.inFrame(Mth.rotLerp(partial, player.yHeadRotO, player.yHeadRot), headPitch, s.fwd, s.right, s.up, s.look);
         float look = (float)s.look[0];
         float lookYaw = Mth.clamp(look, -85.0F, 85.0F);
         float lookPitch = (float)s.look[1];
         s.lookYaw = lookYaw;
         s.lookPitch = lookPitch;
         s.twist = Mth.clamp(look - lookYaw, -60.0F, 60.0F);
         float blend = profile.blendInTicks <= 0.0F ? 1.0F : (float)(s.clock.mountedTicks() / profile.blendInTicks);
         blend = Mth.clamp(blend, 0.0F, 1.0F);
         s.weight = blend * blend * (3.0F - 2.0F * blend);
         fillVars(s, lookYaw, lookPitch);
         SignalView previous = AnimQueries.bound();
         AnimQueries.install(AnimSignals.scalarKeys());
         AnimQueries.bind(snapshot(s, body));
         RiderQueries.NS.bind(s.vars);

         try {
            s.springs.step(profile.motion, seconds, true);
            buildDelta(s, profile);
            s.delta[RiderBone.TORSO.ordinal()][RiderBone.Dof.RY.ordinal()] += s.twist * (float) (Math.PI / 180.0);
         } finally {
            RiderQueries.NS.unbind();
            AnimQueries.bind(previous);
         }

         return s;
      }
   }

   private static void start(RiderAnimator.State s, FaunaBase body, RiderPoseProfile profile) {
      s.bodyId = body.getId();
      s.profile = profile;
      s.load.reset();
      s.springs.reset();
      s.snapshot = null;
      s.snapshotTick = Integer.MIN_VALUE;
   }

   private static StateSnapshot snapshot(RiderAnimator.State s, FaunaBase body) {
      if (s.snapshot == null || s.snapshotTick != body.tickCount) {
         s.snapshot = StateSnapshot.from(body);
         s.snapshotTick = body.tickCount;
      }

      return s.snapshot;
   }

   private static double[] seatAndAxes(RiderAnimator.State s, FaunaBase body, float partial) {
      String bone = seat(body).attachBone(body.isSaddled());
      Vec3 bodyAt = body.getPosition(partial);
      Vec3 offset = BonePalettes.localOffset(body.getId(), bone);
      Matrix4f frame = BonePalettes.worldMatrix(body.getId(), bone);
      if (offset == null) {
         FaunaRendererAz.SeatStandIn standIn = FaunaRendererAz.seatStandIn(body, bone);
         if (standIn != null) {
            offset = BonePalettes.localOffset(body.getId(), standIn.parent(), standIn.pivot());
            frame = BonePalettes.worldMatrix(body.getId(), standIn.parent());
         }
      }

      double[] c0 = s.c0;
      double[] c1 = s.c1;
      double[] c2 = s.c2;
      boolean seatAxes = frame != null
         && normalise(frame.m00(), frame.m01(), frame.m02(), c0)
         && normalise(frame.m10(), frame.m11(), frame.m12(), c1)
         && normalise(frame.m20(), frame.m21(), frame.m22(), c2);
      if (!seatAxes) {
         double yaw = Math.toRadians(Mth.rotLerp(partial, body.yBodyRotO, body.yBodyRot));
         double sin = Math.sin(yaw);
         double cos = Math.cos(yaw);
         c0[0] = -cos;
         c0[1] = 0.0;
         c0[2] = -sin;
         c1[0] = 0.0;
         c1[1] = 1.0;
         c1[2] = 0.0;
         c2[0] = sin;
         c2[1] = 0.0;
         c2[2] = -cos;
      }

      for (int i = 0; i < 3; i++) {
         s.fwd[i] = -c2[i];
         s.right[i] = c0[i];
         s.up[i] = c1[i];
      }

      s.seat[0] = bodyAt.x;
      s.seat[1] = bodyAt.y;
      s.seat[2] = bodyAt.z;
      s.offset[0] = offset == null ? 0.0 : offset.x;
      s.offset[1] = offset == null ? 0.0 : offset.y;
      s.offset[2] = offset == null ? 0.0 : offset.z;
      return s.seat;
   }

   public static float vanillaBodyYaw(FaunaBase body, AbstractClientPlayer player, float partial) {
      float head = Mth.rotLerp(partial, player.yHeadRotO, player.yHeadRot);
      float vehicle = Mth.rotLerp(partial, body.yBodyRotO, body.yBodyRot);
      float offset = Mth.clamp(Mth.wrapDegrees(head - vehicle), -85.0F, 85.0F);
      float riderBody = head - offset;
      if (offset * offset > 2500.0F) {
         riderBody += offset * 0.2F;
      }

      return riderBody;
   }

   private static boolean normalise(float x, float y, float z, double[] out) {
      double len = Math.sqrt((double)x * x + (double)y * y + (double)z * z);
      if (len > 1.0E-6 && Double.isFinite(len)) {
         out[0] = x / len;
         out[1] = y / len;
         out[2] = z / len;
         return true;
      } else {
         return false;
      }
   }

   private static void fillVars(RiderAnimator.State s, float lookYaw, float lookPitch) {
      double[] v = s.vars;
      RiderLoad l = s.load;
      v[RiderQueries.Var.LOAD_FORWARD.ordinal()] = l.loadForward();
      v[RiderQueries.Var.LOAD_RIGHT.ordinal()] = l.loadRight();
      v[RiderQueries.Var.LOAD_UP.ordinal()] = l.loadUp();
      v[RiderQueries.Var.ACCEL_FORWARD.ordinal()] = l.accelForward();
      v[RiderQueries.Var.ACCEL_RIGHT.ordinal()] = l.accelRight();
      v[RiderQueries.Var.ACCEL_UP.ordinal()] = l.accelUp();
      v[RiderQueries.Var.GRAVITY_FORWARD.ordinal()] = l.gravityForward();
      v[RiderQueries.Var.GRAVITY_RIGHT.ordinal()] = l.gravityRight();
      v[RiderQueries.Var.GRAVITY_UP.ordinal()] = l.gravityUp();
      v[RiderQueries.Var.SPEED.ordinal()] = l.speed();
      v[RiderQueries.Var.TIME.ordinal()] = s.clock.mountedTicks() / 20.0;
      v[RiderQueries.Var.MOUNTED.ordinal()] = s.weight;
      v[RiderQueries.Var.LOOK_YAW.ordinal()] = lookYaw;
      v[RiderQueries.Var.LOOK_PITCH.ordinal()] = lookPitch;
      v[RiderQueries.Var.BOB.ordinal()] = l.bob();
   }

   private static void buildDelta(RiderAnimator.State s, RiderPoseProfile profile) {
      for (int b = 0; b < RiderBone.COUNT; b++) {
         for (int d = 0; d < RiderBone.Dof.COUNT; d++) {
            s.delta[b][d] = profile.pose[b][d];
         }
      }

      if (!staticPreview) {
         s.springs
            .evaluate(profile.motion, 1.0, (c, value) -> s.delta[RiderBone.byKey(c.bone).ordinal()][RiderBone.Dof.byKey(c.dof).ordinal()] += (float)value);
      }

      for (int b = 0; b < RiderBone.COUNT; b++) {
         for (int d = 0; d < RiderBone.Dof.COUNT; d++) {
            boolean rot = d < 3;
            s.pose[b][d] = rot ? (float)Math.toRadians(profile.pose[b][d]) : profile.pose[b][d];
            if (rot) {
               s.delta[b][d] = (float)Math.toRadians(s.delta[b][d]);
            }
         }
      }
   }

   private static void holdHands(RiderAnimator.State s, FaunaBase body, float partial) {
      boolean twisted = s.twist != 0.0F;
      if (twisted) {
         for (int b = 0; b < RiderBone.COUNT; b++) {
            System.arraycopy(s.delta[b], 0, s.still[b], 0, 6);
         }

         s.still[RiderBone.TORSO.ordinal()][RiderBone.Dof.RY.ordinal()] -= s.twist * (float) (Math.PI / 180.0);
         s.rig.solve(s.vanilla, s.still, s.pose, s.profile.headFollow, s.weight, s.held);
      }

      for (int i = 0; i < 2; i++) {
         float[] arm = s.out[ARM_PART[i]];
         RiderPoseProfile.Hold hold = s.profile.hold[i];
         Vector3f target = null;
         float weight = s.weight;
         if (hold != null) {
            target = holdPoint(s, body, hold, partial);
            weight *= hold.weight();
         }

         if (target == null && twisted) {
            target = handOf(s.held[ARM_PART[i]], HAND[i], new Vector3f());
         }

         if (target != null) {
            pointArm(arm, HAND[i], target, weight);
         }
      }
   }

   private static Vector3f holdPoint(RiderAnimator.State s, FaunaBase body, RiderPoseProfile.Hold hold, float partial) {
      Vec3 bone = BonePalettes.worldPosition(body.getId(), hold.bone(), body.getPosition(partial));
      Matrix4f frame = BonePalettes.worldMatrix(body.getId(), hold.bone());
      if (bone != null && frame != null) {
         double[] a = new double[3];
         double[] b = new double[3];
         double[] c = new double[3];
         if (normalise(frame.m00(), frame.m01(), frame.m02(), a)
            && normalise(frame.m10(), frame.m11(), frame.m12(), b)
            && normalise(frame.m20(), frame.m21(), frame.m22(), c)) {
            double wx = bone.x + a[0] * hold.x() + b[0] * hold.y() + c[0] * hold.z();
            double wy = bone.y + a[1] * hold.x() + b[1] * hold.y() + c[1] * hold.z();
            double wz = bone.z + a[2] * hold.x() + b[2] * hold.y() + c[2] * hold.z();
            RiderPoseProfile.Seat seat = s.profile.seat;
            double ox = s.seat[0] + s.offset[0] + s.right[0] * seat.x() + s.up[0] * seat.y() - s.fwd[0] * seat.z();
            double oy = s.seat[1] + s.offset[1] + s.right[1] * seat.x() + s.up[1] * seat.y() - s.fwd[1] * seat.z();
            double oz = s.seat[2] + s.offset[2] + s.right[2] * seat.x() + s.up[2] * seat.y() - s.fwd[2] * seat.z();
            double dx = wx - ox;
            double dy = wy - oy;
            double dz = wz - oz;
            double lx = dx * s.right[0] + dy * s.right[1] + dz * s.right[2];
            double ly = dx * s.up[0] + dy * s.up[1] + dz * s.up[2];
            double lz = -(dx * s.fwd[0] + dy * s.fwd[1] + dz * s.fwd[2]);
            return new Vector3f((float)(-16.0 * lx / 0.9375), (float)(16.0 * (1.501F - ly / 0.9375)), (float)(16.0 * lz / 0.9375));
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static Vector3f handOf(float[] part, float[] hand, Vector3f dest) {
      new Quaternionf().rotationZYX(part[5], part[4], part[3]).transform(dest.set(hand[0], hand[1], hand[2]));
      return dest.add(part[0], part[1], part[2]);
   }

   private static void pointArm(float[] part, float[] hand, Vector3f target, float weight) {
      Quaternionf q = new Quaternionf().rotationZYX(part[5], part[4], part[3]);
      Vector3f now = q.transform(new Vector3f(hand[0], hand[1], hand[2])).normalize();
      Vector3f want = new Vector3f(target).sub(part[0], part[1], part[2]);
      if (!(want.lengthSquared() < 1.0E-6F)) {
         want.normalize();
         Quaternionf turn = new Quaternionf().rotationTo(now, want);
         new Quaternionf().slerp(turn, Mth.clamp(weight, 0.0F, 1.0F)).mul(q, q);
         Vector3f euler = new Vector3f();
         RiderRig.toModelPartAngles(q, euler);
         part[3] = euler.x;
         part[4] = euler.y;
         part[5] = euler.z;
      }
   }

   private static ModelPart[] parts(PlayerModel<?> m) {
      return new ModelPart[]{m.body, m.head, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg};
   }

   private static void read(ModelPart p, float[] o) {
      o[0] = p.x;
      o[1] = p.y;
      o[2] = p.z;
      o[3] = p.xRot;
      o[4] = p.yRot;
      o[5] = p.zRot;
   }

   private static void write(float[] o, ModelPart p) {
      p.x = o[0];
      p.y = o[1];
      p.z = o[2];
      p.xRot = o[3];
      p.yRot = o[4];
      p.zRot = o[5];
   }

   private static RiderAnimator.State find(int playerId) {
      Minecraft mc = Minecraft.getInstance();
      if (!(mc.level != null && mc.level.getEntity(playerId) instanceof AbstractClientPlayer player)) {
         return null;
      } else if (player.getVehicle() instanceof FaunaBase body) {
         RiderComponent riders = RiderComponent.of(body);
         return riders == null ? null : riders.peek(playerId);
      } else {
         return null;
      }
   }

   public static boolean posed(int playerId) {
      return find(playerId) != null;
   }

   public static double var(int playerId, RiderQueries.Var var) {
      RiderAnimator.State s = find(playerId);
      return s == null ? 0.0 : s.vars[var.ordinal()];
   }

   public static double spring(int playerId, String name) {
      RiderAnimator.State s = find(playerId);
      return s == null ? 0.0 : s.springs.value(name);
   }

   public static double channel(int playerId, String key) {
      RiderAnimator.State s = find(playerId);
      return s == null ? 0.0 : s.springs.channel(key);
   }

   public static float weight(int playerId) {
      RiderAnimator.State s = find(playerId);
      return s == null ? 0.0F : s.weight;
   }

   public static String kick(String name, double unitsPerSecond) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return "no player";
      } else {
         RiderAnimator.State s = find(mc.player.getId());
         if (s == null) {
            return "not posed - ride a body whose species has a rider_poses file, in third person";
         } else {
            return !s.springs.kick(name, unitsPerSecond)
               ? "no spring '" + name + "' - this rider's springs: " + s.springs.names()
               : "kicked spring." + name + " by " + unitsPerSecond + "/s";
         }
      }
   }

   static final class State {
      int bodyId = Integer.MIN_VALUE;
      RiderPoseProfile profile;
      final RiderClock clock = new RiderClock();
      float weight;
      final RiderLoad load = new RiderLoad();
      final SpringChannels springs = new SpringChannels();
      final double[] vars = new double[RiderQueries.Var.COUNT];
      final RiderRig rig = new RiderRig();
      final float[][] vanilla = new float[6][6];
      final float[][] delta = new float[RiderBone.COUNT][6];
      final float[][] pose = new float[RiderBone.COUNT][6];
      final float[][] out = new float[6][6];
      final double[] fwd = new double[3];
      final double[] right = new double[3];
      final double[] up = new double[3];
      final double[] c0 = new double[3];
      final double[] c1 = new double[3];
      final double[] c2 = new double[3];
      final double[] seat = new double[3];
      final double[] offset = new double[3];
      final double[] look = new double[2];
      float lookYaw;
      float lookPitch;
      float twist;
      final float[][] still = new float[RiderBone.COUNT][6];
      final float[][] held = new float[6][6];
      StateSnapshot snapshot;
      int snapshotTick = Integer.MIN_VALUE;
   }
}
