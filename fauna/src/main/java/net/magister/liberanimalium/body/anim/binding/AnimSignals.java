package net.magister.liberanimalium.body.anim.binding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.magister.liberanimalium.anim.rig.Footing;
import net.magister.liberanimalium.body.state.AttackMotion;
import net.magister.liberanimalium.body.state.BodyStance;
import net.magister.liberanimalium.body.state.FlightTells;
import net.magister.liberanimalium.body.state.RiderMotion;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.magister.liberanimalium.chassis.component.GripComponent;
import net.magister.liberanimalium.chassis.component.RollComponent;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class AnimSignals {
   private static final List<String> ENUM_KEYS = new ArrayList<>();
   private static final List<AnimSignals.EnumFn> ENUM_FNS = new ArrayList<>();
   private static final Map<String, Integer> ENUM_IDX = new HashMap<>();
   private static final List<String> BOOL_KEYS = new ArrayList<>();
   private static final List<AnimSignals.BoolFn> BOOL_FNS = new ArrayList<>();
   private static final Map<String, Integer> BOOL_IDX = new HashMap<>();
   private static final List<String> SCALAR_KEYS = new ArrayList<>();
   private static final List<AnimSignals.ScalarFn> SCALAR_FNS = new ArrayList<>();
   private static final Map<String, Integer> SCALAR_IDX = new HashMap<>();
   private static final double STATIONARY_EPS = 0.015;
   private static final float TURN_DIR_EPS = 1.5F;
   private static boolean librarySealed;

   private AnimSignals() {
   }

   private static void claim(String key) {
      if (librarySealed && key.indexOf(58) < 0) {
         throw new IllegalArgumentException("a host's animation signal names its namespace: <namespace>:" + key);
      }

      if (isRegistered(key)) {
         throw new IllegalStateException("animation signal '" + key + "' is already registered");
      }
   }

   public static synchronized void registerEnum(String key, AnimSignals.EnumFn fn) {
      claim(key);
      ENUM_IDX.put(key, ENUM_KEYS.size());
      ENUM_KEYS.add(key);
      ENUM_FNS.add(fn);
   }

   public static synchronized void registerBool(String key, AnimSignals.BoolFn fn) {
      claim(key);
      BOOL_IDX.put(key, BOOL_KEYS.size());
      BOOL_KEYS.add(key);
      BOOL_FNS.add(fn);
   }

   public static synchronized void registerScalar(String key, AnimSignals.ScalarFn fn) {
      claim(key);
      SCALAR_IDX.put(key, SCALAR_KEYS.size());
      SCALAR_KEYS.add(key);
      SCALAR_FNS.add(fn);
   }

   public static int enumIndex(String key) {
      Integer i = ENUM_IDX.get(key);
      return i == null ? -1 : i;
   }

   public static int boolIndex(String key) {
      Integer i = BOOL_IDX.get(key);
      return i == null ? -1 : i;
   }

   public static int scalarIndex(String key) {
      Integer i = SCALAR_IDX.get(key);
      return i == null ? -1 : i;
   }

   public static int enumCount() {
      return ENUM_KEYS.size();
   }

   public static int boolCount() {
      return BOOL_KEYS.size();
   }

   public static int scalarCount() {
      return SCALAR_KEYS.size();
   }

   public static boolean isScalarKey(String key) {
      return scalarIndex(key) >= 0;
   }

   public static boolean isRegistered(String key) {
      return enumIndex(key) >= 0 || boolIndex(key) >= 0 || scalarIndex(key) >= 0;
   }

   public static List<String> scalarKeys() {
      return List.copyOf(SCALAR_KEYS);
   }

   public static List<String> allKeys() {
      List<String> out = new ArrayList<>(ENUM_KEYS.size() + BOOL_KEYS.size() + SCALAR_KEYS.size());
      out.addAll(ENUM_KEYS);
      out.addAll(BOOL_KEYS);
      out.addAll(SCALAR_KEYS);
      return List.copyOf(out);
   }

   public static String[] captureEnums(FaunaBase d) {
      String[] a = new String[ENUM_FNS.size()];

      for (int i = 0; i < a.length; i++) {
         a[i] = ENUM_FNS.get(i).read(d);
      }

      return a;
   }

   public static boolean[] captureBools(FaunaBase d) {
      boolean[] a = new boolean[BOOL_FNS.size()];

      for (int i = 0; i < a.length; i++) {
         a[i] = BOOL_FNS.get(i).read(d);
      }

      return a;
   }

   public static float[] captureScalars(FaunaBase d) {
      float[] a = new float[SCALAR_FNS.size()];

      for (int i = 0; i < a.length; i++) {
         a[i] = SCALAR_FNS.get(i).read(d);
      }

      return a;
   }

   private static double horizSpeed(FaunaBase d) {
      if (d.isClimbing()) {
         Footing foot = d.footing();
         return Math.hypot(foot.x() - foot.groundOld().x, foot.z() - foot.groundOld().z);
      }

      double vel = RiderMotion.of(d).horizontalDistance();
      if (vel > 0.001) {
         return vel;
      }

      float synced = d.getHorizontalSpeed();
      if (synced > 0.001F) {
         return synced;
      }

      double dx = d.getX() - d.groundOld().x;
      double dz = d.getZ() - d.groundOld().z;
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static float headingOff(FaunaBase d) {
      Footing foot = d.footing();
      double dx = foot.x() - foot.groundOld().x;
      double dz = foot.z() - foot.groundOld().z;
      if (dx * dx + dz * dz < 1.0E-8 && !d.isClimbing()) {
         Vec3 v = RiderMotion.of(d);
         dx = v.x;
         dz = v.z;
      }

      if (dx * dx + dz * dz < 1.0E-8) {
         return 0.0F;
      }

      float travel = (float)Math.toDegrees(Math.atan2(-dx, dz));
      return Math.abs(Mth.wrapDegrees(travel - foot.yaw()));
   }

   static {
      registerEnum("transport", d -> d.getStateContext().getTransportMode().name());
      registerBool("is_sitting", BodyStance::sitting);
      registerBool("is_incapacitated", BodyStance::incapacitated);
      registerBool("is_charging_takeoff", BodyStance::chargingTakeoff);
      registerBool("is_hovering", FlightTells::hovering);
      registerBool("is_climbing", FlightTells::climbing);
      registerEnum("attack", d -> AttackMotion.fromOrdinal(d.getLayerAttackOrdinal()).name());
      registerEnum("grip", d -> GripComponent.stateOf(d).name());
      registerBool("isLowHealth", FaunaBase::isLowHealth);
      registerBool("isMeleeSwinging", d -> d.swingTime > 0);
      registerBool("isMeleeFallback", d -> d.<Integer>getData(FaunaBase.MELEE_ATTACK_SLOT) > 0);
      registerBool("isSprinting", FaunaBase::isSprinting);
      registerBool("isStationary", d -> horizSpeed(d) < 0.015);
      registerBool("isBraking", d -> d.isBraking() && !d.getStaminaSystem().isStaminaDepleted());
      registerBool("isBoosting", d -> d.isBoosting() && !d.getStaminaSystem().isStaminaDepleted());
      registerBool("isAccelerating", FaunaBase::isAccelerating);
      registerBool("isDiveBreaking", d -> d.getStateContext().isDiveBreaking());
      registerBool("isAbilityActive", FaunaBase::isSecondaryAbilityWindowOpen);
      registerBool("is_dodge_rolling", d -> {
         RollComponent rolls = d.componentRegistry.get(RollComponent.class);
         return rolls != null && rolls.rollingOver();
      });

      for (PoseFlag pose : PoseFlag.values()) {
         registerBool("fauna_" + pose.name().toLowerCase(Locale.ROOT), d -> d.hasFaunaPose(pose));
      }

      registerBool("isMoving", d -> horizSpeed(d) >= 0.015);
      registerScalar("horiz_speed", d -> (float)horizSpeed(d));
      registerScalar("heading_off", AnimSignals::headingOff);
      registerBool("is_steered", FaunaBase::steeredByRider);
      registerBool("steps_reverse", d -> d.steeredByRider() && horizSpeed(d) >= 0.015 && headingOff(d) >= d.animSteps().gait.reverseFrom());
      registerScalar("vert_speed", d -> (float)(Math.abs(RiderMotion.of(d).y) > 0.001 ? RiderMotion.of(d).y : d.getY() - d.yo));
      registerScalar("world_vel_y", d -> (float)(Math.abs(RiderMotion.of(d).y) > 0.001 ? RiderMotion.of(d).y : d.getY() - d.yo));
      registerScalar("total_speed", d -> {
         double h = horizSpeed(d);
         double v = Math.abs(RiderMotion.of(d).y) > 0.001 ? RiderMotion.of(d).y : d.getY() - d.yo;
         return (float)Math.sqrt(h * h + v * v);
      });
      registerScalar("normalized_y", d -> {
         double total = RiderMotion.of(d).length();
         return total > 0.01 ? (float)(RiderMotion.of(d).y / total) : 0.0F;
      });
      registerScalar("local_vel_x", d -> {
         FaunaFlightRotationComponent rot = d.componentRegistry.get(FaunaFlightRotationComponent.class);
         return rot != null ? rot.getRelVelX(0.0F) : 0.0F;
      });
      registerScalar("local_vel_y", d -> {
         FaunaFlightRotationComponent rot = d.componentRegistry.get(FaunaFlightRotationComponent.class);
         return rot != null ? rot.getRelVelY(0.0F) : 0.0F;
      });
      registerScalar("local_vel_z", d -> {
         FaunaFlightRotationComponent rot = d.componentRegistry.get(FaunaFlightRotationComponent.class);
         return rot != null ? rot.getRelVelZ(0.0F) : 0.0F;
      });
      registerScalar("gait_phase", d -> (float)d.getBoneAccessCache().getGaitPhase());
      registerScalar("pitch", d -> d.getStateContext().getCalculatedPitch());
      registerScalar("roll", d -> {
         FaunaFlightRotationComponent rot = d.componentRegistry.get(FaunaFlightRotationComponent.class);
         return rot != null ? Mth.wrapDegrees(rot.getRoll()) : 0.0F;
      });
      registerScalar("roll_abs", d -> {
         FaunaFlightRotationComponent rot = d.componentRegistry.get(FaunaFlightRotationComponent.class);
         return rot != null ? Math.abs(Mth.wrapDegrees(rot.getRoll())) : 0.0F;
      });
      registerScalar("food_level", d -> ((Integer)d.getEntityData().get(FaunaBase.getHungerLevelData())).intValue());
      registerScalar("wariness", d -> AnimSignalSync.warinessOf(d.getSyncedAnimSignalsPacked()));
      registerBool("isBegging", d -> AnimSignalSync.beggingOf(d.getSyncedAnimSignalsPacked()));
      registerBool("isEating", d -> AnimSignalSync.eatingOf(d.getSyncedAnimSignalsPacked()));
      registerEnum("activity", d -> AnimSignalSync.activityName(AnimSignalSync.activityOf(d.getSyncedAnimSignalsPacked())));
      registerEnum("stance", d -> AnimSignalSync.stanceName(d.getSyncedSocialSignalsPacked()));
      registerBool("isRefusing", d -> AnimSignalSync.refusingOf(d.getSyncedAnimSignalsPacked()));
      registerScalar("temper_aggression", d -> AnimSignalSync.traitOf(d.getSyncedTemperamentPacked(), 0));
      registerScalar("temper_bravery", d -> AnimSignalSync.traitOf(d.getSyncedTemperamentPacked(), 1));
      registerScalar("temper_dominance", d -> AnimSignalSync.traitOf(d.getSyncedTemperamentPacked(), 2));
      registerScalar("temper_energy", d -> AnimSignalSync.traitOf(d.getSyncedTemperamentPacked(), 3));
      registerScalar("temper_nervousness", d -> AnimSignalSync.traitOf(d.getSyncedTemperamentPacked(), 4));
      registerScalar("temper_sympathy", d -> AnimSignalSync.traitOf(d.getSyncedTemperamentPacked(), 5));
      registerScalar("yaw_rate", d -> Mth.wrapDegrees(d.yBodyRot - d.yBodyRotO));
      registerEnum("turn_dir", d -> {
         float r = Mth.wrapDegrees(d.yBodyRot - d.yBodyRotO);
         return r > 1.5F ? "RIGHT" : (r < -1.5F ? "LEFT" : "NONE");
      });
      registerBool("on_ground", Entity::onGround);
      registerBool("in_water", Entity::isInWater);
      registerBool("is_in_air", d -> !d.onGround() && !d.isInWater());
      registerScalar("fall_distance", d -> d.fallDistance);
      registerScalar("health_frac", d -> d.getMaxHealth() > 0.0F ? d.getHealth() / d.getMaxHealth() : 0.0F);
      registerScalar("look_yaw", FaunaBase::getNetworkHeadYaw);
      registerScalar("look_pitch", FaunaBase::getNetworkHeadPitch);
      registerBool("has_look_target", FaunaBase::hasNetworkLookTarget);
      librarySealed = true;
   }

   @FunctionalInterface
   public interface BoolFn {
      boolean read(FaunaBase var1);
   }

   @FunctionalInterface
   public interface EnumFn {
      String read(FaunaBase var1);
   }

   @FunctionalInterface
   public interface ScalarFn {
      float read(FaunaBase var1);
   }
}
