package net.magister.liberanimalium.chassis;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.UnaryOperator;
import net.magister.liberanimalium.anim.rig.EntityFooting;
import net.magister.liberanimalium.anim.rig.GroundOracle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class ShoulderFooting extends EntityFooting {
   public static final double SHOULDER_SHARE = 0.75;
   private static final double PIXEL = 0.05859375;
   private static final double HALF_WIDTH = 0.46875;
   private static final double HALF_DEPTH = 0.1171875;
   private static final double TORSO = 0.703125;
   private static final double HEAD_HALF = 0.234375;
   private static final double HEAD_HEIGHT = 0.46875;
   private static final double HEAD_GRIP = 0.1171875;
   private static final double PLAYER_WIDTH = 0.6;
   private static final Map<FaunaBase, ShoulderFooting> OF = Collections.synchronizedMap(new WeakHashMap<>());
   private final FaunaBase pet;
   private final ShoulderFooting.Oracle oracle = new ShoulderFooting.Oracle();
   private Player owner;

   private ShoulderFooting(FaunaBase pet) {
      super(pet);
      this.pet = pet;
   }

   static ShoulderFooting of(FaunaBase pet, Player owner) {
      ShoulderFooting footing = OF.computeIfAbsent(pet, ShoulderFooting::new);
      footing.owner = owner;
      return footing;
   }

   public static double shoulderY(Player owner) {
      return owner.getY() + owner.getBbHeight() * 0.75;
   }

   @Override
   public boolean reaches() {
      return false;
   }

   @Override
   public Vec3 groundOld() {
      return this.ride().apply(new Vec3(this.pet.xo, this.pet.yo, this.pet.zo));
   }

   @Override
   public boolean onGround() {
      return true;
   }

   @Override
   public boolean held(double drop) {
      return true;
   }

   @Override
   public GroundOracle oracle() {
      return this.oracle;
   }

   @Override
   public UnaryOperator<Vec3> ride() {
      return this.carry(1.0F);
   }

   @Override
   public UnaryOperator<Vec3> rideBetween(float partial) {
      return this.carry(partial);
   }

   private UnaryOperator<Vec3> carry(float share) {
      Player o = this.owner;
      double height = o.getBbHeight() * 0.75;
      double x = Mth.lerp(share, o.xo, o.getX());
      double y = Mth.lerp(share, o.yo, o.getY());
      double z = Mth.lerp(share, o.zo, o.getZ());
      Vec3 neckOld = new Vec3(o.xo, o.yo + height, o.zo);
      Vec3 neckNow = new Vec3(x, y + height, z);
      float headOld = o.yHeadRotO;
      float headNow = Mth.rotLerp(share, o.yHeadRotO, o.yHeadRot);
      float pitchOld = o.xRotO;
      float pitchNow = Mth.lerp(share, o.xRotO, o.getXRot());
      double turn = Mth.wrapDegrees(o.yBodyRot - o.yBodyRotO) * share * (float) (Math.PI / 180.0);
      double cos = Math.cos(turn);
      double sin = Math.sin(turn);
      return p -> {
         Vector3f local = toHead(p.subtract(neckOld), headOld, pitchOld);
         if (onHead(local)) {
            Vector3f moved = fromHead(local, headNow, pitchNow);
            return neckNow.add(moved.x, moved.y, moved.z);
         } else {
            double dx = p.x - o.xo;
            double dz = p.z - o.zo;
            return new Vec3(x + dx * cos - dz * sin, p.y - o.yo + y, z + dx * sin + dz * cos);
         }
      };
   }

   private static Vector3f toHead(Vec3 offset, float headYaw, float pitch) {
      Vector3f v = new Vector3f((float)offset.x, (float)offset.y, (float)offset.z);
      return v.rotateY(headYaw * (float) (Math.PI / 180.0)).rotateX(-pitch * (float) (Math.PI / 180.0));
   }

   private static Vector3f fromHead(Vector3f local, float headYaw, float pitch) {
      return new Vector3f(local).rotateX(pitch * (float) (Math.PI / 180.0)).rotateY(-headYaw * (float) (Math.PI / 180.0));
   }

   private static boolean onHead(Vector3f local) {
      double reach = 0.3515625;
      return Math.abs(local.x) <= reach && Math.abs(local.z) <= reach && local.y >= -0.1171875 && local.y <= 0.5859375;
   }

   public static Matrix4f modelFrame(FaunaBase pet, Player owner, float partialTick, float renderYaw) {
      double height = owner.getBbHeight() * 0.75;
      Vector3f seat = new Vector3f(
            (float)(pet.getX() - owner.getX()), (float)(pet.getY() - owner.getY() - height), (float)(pet.getZ() - owner.getZ())
         )
         .rotateY(owner.yBodyRot * (float) (Math.PI / 180.0));
      double neckX = Mth.lerp(partialTick, owner.xo, owner.getX()) - Mth.lerp(partialTick, pet.xo, pet.getX());
      double neckY = Mth.lerp(partialTick, owner.yo, owner.getY()) + height - Mth.lerp(partialTick, pet.yo, pet.getY());
      double neckZ = Mth.lerp(partialTick, owner.zo, owner.getZ()) - Mth.lerp(partialTick, pet.zo, pet.getZ());
      float headYaw = Mth.rotLerp(partialTick, owner.yHeadRotO, owner.yHeadRot);
      float pitch = Mth.lerp(partialTick, owner.xRotO, owner.getXRot());
      return new Matrix4f()
         .translate((float)neckX, (float)neckY, (float)neckZ)
         .rotateY(-headYaw * (float) (Math.PI / 180.0))
         .rotateX(pitch * (float) (Math.PI / 180.0))
         .translate(seat)
         .rotateY(renderYaw * (float) (Math.PI / 180.0));
   }

   public static float renderYaw(LivingEntity entity, float partialTick) {
      float yaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
      if (entity.isPassenger() && entity.getVehicle() instanceof LivingEntity vehicle) {
         float head = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
         yaw = Mth.rotLerp(partialTick, vehicle.yBodyRotO, vehicle.yBodyRot);
         float off = Mth.clamp(Mth.wrapDegrees(head - yaw), -85.0F, 85.0F);
         yaw = head - off;
         if (off * off > 2500.0F) {
            yaw += off * 0.2F;
         }
      }

      return yaw;
   }

   private final class Oracle implements GroundOracle {
      private double[] local(double x, double y, double z) {
         double yaw = ShoulderFooting.this.owner.yBodyRot * (float) (Math.PI / 180.0);
         double dx = x - ShoulderFooting.this.owner.getX();
         double dz = z - ShoulderFooting.this.owner.getZ();
         return new double[]{
            dx * Math.cos(yaw) + dz * Math.sin(yaw), y - ShoulderFooting.shoulderY(ShoulderFooting.this.owner), -dx * Math.sin(yaw) + dz * Math.cos(yaw)
         };
      }

      private double scale() {
         return ShoulderFooting.this.owner.getBbWidth() / 0.6;
      }

      @Override
      public boolean inside(Entity entity, double x, double y, double z) {
         double[] p = this.local(x, y, z);
         double s = this.scale();
         return Math.abs(p[0]) <= 0.46875 * s && Math.abs(p[2]) <= 0.1171875 * s && p[1] <= 0.0 && p[1] >= -0.703125 * s;
      }

      @Override
      public Vec3 along(Entity entity, Vec3 from, Vec3 to) {
         double[] a = this.local(from.x, from.y, from.z);
         double[] b = this.local(to.x, to.y, to.z);
         double s = this.scale();
         double[] lo = new double[]{-0.46875 * s, -0.703125 * s, -0.1171875 * s};
         double[] hi = new double[]{0.46875 * s, 0.0, 0.1171875 * s};
         double enter = 0.0;
         double exit = 1.0;
         boolean startsInside = true;

         for (int i = 0; i < 3; i++) {
            double d = b[i] - a[i];
            if (a[i] < lo[i] || a[i] > hi[i]) {
               startsInside = false;
            }

            if (Math.abs(d) < 1.0E-12) {
               if (a[i] < lo[i] || a[i] > hi[i]) {
                  return null;
               }
            } else {
               double t0 = (lo[i] - a[i]) / d;
               double t1 = (hi[i] - a[i]) / d;
               enter = Math.max(enter, Math.min(t0, t1));
               exit = Math.min(exit, Math.max(t0, t1));
               if (enter > exit) {
                  return null;
               }
            }
         }

         return startsInside ? null : from.add(to.subtract(from).scale(enter));
      }

      @Override
      public double surfaceAbove(Entity entity, double x, double z, double fromY, double upMax) {
         if (!this.inside(entity, x, fromY, z)) {
            return Double.NaN;
         }

         double top = ShoulderFooting.shoulderY(ShoulderFooting.this.owner);
         return top - fromY <= upMax ? top : Double.NaN;
      }
   }
}
