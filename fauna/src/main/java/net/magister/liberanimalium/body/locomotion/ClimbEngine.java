package net.magister.liberanimalium.body.locomotion;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.anim.rig.Footing;
import net.magister.liberanimalium.anim.rig.GroundOracle;
import net.magister.liberanimalium.anim.rig.LevelGroundOracle;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.body.stats.FaunaStatSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.magister.liberanimalium.ride.flight.rotation.FaunaRotationModifiers;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ClimbEngine implements ModeEngine {
   static final float ENTER_PUSH = 0.5F;
   static final double CONTACT = 0.001;
   static final double STANDS = 0.0625;
   static final double STEP_GAIN = 0.5;
   private static final double MARCH = 0.125;
   private static final int NEVER = -100;
   private FaunaBase body;
   private final Quaternionf frame = new Quaternionf();
   private final Quaternionf prevFrame = new Quaternionf();
   private final Quaternionf attitude = new Quaternionf();
   private final Quaternionf prevAttitude = new Quaternionf();
   private float heading;
   private final Quaternionf view = new Quaternionf();
   private float pace;
   private int ownedTick = -100;
   private int followedTick = -100;
   private boolean pinned;
   private SurfaceField.Reading reading = SurfaceField.NONE;
   private final LevelSpace space = new LevelSpace();
   private int stoodTick = -100;
   @Nullable
   private UnaryOperator<Vec3> left;
   private int leftTick = -100;
   private final GroundOracle faceOracle = new GroundOracle() {
      @Override
      public Vec3 along(Entity entity, Vec3 from, Vec3 to) {
         Vec3 hit = LevelGroundOracle.INSTANCE.along(entity, ClimbEngine.this.toWorld(from), ClimbEngine.this.toWorld(to));
         return hit == null ? null : ClimbEngine.this.toLevel(hit);
      }

      @Override
      public boolean inside(Entity entity, double x, double y, double z) {
         Vec3 at = ClimbEngine.this.toWorld(new Vec3(x, y, z));
         return LevelGroundOracle.INSTANCE.inside(entity, at.x, at.y, at.z);
      }

      @Override
      public double surfaceAbove(Entity entity, double x, double z, double fromY, double upMax) {
         if (!this.inside(entity, x, fromY, z)) {
            return Double.NaN;
         }

         double low = fromY;

         for (double y = fromY; y < fromY + upMax; low = y) {
            y = Math.min(y + 0.125, fromY + upMax);
            if (!this.inside(entity, x, y, z)) {
               double high = y;

               for (int i = 0; i < 8; i++) {
                  double mid = 0.5 * (low + high);
                  if (this.inside(entity, x, mid, z)) {
                     low = mid;
                  } else {
                     high = mid;
                  }
               }

               return high;
            }
         }

         return Double.NaN;
      }
   };
   private final Footing face = new Footing() {
      @Override
      public double x() {
         return ClimbEngine.this.space.stand().x;
      }

      @Override
      public double y() {
         return ClimbEngine.this.space.stand().y;
      }

      @Override
      public double z() {
         return ClimbEngine.this.space.stand().z;
      }

      @Override
      public double yo() {
         return ClimbEngine.this.space.standO().y;
      }

      @Override
      public Vec3 groundOld() {
         return ClimbEngine.this.space.standO();
      }

      @Override
      public float yaw() {
         return ClimbEngine.this.space.yaw();
      }

      @Override
      public float yawO() {
         return ClimbEngine.this.space.yawO();
      }

      @Override
      public boolean onGround() {
         return ClimbEngine.this.reading.held() && ClimbEngine.this.reading.gap() <= 0.0625;
      }

      @Override
      public boolean held(double drop) {
         Vector3f up = SurfaceFrame.up(ClimbEngine.this.frame);
         AABB under = ClimbEngine.this.body.getBoundingBox().move(-up.x * drop, -up.y * drop, -up.z * drop);

         for (VoxelShape shape : ClimbEngine.this.body.level().getBlockCollisions(ClimbEngine.this.body, under)) {
            if (!shape.isEmpty()) {
               return true;
            }
         }

         return false;
      }

      @Override
      public double height() {
         return 2.0 * SurfaceFrame.standOff(SurfaceFrame.up(ClimbEngine.this.frame), ClimbEngine.this.body.getBbWidth(), ClimbEngine.this.body.getBbHeight());
      }

      @Override
      public GroundOracle oracle() {
         return ClimbEngine.this.faceOracle;
      }

      @Nullable
      @Override
      public UnaryOperator<Vec3> ride() {
         return ClimbEngine.this.space.ride();
      }

      @Override
      public Vec3 drawn(Vec3 world, float partialTick) {
         return ClimbEngine.this.space.standO().lerp(ClimbEngine.this.space.stand(), partialTick);
      }

      @Override
      public Matrix4f modelToStand(Matrix4f modelToWorld, float partialTick) {
         return LevelSpace.modelToStand(
            modelToWorld,
            Mth.rotLerp(partialTick, ClimbEngine.this.space.yawO(), ClimbEngine.this.space.yaw()),
            ClimbEngine.this.standOff(partialTick),
            ClimbEngine.this.attitude(partialTick),
            ClimbEngine.this.body.getBbHeight() / 2.0F
         );
      }

      @Override
      public Vec3 toWorld(Vec3 point) {
         return ClimbEngine.this.toWorld(point);
      }
   };

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   @Override
   public LocomotionMode mode() {
      return LocomotionMode.CLIMB;
   }

   public Quaternionf attitude(float partialTick) {
      return !this.owned() && this.body.tickCount - this.followedTick > 1
         ? SurfaceFrame.body(new Quaternionf(), this.body.getYRot())
         : new Quaternionf(this.prevAttitude).slerp(this.attitude, partialTick);
   }

   public Quaternionf look(float partialTick) {
      return new Quaternionf(this.prevFrame).slerp(this.frame, partialTick).mul(this.view);
   }

   public void turnLook(float yaw, float pitch) {
      this.view.rotateY(-yaw * (float) (Math.PI / 180.0)).rotateX(-pitch * (float) (Math.PI / 180.0)).normalize();
   }

   public void levelLook(float seconds) {
      this.view.set(SurfaceFrame.levelled(this.view, (float)(1.0 - Math.exp(-this.lookLevelRate() * seconds))));
   }

   public float[] lookFromBody(float riderYaw, float riderPitch) {
      Vector3f look = this.attitude(1.0F).transformInverse(SurfaceFrame.forward(SurfaceFrame.look(new Quaternionf(), riderYaw, riderPitch)));
      return new float[]{Mth.wrapDegrees(SurfaceFrame.yawOf(look, 180.0F) - 180.0F), SurfaceFrame.pitchOf(look)};
   }

   private double lookLevelRate() {
      PhysicsSheet sheet = PhysicsSheet.of(this.body);
      PhysicsSheet.Attitude a = sheet.attitude();
      return sheet.flight().autoLevelRoll() * FaunaRotationModifiers.rollLevelFactor(a, 0.0) * a.rollAuthorityHover();
   }

   public float headTurn() {
      return Mth.wrapDegrees(SurfaceFrame.yawOf(this.view.transform(new Vector3f(0.0F, 0.0F, -1.0F)), this.heading) - this.heading);
   }

   public double standOff(float partialTick) {
      return SurfaceFrame.standOff(SurfaceFrame.up(this.attitude(partialTick)), this.body.getBbWidth(), this.body.getBbHeight());
   }

   public boolean owned() {
      return this.body.tickCount - this.ownedTick <= 1;
   }

   public boolean movedThisTick() {
      return this.ownedTick == this.body.tickCount;
   }

   public boolean attached() {
      return this.reading.held();
   }

   public SurfaceField.Reading reading() {
      return this.reading;
   }

   public boolean pinned() {
      return this.pinned;
   }

   public void pin(boolean hold) {
      this.pinned = hold;
   }

   public void takeHold(@Nullable LivingEntity rider) {
      this.own(rider);
   }

   public Vector3f askedDirection(LivingEntity rider) {
      this.own(rider);
      return SurfaceFrame.forward(new Quaternionf(this.frame).mul(this.view));
   }

   @Nullable
   public static ClimbEngine lookOf(FaunaBase body) {
      if (body.isClimbing() && body.steeredByRider() && body.isControlledByLocalInstance()) {
         ClimbEngine climb = body.componentRegistry.get(ClimbEngine.class);
         return climb != null && climb.owned() ? climb : null;
      } else {
         return null;
      }
   }

   public static SurfaceField.Reading read(FaunaBase body, double reach) {
      AABB box = body.getBoundingBox();
      List<AABB> solids = new ArrayList<>();

      for (VoxelShape shape : body.level().getBlockCollisions(body, box.inflate(reach))) {
         solids.addAll(shape.toAabbs());
      }

      return SurfaceField.read(box, solids, reach);
   }

   public static boolean reachesFor(FaunaBase body, float forward) {
      if (forward <= 0.5F || !body.horizontalCollision || body.minorHorizontalCollision) {
         return false;
      }

      if (!body.isModeAvailable(LocomotionMode.CLIMB)) {
         return false;
      }

      PhysicsSheet.Climb climb = PhysicsSheet.of(body).climb();
      SurfaceField.Reading faces = read(body, climb.reach());
      return faces.directed() && faces.normal().y < level(climb);
   }

   private static double level(PhysicsSheet.Climb climb) {
      return Math.cos(Math.toRadians(climb.exitAngle()));
   }

   @Override
   public void tick(SteeringIntent intent) {
      FaunaStatSheet stats = this.body.getStatSheet();
      PhysicsSheet sheet = PhysicsSheet.of(this.body);
      PhysicsSheet.Climb climb = sheet.climb();
      LivingEntity rider = this.body.steeredByRider() ? this.body.getControllingPassenger() : null;
      if (stats != null && climb != null && this.body.isModeAvailable(LocomotionMode.CLIMB)) {
         this.own(rider);
         this.reading = read(this.body, climb.reach());
         if (this.reading.held() && (rider != null || this.pinned)) {
            Vector3f frameUp = SurfaceFrame.up(this.frame);
            Vec3 normal = this.reading.directed() ? this.reading.normal() : new Vec3(frameUp.x, frameUp.y, frameUp.z);
            if (rider != null && this.body.isBoosting()) {
               this.pushOff(normal, climb);
            } else {
               this.prevFrame.set(this.frame);
               this.frame.set(SurfaceFrame.turnToward(this.frame, normal, climb.frameRate()));
               float target = rider != null
                  ? SurfaceFrame.localYaw(this.frame, Vec3.directionFromRotation(intent.pitchTarget(), intent.yawTarget()), this.heading)
                  : this.heading;
               float sprintTop = stats.groundSpeed() * 2.0F;
               this.heading = this.heading
                  + WalkEngine.computeYawTurnAmount(this.heading, target, stats.groundTurn(), this.body.getDeltaMovement().length(), sprintTop);
               this.prevAttitude.set(this.attitude);
               this.attitude.set(SurfaceFrame.body(this.frame, this.heading));
               float forward = rider != null ? intent.forward() : 0.0F;
               PhysicsSheet.Ground ground = sheet.ground();
               float maxVel = stats.groundSpeed()
                  * this.body.profile().proficiency(LocomotionMode.CLIMB)
                  * SurfaceFrame.speedShare(SurfaceFrame.incline(SurfaceFrame.up(this.frame)), climb.speed(), climb.ceilingSpeed());
               float top = WalkEngine.gaitTop(maxVel, rider != null, intent.sprint(), forward);
               float effectiveTop = WalkEngine.turnCut(top, Math.abs(Mth.wrapDegrees(target - this.heading)), ground.turnSpeedCoupling());
               this.pace = WalkEngine.pace(this.pace, forward, effectiveTop, WalkEngine.rampStep(top, ground.accelTime() * stats.inertia()));
               float strafe = rider != null ? intent.strafe() * effectiveTop * 0.55F : 0.0F;
               Vec3 nose = onFace(SurfaceFrame.forward(this.attitude), normal);
               Vec3 side = normal.cross(nose);
               Vec3 wanted = nose.scale(this.pace).add(side.scale(strafe));
               Vec3 current = this.body.getDeltaMovement();
               Vec3 along = current.subtract(normal.scale(current.dot(normal)));
               double share = WalkEngine.inertiaShare(ground.response() / stats.inertia(), wanted.dot(along), along.length());
               Vec3 onward = along.add(wanted.subtract(along).scale(share));
               boolean touches = this.reading.gap() <= 0.001;
               Vec3 over = touches ? this.overStep(onward, normal, this.body.maxUpStep()) : null;
               if (over != null) {
                  this.body.setPos(this.body.position().add(over));
                  this.body.setDeltaMovement(onward);
               } else {
                  Vec3 next = touches ? onward : onward.subtract(normal.scale(Math.min(climb.stick(), this.reading.gap())));
                  this.body.setDeltaMovement(next);
                  this.body.move(MoverType.SELF, next);
               }

               this.body.resetFallDistance();
               this.publishAngles();
               double flat = level(climb);
               if (touches && normal.y >= flat && SurfaceFrame.up(this.frame).y >= flat) {
                  this.pinned = false;
                  this.leave();
                  this.body.getLocomotionOrchestrator().requestMode(this.body.restingMode(), ModeChangeCause.CLIMB_OFF);
                  WalkEngine walk = this.body.componentRegistry.get(WalkEngine.class);
                  if (walk != null) {
                     walk.takeOver();
                  }
               } else {
                  this.settle(this.heading);
               }
            }
         } else {
            this.letGo();
         }
      } else {
         this.letGo();
      }
   }

   public void follow() {
      PhysicsSheet.Climb climb = PhysicsSheet.of(this.body).climb();
      if (climb != null) {
         boolean first = this.body.tickCount - this.followedTick > 1 && !this.owned();
         this.followedTick = this.body.tickCount;
         this.reading = read(this.body, climb.reach());
         Vec3 look = Vec3.directionFromRotation(this.body.getXRot(), this.body.getYRot());
         Vector3f nose = new Vector3f((float)look.x, (float)look.y, (float)look.z);
         if (first) {
            this.frame.identity();
            this.attitude.set(SurfaceFrame.fromAxes(SurfaceFrame.up(this.frame), nose, this.attitude));
         }

         this.prevFrame.set(this.frame);
         if (this.reading.directed()) {
            this.frame.set(SurfaceFrame.turnToward(this.frame, this.reading.normal(), climb.frameRate()));
         }

         this.prevAttitude.set(this.attitude);
         this.attitude.set(SurfaceFrame.fromAxes(SurfaceFrame.up(this.frame), nose, this.attitude));
         if (first) {
            this.prevAttitude.set(this.attitude);
         }

         this.settle(SurfaceFrame.localYaw(this.frame, look, this.space.yaw()));
      }
   }

   private void own(@Nullable LivingEntity rider) {
      if (!this.owned()) {
         if (this.body.tickCount - this.followedTick <= 1) {
            Vector3f nose = SurfaceFrame.forward(this.attitude);
            this.heading = SurfaceFrame.localYaw(this.frame, new Vec3(nose.x, nose.y, nose.z), this.heading);
         } else {
            this.frame.identity();
            this.heading = this.body.getYRot();
            this.attitude.set(SurfaceFrame.body(this.frame, this.heading));
            this.prevAttitude.set(this.attitude);
         }

         this.prevFrame.set(this.frame);
         Vector3f nose = SurfaceFrame.forward(this.attitude);
         Vec3 v = this.body.getDeltaMovement();
         this.pace = (float)(v.x * nose.x + v.y * nose.y + v.z * nose.z);
         if (rider != null) {
            this.view.set(new Quaternionf(this.frame).conjugate().mul(SurfaceFrame.look(new Quaternionf(), rider.getYRot(), rider.getXRot())));
         }
      }

      this.ownedTick = this.body.tickCount;
   }

   private static Vec3 onFace(Vector3f direction, Vec3 normal) {
      Vec3 d = new Vec3(direction.x, direction.y, direction.z);
      Vec3 flat = d.subtract(normal.scale(d.dot(normal)));
      return flat.lengthSqr() < 1.0E-8 ? Vec3.ZERO : flat.normalize();
   }

   @Nullable
   private Vec3 overStep(Vec3 wanted, Vec3 normal, double step) {
      double asked = wanted.length();
      if (!(step <= 0.0) && !(asked < 1.0E-6)) {
         Vec3 way = wanted.scale(1.0 / asked);
         AABB box = this.body.getBoundingBox();
         double got = Entity.collideBoundingBox(this.body, wanted, box, this.body.level(), List.of()).dot(way);
         if (got >= asked - 1.0E-6) {
            return null;
         }

         Vec3 out = Entity.collideBoundingBox(this.body, normal.scale(step), box, this.body.level(), List.of());
         Vec3 on = Entity.collideBoundingBox(this.body, wanted, box.move(out), this.body.level(), List.of());
         Vec3 in = Entity.collideBoundingBox(this.body, out.scale(-1.0), box.move(out).move(on), this.body.level(), List.of());
         Vec3 stepped = out.add(on).add(in);
         return stepped.dot(way) >= got + 0.5 * (asked - got) ? stepped : null;
      } else {
         return null;
      }
   }

   private void publishAngles() {
      Vector3f nose = SurfaceFrame.forward(this.attitude);
      this.body.setYRot(SurfaceFrame.yawOf(nose, this.body.getYRot()));
      this.body.setXRot(SurfaceFrame.pitchOf(nose));
      this.body.yBodyRot = this.body.getYRot();
      this.body.yHeadRot = this.body.yBodyRot;
   }

   private void letGo() {
      this.pinned = false;
      this.leave();
      this.body.getLocomotionOrchestrator().requestMode(this.body.restingMode(), ModeChangeCause.CLIMB_OFF);
      Vec3 v = this.body.getDeltaMovement();
      this.body.setDeltaMovement(v.x, WalkEngine.fall(v.y), v.z);
      this.body.move(MoverType.SELF, this.body.getDeltaMovement());
   }

   private void pushOff(Vec3 normal, PhysicsSheet.Climb climb) {
      this.publishAngles();
      this.leave();
      FaunaFlightRotationComponent rotation = this.body.componentRegistry.get(FaunaFlightRotationComponent.class);
      if (rotation != null) {
         float roll = FaunaFlightRotationComponent.decomposeSwingTwist(new Matrix3f().set(this.attitude))[2];
         rotation.setRoll(Float.isNaN(roll) ? 0.0F : roll);
      }

      this.body.getLocomotionOrchestrator().requestMode(this.body.canFly() ? LocomotionMode.FLY : this.body.restingMode(), ModeChangeCause.CLIMB_OFF);
      this.body.setDeltaMovement(normal.scale(climb.pushOff()));
      this.body.move(MoverType.SELF, this.body.getDeltaMovement());
   }

   public boolean footed() {
      return this.stoodTick == this.body.tickCount;
   }

   public Footing footing() {
      return this.face;
   }

   @Nullable
   public UnaryOperator<Vec3> leftRide() {
      return this.leftTick == this.body.tickCount ? this.left : null;
   }

   private void settle(float yaw) {
      Vector3f up = SurfaceFrame.up(this.frame);
      double off = SurfaceFrame.standOff(up, this.body.getBbWidth(), this.body.getBbHeight());
      Vec3 centre = this.body.getBoundingBox().getCenter();
      if (this.body.tickCount - this.stoodTick != 1) {
         this.space.start(new Vec3(this.body.xo, this.body.yo, this.body.zo), this.body.yBodyRotO);
      }

      this.space.step(new Vec3(centre.x - up.x * off, centre.y - up.y * off, centre.z - up.z * off), this.frame, yaw);
      this.stoodTick = this.body.tickCount;
   }

   private void leave() {
      if (this.body.tickCount - this.stoodTick <= 1) {
         this.left = this.space.out();
         this.leftTick = this.body.tickCount;
      }

      this.stoodTick = -100;
   }

   private Vec3 toWorld(Vec3 p) {
      return this.space.toWorld(p);
   }

   private Vec3 toLevel(Vec3 p) {
      return this.space.toLevel(p);
   }
}
