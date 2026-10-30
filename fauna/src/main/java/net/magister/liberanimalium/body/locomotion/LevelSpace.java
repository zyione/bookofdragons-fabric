package net.magister.liberanimalium.body.locomotion;

import java.util.function.UnaryOperator;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;

public final class LevelSpace {
   private static final double TURNED = 1.0E-9;
   private Vec3 origin = Vec3.ZERO;
   private Vec3 originO = Vec3.ZERO;
   private Vec3 stand = Vec3.ZERO;
   private Vec3 standO = Vec3.ZERO;
   private float yaw;
   private float yawO;
   private final Quaterniond frame = new Quaterniond();
   private final Quaterniond frameO = new Quaterniond();
   @Nullable
   private UnaryOperator<Vec3> ride;

   public void start(Vec3 was, float yawWas) {
      this.origin = this.stand = was;
      this.yaw = yawWas;
      this.frame.identity();
   }

   public void step(Vec3 originNow, Quaternionf frameNow, float yawNow) {
      this.originO = this.origin;
      this.standO = this.stand;
      this.yawO = this.yaw;
      this.frameO.set(this.frame);
      this.origin = originNow;
      this.frame.set(frameNow.x, frameNow.y, frameNow.z, frameNow.w);
      Vector3d moved = this.frame
         .transformInverse(
            new Vector3d(
               this.origin.x - this.originO.x, this.origin.y - this.originO.y, this.origin.z - this.originO.z
            )
         );
      this.stand = this.standO.add(moved.x, moved.y, moved.z);
      this.yaw = yawNow;
      this.ride = Math.abs(this.frame.dot(this.frameO)) < 0.999999999
         ? carry(this.frameO, this.originO, this.standO, this.frame, this.origin, this.stand)
         : null;
   }

   public Vec3 stand() {
      return this.stand;
   }

   public Vec3 standO() {
      return this.standO;
   }

   public float yaw() {
      return this.yaw;
   }

   public float yawO() {
      return this.yawO;
   }

   @Nullable
   public UnaryOperator<Vec3> ride() {
      return this.ride;
   }

   public UnaryOperator<Vec3> out() {
      return carry(this.frame, this.origin, this.stand, new Quaterniond(), this.origin, this.origin);
   }

   public Vec3 toWorld(Vec3 p) {
      Vector3d d = this.frame.transform(new Vector3d(p.x - this.stand.x, p.y - this.stand.y, p.z - this.stand.z));
      return new Vec3(this.origin.x + d.x, this.origin.y + d.y, this.origin.z + d.z);
   }

   public Vec3 toLevel(Vec3 p) {
      Vector3d d = this.frame
         .transformInverse(new Vector3d(p.x - this.origin.x, p.y - this.origin.y, p.z - this.origin.z));
      return new Vec3(this.stand.x + d.x, this.stand.y + d.y, this.stand.z + d.z);
   }

   private static UnaryOperator<Vec3> carry(Quaterniond fromFrame, Vec3 fromOrigin, Vec3 fromStand, Quaterniond toFrame, Vec3 toOrigin, Vec3 toStand) {
      Quaterniond from = new Quaterniond(fromFrame);
      Quaterniond to = new Quaterniond(toFrame);
      return p -> {
         Vector3d d = from.transform(new Vector3d(p.x - fromStand.x, p.y - fromStand.y, p.z - fromStand.z));
         d.add(fromOrigin.x - toOrigin.x, fromOrigin.y - toOrigin.y, fromOrigin.z - toOrigin.z);
         to.transformInverse(d);
         return new Vec3(toStand.x + d.x, toStand.y + d.y, toStand.z + d.z);
      };
   }

   public static Matrix4f modelToStand(Matrix4f modelToWorld, float yaw, double standOff, Quaternionf attitude, float halfHeight) {
      return new Matrix4f()
         .rotationY((180.0F - yaw) * (float) (Math.PI / 180.0))
         .translate(0.0F, (float)standOff, 0.0F)
         .rotate(new Quaternionf(attitude).conjugate())
         .translate(0.0F, -halfHeight, 0.0F)
         .mul(modelToWorld);
   }
}
