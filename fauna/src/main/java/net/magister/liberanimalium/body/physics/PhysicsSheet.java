package net.magister.liberanimalium.body.physics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.magister.liberanimalium.body.data.FaunaBodyPlan;
import net.magister.liberanimalium.body.data.FaunaWeightClass;
import net.magister.liberanimalium.body.stats.derivation.StatDerivationTable;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record PhysicsSheet(
   PhysicsSheet.Flight flight,
   PhysicsSheet.Ride ride,
   PhysicsSheet.Attitude attitude,
   PhysicsSheet.Throttle throttle,
   PhysicsSheet.Chase chase,
   PhysicsSheet.AiBank aiBank,
   PhysicsSheet.Rider2d rider2d,
   PhysicsSheet.Landing landing,
   PhysicsSheet.Takeoff takeoff,
   PhysicsSheet.DiveBreak diveBreak,
   PhysicsSheet.Ground ground,
   PhysicsSheet.Water water,
   PhysicsSheet.Medium underwater,
   Map<FaunaWeightClass, PhysicsSheet.Weight> weights,
   Map<FaunaBodyPlan, PhysicsSheet.Plan> plans,
   StatDerivationTable stats,
   @Nullable PhysicsSheet.Climb climb,
   PhysicsSheet.Grip grip
) {
   private static volatile Map<String, PhysicsSheet> byHost = Map.of();
   private static volatile PhysicsSheet fallback;
   private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
   private static final Logger LOGGER = LoggerFactory.getLogger("PhysicsSheet");

   public PhysicsSheet.Flight flightIn(boolean water) {
      return water ? this.underwater.flight() : this.flight;
   }

   public PhysicsSheet.Attitude attitudeIn(boolean water) {
      return water ? this.underwater.attitude() : this.attitude;
   }

   public PhysicsSheet.Throttle throttleIn(boolean water) {
      return water ? this.underwater.throttle() : this.throttle;
   }

   public PhysicsSheet.Weight weight(FaunaWeightClass weightClass) {
      return this.weights.get(weightClass);
   }

   public PhysicsSheet.Plan plan(FaunaBodyPlan plan) {
      return this.plans.get(plan);
   }

   public static PhysicsSheet of(@Nullable Entity creature) {
      return forHost(creature == null ? null : SpeciesAssets.hostOf(creature));
   }

   public static PhysicsSheet forHost(@Nullable String host) {
      PhysicsSheet own = host == null ? null : byHost.get(host);
      if (own != null) {
         return own;
      }

      PhysicsSheet any = fallback;
      if (any == null) {
         throw new IllegalStateException("physics sheet not loaded - no host's data/<host>/fauna/physics.json loaded, see the log");
      }

      if (host != null && !byHost.isEmpty() && WARNED.add(host)) {
         LOGGER.warn("[Physics] {} ships no fauna/physics.json; its creatures read another host's sheet", host);
      }

      return any;
   }

   @Nullable
   public static PhysicsSheet installed(String host) {
      return byHost.get(host);
   }

   public static boolean loaded() {
      return fallback != null;
   }

   public static void install(PhysicsSheet sheet) {
      byHost = Map.of();
      fallback = sheet;
   }

   public static void installAll(Map<String, PhysicsSheet> sheets) {
      if (!sheets.isEmpty()) {
         byHost = Collections.unmodifiableMap(new LinkedHashMap<>(sheets));
         fallback = sheets.values().iterator().next();
         WARNED.clear();
      }
   }

   public static PhysicsSheet parse(JsonObject root) {
      PhysicsSheet.Keys top = new PhysicsSheet.Keys(root, "");
      PhysicsSheet.Flight flight = readFlight(top);
      PhysicsSheet.Ride ride = readRide(top);
      PhysicsSheet.Attitude attitude = readAttitude(top);
      PhysicsSheet.Throttle throttle = readThrottle(top);
      PhysicsSheet.Chase chase = readChase(top);
      PhysicsSheet.AiBank aiBank = readAiBank(top);
      PhysicsSheet.Rider2d rider2d = readRider2d(top);
      PhysicsSheet.Landing landing = readLanding(top);
      PhysicsSheet.Takeoff takeoff = readTakeoff(top);
      PhysicsSheet.DiveBreak diveBreak = readDiveBreak(top);
      PhysicsSheet.Ground ground = readGround(top);
      PhysicsSheet.Water water = readWater(top);
      Map<FaunaWeightClass, PhysicsSheet.Weight> weights = readWeights(top);
      Map<FaunaBodyPlan, PhysicsSheet.Plan> plans = readPlans(top);
      StatDerivationTable stats = StatDerivationTable.parse(top.object("stats").json);
      PhysicsSheet.Climb climb = readClimb(top);
      PhysicsSheet.Grip grip = readGrip(top);
      top.done();
      return new PhysicsSheet(
         flight,
         ride,
         attitude,
         throttle,
         chase,
         aiBank,
         rider2d,
         landing,
         takeoff,
         diveBreak,
         ground,
         water,
         PhysicsSheet.Medium.underwater(flight, attitude, throttle, water),
         weights,
         plans,
         stats,
         climb,
         grip
      );
   }

   private static PhysicsSheet.Flight readFlight(PhysicsSheet.Keys top) {
      PhysicsSheet.Keys f = top.object("flight");
      PhysicsSheet.Flight flight = new PhysicsSheet.Flight(
         f.num("turn_rate"),
         f.num("turn_speed_penalty"),
         f.num("pitch_rate"),
         f.num("yaw_rate"),
         f.num("roll_rate"),
         f.num("bank_rate"),
         f.num("banking"),
         f.num("roll_reference_speed"),
         f.num("bank_yaw_low"),
         f.num("bank_yaw_high"),
         f.num("auto_level_pitch"),
         f.num("auto_level_roll"),
         f.num("response"),
         f.num("vertical_drag"),
         f.num("dive_overdrive_cap"),
         f.num("forward_overdrive_cap"),
         f.num("overdrive_bleed"),
         f.num("glide_bleed"),
         f.num("climb_bleed"),
         f.num("dive_gain"),
         f.num("stall_angle"),
         f.num("stall_recover_angle"),
         f.num("spin_drag"),
         f.num("vane"),
         f.num("vane_speed"),
         f.num("hover_pitch_limit"),
         f.num("hover_box_rate"),
         f.num("strafe_speed"),
         f.num("strafe_lean"),
         f.num("strafe_lean_rate"),
         f.numOr("pitch_pull", 0.0F)
      );
      f.done();
      return flight;
   }

   private static PhysicsSheet.Ride readRide(PhysicsSheet.Keys top) {
      PhysicsSheet.Keys r = top.object("ride");
      PhysicsSheet.Ride ride = new PhysicsSheet.Ride(
         r.num("launch_throttle"),
         r.num("launch_angle"),
         (int)r.num("push_off_ticks"),
         (int)r.num("double_tap_ticks"),
         r.numOr("dodge_distance", 6.0F),
         (int)r.numOr("dodge_ticks", 14.0F),
         r.numOr("dodge_stamina", 12.0F),
         (int)r.numOr("dodge_wait_ticks", 10.0F),
         r.numOr("dodge_min_speed", 0.5F),
         r.numOr("dodge_roll_rate", 4500.0F),
         r.numOr("dodge_roll_gain", 0.35F),
         r.numOr("dodge_overshoot", 40.0F),
         r.numOr("dodge_bounce", 0.3F),
         r.numOr("dodge_drift", 0.5F)
      );
      r.done();
      return ride;
   }

   private static PhysicsSheet.Attitude readAttitude(PhysicsSheet.Keys top) {
      PhysicsSheet.Keys at = top.object("attitude");
      PhysicsSheet.Attitude attitude = new PhysicsSheet.Attitude(
         at.num("envelope_low"),
         at.num("envelope_high"),
         at.num("smoothing"),
         at.num("pitch_authority_hover"),
         at.num("yaw_authority_hover"),
         at.num("roll_authority_hover"),
         at.num("rotation_penalty_scale"),
         at.num("bank_yaw_gain"),
         at.num("bank_pitch_drop"),
         at.num("level_aero"),
         at.num("level_aero_floor"),
         at.num("level_aero_fade"),
         at.num("level_hover_roll"),
         at.num("level_hover_pitch"),
         at.num("level_hover_fade"),
         at.num("free_look_level"),
         at.num("free_look_snap"),
         (int)at.num("free_look_exit_ticks"),
         at.num("free_look_exit_lerp"),
         at.num("roll_deadzone"),
         at.num("roll_deadzone_full"),
         at.num("roll_deadzone_time"),
         at.num("roll_agility"),
         at.num("roll_inertia")
      );
      at.done();
      return attitude;
   }

   private static PhysicsSheet.Throttle readThrottle(PhysicsSheet.Keys top) {
      PhysicsSheet.Keys th = top.object("throttle");
      PhysicsSheet.Throttle throttle = new PhysicsSheet.Throttle(
         th.num("step"),
         th.num("boost"),
         th.num("brake"),
         (int)th.num("power_ramp_ticks"),
         (int)th.num("boost_ramp_ticks"),
         (int)th.num("cruise_hold_ticks"),
         th.num("cruise_throttle"),
         th.num("overdrive_gate"),
         th.num("overdrive_pitch_start"),
         th.num("overdrive_pitch_full"),
         th.num("roll_bleed"),
         th.num("ai_tracking_band"),
         th.num("response_penalty")
      );
      th.done();
      return throttle;
   }

   private static PhysicsSheet.Chase readChase(PhysicsSheet.Keys top) {
      PhysicsSheet.Keys ch = top.object("chase");
      PhysicsSheet.Chase chase = new PhysicsSheet.Chase(ch.num("pitch_scale"), ch.num("climb_factor"), ch.num("dive_factor"));
      ch.done();
      return chase;
   }

   private static PhysicsSheet.AiBank readAiBank(PhysicsSheet.Keys top) {
      PhysicsSheet.Keys ab = top.object("ai_bank");
      PhysicsSheet.AiBank aiBank = new PhysicsSheet.AiBank(ab.num("per_degree"), ab.num("max"), ab.num("rate_min"), ab.num("rate_max"), ab.num("rate_scale"));
      ab.done();
      return aiBank;
   }

   private static PhysicsSheet.Rider2d readRider2d(PhysicsSheet.Keys top) {
      PhysicsSheet.Keys r2 = top.object("rider_2d");
      PhysicsSheet.Rider2d rider2d = new PhysicsSheet.Rider2d(
         r2.num("deadzone_air"),
         r2.num("deadzone_ground"),
         (int)r2.num("deadzone_delay_air"),
         (int)r2.num("deadzone_delay_ground"),
         r2.num("bank_saturation"),
         r2.num("bank_max"),
         r2.num("bank_ease"),
         r2.num("roll_speed"),
         r2.num("roll_throttle_scale"),
         r2.num("roll_auto_center")
      );
      r2.done();
      return rider2d;
   }

   private static PhysicsSheet.Landing readLanding(PhysicsSheet.Keys top) {
      PhysicsSheet.Keys la = top.object("landing");
      PhysicsSheet.Landing landing = new PhysicsSheet.Landing(
         la.num("terminal_base"), la.num("terminal_per_height"), la.num("flare_decay"), la.num("flare_entry"), (int)la.num("window_ticks")
      );
      la.done();
      return landing;
   }

   private static PhysicsSheet.Takeoff readTakeoff(PhysicsSheet.Keys top) {
      PhysicsSheet.Keys to = top.object("takeoff");
      PhysicsSheet.Takeoff takeoff = new PhysicsSheet.Takeoff(
         (int)to.num("headroom"),
         (int)to.num("launch_window_ticks"),
         to.num("cruise_seed"),
         to.num("leap_height"),
         to.num("leap_speed"),
         (int)to.num("leap_ticks")
      );
      to.done();
      return takeoff;
   }

   private static PhysicsSheet.DiveBreak readDiveBreak(PhysicsSheet.Keys top) {
      PhysicsSheet.Keys db = top.object("dive_break");
      PhysicsSheet.DiveBreak diveBreak = new PhysicsSheet.DiveBreak(
         db.num("range"), db.num("min_speed"), db.num("curve"), db.num("raycast_range"), db.num("raycast_step")
      );
      db.done();
      return diveBreak;
   }

   private static PhysicsSheet.Ground readGround(PhysicsSheet.Keys top) {
      PhysicsSheet.Keys g = top.object("ground");
      PhysicsSheet.Ground ground = new PhysicsSheet.Ground(g.num("accel_time"), g.num("response"), g.num("turn_speed_coupling"));
      g.done();
      return ground;
   }

   private static PhysicsSheet.Water readWater(PhysicsSheet.Keys top) {
      PhysicsSheet.Keys wa = top.object("water");
      PhysicsSheet.Water water = new PhysicsSheet.Water(
         wa.num("sensitivity"),
         wa.num("turn"),
         wa.num("turn_penalty"),
         wa.num("roll"),
         wa.num("banking"),
         wa.num("smoothing"),
         wa.num("rest_authority"),
         wa.num("righting"),
         wa.num("pitch_righting"),
         wa.num("surface_roll"),
         wa.num("surface_pitch"),
         wa.num("lift"),
         wa.num("diver_lift"),
         wa.num("damping"),
         wa.num("axial"),
         wa.num("cross"),
         wa.num("stroke"),
         wa.num("glide_bleed"),
         wa.num("sprint"),
         wa.num("strafe"),
         wa.num("sink"),
         (int)wa.num("duck_ticks"),
         wa.num("breach_speed"),
         wa.num("leap_height"),
         (int)wa.num("porpoise_ticks"),
         (int)wa.num("breath_ticks"),
         wa.num("air_low"),
         wa.num("surface_margin"),
         wa.num("enter_share"),
         wa.num("exit_share"),
         wa.numOr("skim_speed", 0.5F),
         wa.numOr("skim_drag", 0.05F),
         wa.numOr("skim_bounce", 0.3F),
         wa.numOr("plunge_drag", 0.05F),
         wa.numOr("plunge_lift", 0.04F),
         (int)wa.numOr("plunge_ticks", 30.0F)
      );
      wa.done();
      return water;
   }

   private static Map<FaunaWeightClass, PhysicsSheet.Weight> readWeights(PhysicsSheet.Keys top) {
      PhysicsSheet.Keys w = top.object("weight_classes");
      Map<FaunaWeightClass, PhysicsSheet.Weight> weights = new EnumMap<>(FaunaWeightClass.class);

      for (FaunaWeightClass c : FaunaWeightClass.values()) {
         PhysicsSheet.Keys k = w.object(c.name());
         weights.put(
            c,
            new PhysicsSheet.Weight(
               k.num("gravity"),
               k.num("inertia"),
               k.num("health"),
               k.num("armor"),
               k.num("speed"),
               k.num("camera_distance"),
               k.num("camera_shake"),
               k.num("float_line"),
               k.numOr("mass", 0.0F),
               k.numOr("lift", 0.0F),
               k.numOr("load_drag", 1.0F)
            )
         );
         k.done();
      }

      w.done();
      return weights;
   }

   private static Map<FaunaBodyPlan, PhysicsSheet.Plan> readPlans(PhysicsSheet.Keys top) {
      PhysicsSheet.Keys p = top.object("body_plans");
      Map<FaunaBodyPlan, PhysicsSheet.Plan> plans = new EnumMap<>(FaunaBodyPlan.class);

      for (FaunaBodyPlan plan : FaunaBodyPlan.values()) {
         PhysicsSheet.Keys k = p.object(plan.name());
         plans.put(
            plan,
            new PhysicsSheet.Plan(
               k.num("top_speed"),
               k.num("agility"),
               k.num("stall_speed"),
               k.num("climb_bleed"),
               k.num("dive_gain"),
               k.num("auto_level"),
               k.num("bank_yaw"),
               k.numOr("dive_cap", 0.0F)
            )
         );
         k.done();
      }

      p.done();
      return plans;
   }

   private static PhysicsSheet.Climb readClimb(PhysicsSheet.Keys top) {
      PhysicsSheet.Climb climb = null;
      PhysicsSheet.Keys cl = top.objectOr("climb");
      if (cl != null) {
         climb = new PhysicsSheet.Climb(
            cl.num("reach"), cl.num("stick"), cl.num("frame_rate"), cl.num("speed"), cl.num("ceiling_speed"), cl.num("push_off"), cl.num("exit_angle")
         );
         cl.done();
      }

      return climb;
   }

   private static PhysicsSheet.Grip readGrip(PhysicsSheet.Keys top) {
      PhysicsSheet.Grip grip = PhysicsSheet.Grip.DEFAULT;
      PhysicsSheet.Keys gr = top.objectOr("grip");
      if (gr != null) {
         PhysicsSheet.Keys swing = gr.object("swing");
         PhysicsSheet.Keys beat = gr.object("beat");
         grip = new PhysicsSheet.Grip(
            swing.num("rate"),
            swing.num("damping"),
            beat.num("rate"),
            beat.num("damping"),
            gr.num("hang"),
            gr.num("sweep"),
            gr.num("trail"),
            gr.num("tuck"),
            gr.num("pull"),
            gr.num("bite"),
            gr.num("curl_max"),
            gr.num("fling"),
            gr.num("thrown_drag")
         );
         swing.done();
         beat.done();
         gr.done();
      }

      return grip;
   }

   public record AiBank(float perDegree, float max, float rateMin, float rateMax, float rateScale) {
   }

   public record Attitude(
      float envelopeLow,
      float envelopeHigh,
      float smoothing,
      float pitchAuthorityHover,
      float yawAuthorityHover,
      float rollAuthorityHover,
      float rotationPenaltyScale,
      float bankYawGain,
      float bankPitchDrop,
      float levelAero,
      float levelAeroFloor,
      float levelAeroFade,
      float levelHoverRoll,
      float levelHoverPitch,
      float levelHoverFade,
      float freeLookLevel,
      float freeLookSnap,
      int freeLookExitTicks,
      float freeLookExitLerp,
      float rollDeadzone,
      float rollDeadzoneFull,
      float rollDeadzoneTime,
      float rollAgility,
      float rollInertia
   ) {
   }

   public record Chase(float pitchScale, float climbFactor, float diveFactor) {
   }

   public record Climb(float reach, float stick, float frameRate, float speed, float ceilingSpeed, float pushOff, float exitAngle) {
   }

   public record DiveBreak(float range, float minSpeed, float curve, float raycastRange, float raycastStep) {
   }

   public record Flight(
      float turnRate,
      float turnSpeedPenalty,
      float pitchRate,
      float yawRate,
      float rollRate,
      float bankRate,
      float banking,
      float rollReferenceSpeed,
      float bankYawLow,
      float bankYawHigh,
      float autoLevelPitch,
      float autoLevelRoll,
      float response,
      float verticalDrag,
      float diveOverdriveCap,
      float forwardOverdriveCap,
      float overdriveBleed,
      float glideBleed,
      float climbBleed,
      float diveGain,
      float stallAngle,
      float stallRecoverAngle,
      float spinDrag,
      float vane,
      float vaneSpeed,
      float hoverPitchLimit,
      float hoverBoxRate,
      float strafeSpeed,
      float strafeLean,
      float strafeLeanRate,
      float pitchPull
   ) {
   }

   public record Grip(
      float swingRate,
      float swingDamping,
      float beatRate,
      float beatDamping,
      float hang,
      float sweep,
      float trail,
      float tuck,
      float pull,
      float bite,
      float curlMax,
      float fling,
      float thrownDrag
   ) {
      public static final PhysicsSheet.Grip DEFAULT = new PhysicsSheet.Grip(
         0.26F, 0.75F, 2.8F, 0.3F, 60.0F, 70.0F, 30.0F, 0.2F, 0.3F, 1.0F, 100.0F, 2.0F, 0.99F
      );

      public PhysicsSheet.Grip laden(double load) {
         if (!(load > 0.0)) {
            return this;
         }

         float slower = (float)(1.0 / Math.sqrt(1.0 + load));
         return new PhysicsSheet.Grip(
            this.swingRate * slower,
            this.swingDamping,
            this.beatRate * slower,
            this.beatDamping,
            this.hang,
            this.sweep,
            this.trail,
            this.tuck,
            this.pull,
            this.bite,
            this.curlMax,
            this.fling,
            this.thrownDrag
         );
      }
   }

   public record Ground(float accelTime, float response, float turnSpeedCoupling) {
   }

   private static final class Keys {
      private final JsonObject json;
      private final String path;
      private final Set<String> read = new HashSet<>();

      Keys(JsonObject json, String path) {
         this.json = json;
         this.path = path;
      }

      private JsonElement require(String key) {
         JsonElement e = this.json.get(key);
         if (e == null) {
            throw new JsonSyntaxException("physics: missing " + this.path + key);
         }

         this.read.add(key);
         return e;
      }

      float num(String key) {
         JsonElement e = this.require(key);
         if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            return e.getAsFloat();
         } else {
            throw new JsonSyntaxException("physics: " + this.path + key + " must be a number");
         }
      }

      float numOr(String key, float absent) {
         return this.json.has(key) ? this.num(key) : absent;
      }

      PhysicsSheet.Keys object(String key) {
         JsonElement e = this.require(key);
         if (!e.isJsonObject()) {
            throw new JsonSyntaxException("physics: " + this.path + key + " must be an object");
         } else {
            return new PhysicsSheet.Keys(e.getAsJsonObject(), this.path + key + ".");
         }
      }

      @Nullable
      PhysicsSheet.Keys objectOr(String key) {
         return this.json.has(key) ? this.object(key) : null;
      }

      void done() {
         for (String key : this.json.keySet()) {
            if (!this.read.contains(key) && !key.startsWith("__")) {
               throw new JsonSyntaxException("physics: " + this.path + key + " is read by nothing");
            }
         }
      }
   }

   public record Landing(float terminalBase, float terminalPerHeight, float flareDecay, float flareEntry, int windowTicks) {
   }

   public record Medium(PhysicsSheet.Flight flight, PhysicsSheet.Attitude attitude, PhysicsSheet.Throttle throttle) {
      public static PhysicsSheet.Medium underwater(PhysicsSheet.Flight a, PhysicsSheet.Attitude at, PhysicsSheet.Throttle th, PhysicsSheet.Water w) {
         PhysicsSheet.Flight flight = new PhysicsSheet.Flight(
            a.turnRate() * w.turn(),
            a.turnSpeedPenalty() * w.turnPenalty(),
            a.pitchRate() * w.turn(),
            a.yawRate() * w.turn(),
            a.rollRate() * w.roll(),
            a.bankRate(),
            a.banking() * w.banking(),
            a.rollReferenceSpeed(),
            a.bankYawHigh() * w.restAuthority(),
            a.bankYawHigh(),
            0.0F,
            0.0F,
            a.response(),
            1.0F,
            w.sprint(),
            w.sprint(),
            a.overdriveBleed(),
            w.glideBleed(),
            0.0F,
            0.0F,
            a.stallAngle(),
            a.stallRecoverAngle(),
            a.spinDrag(),
            a.vane(),
            a.vaneSpeed(),
            90.0F,
            a.hoverBoxRate(),
            w.strafe(),
            a.strafeLean(),
            a.strafeLeanRate(),
            0.0F
         );
         PhysicsSheet.Attitude attitude = new PhysicsSheet.Attitude(
            at.envelopeLow(),
            at.envelopeHigh(),
            w.smoothing(),
            w.restAuthority(),
            w.restAuthority(),
            w.restAuthority(),
            at.rotationPenaltyScale(),
            at.bankYawGain(),
            0.0F,
            at.levelAero(),
            at.levelAeroFloor(),
            at.levelAeroFade(),
            at.levelHoverRoll(),
            at.levelHoverPitch(),
            at.levelHoverFade(),
            at.freeLookLevel(),
            at.freeLookSnap(),
            at.freeLookExitTicks(),
            at.freeLookExitLerp(),
            at.rollDeadzone(),
            at.rollDeadzoneFull(),
            at.rollDeadzoneTime(),
            0.0F,
            w.smoothing()
         );
         PhysicsSheet.Throttle throttle = new PhysicsSheet.Throttle(
            th.step() * w.stroke(),
            th.boost() * w.stroke(),
            th.brake() * w.stroke(),
            th.powerRampTicks(),
            th.boostRampTicks(),
            th.cruiseHoldTicks(),
            th.cruiseThrottle(),
            th.overdriveGate(),
            th.overdrivePitchStart(),
            th.overdrivePitchFull(),
            th.rollBleed(),
            th.aiTrackingBand(),
            th.responsePenalty()
         );
         return new PhysicsSheet.Medium(flight, attitude, throttle);
      }
   }

   public record Plan(float topSpeed, float agility, float stallSpeed, float climbBleed, float diveGain, float autoLevel, float bankYaw, float diveCap) {
      public static final PhysicsSheet.Plan GROUND = new PhysicsSheet.Plan(1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F);
   }

   public record Ride(
      float launchThrottle,
      float launchAngle,
      int pushOffTicks,
      int doubleTapTicks,
      float dodgeDistance,
      int dodgeTicks,
      float dodgeStamina,
      int dodgeWaitTicks,
      float dodgeMinSpeed,
      float dodgeRollRate,
      float dodgeRollGain,
      float dodgeOvershoot,
      float dodgeBounce,
      float dodgeDrift
   ) {
   }

   public record Rider2d(
      float deadzoneAir,
      float deadzoneGround,
      int deadzoneDelayAir,
      int deadzoneDelayGround,
      float bankSaturation,
      float bankMax,
      float bankEase,
      float rollSpeed,
      float rollThrottleScale,
      float rollAutoCenter
   ) {
   }

   public record Takeoff(int headroom, int launchWindowTicks, float cruiseSeed, float leapHeight, float leapSpeed, int leapTicks) {
   }

   public record Throttle(
      float step,
      float boost,
      float brake,
      int powerRampTicks,
      int boostRampTicks,
      int cruiseHoldTicks,
      float cruiseThrottle,
      float overdriveGate,
      float overdrivePitchStart,
      float overdrivePitchFull,
      float rollBleed,
      float aiTrackingBand,
      float responsePenalty
   ) {
   }

   public record Water(
      float sensitivity,
      float turn,
      float turnPenalty,
      float roll,
      float banking,
      float smoothing,
      float restAuthority,
      float righting,
      float pitchRighting,
      float surfaceRoll,
      float surfacePitch,
      float lift,
      float diverLift,
      float damping,
      float axial,
      float cross,
      float stroke,
      float glideBleed,
      float sprint,
      float strafe,
      float sink,
      int duckTicks,
      float breachSpeed,
      float leapHeight,
      int porpoiseTicks,
      int breathTicks,
      float airLow,
      float surfaceMargin,
      float enterShare,
      float exitShare,
      float skimSpeed,
      float skimDrag,
      float skimBounce,
      float plungeDrag,
      float plungeLift,
      int plungeTicks
   ) {
   }

   public record Weight(
      float gravity,
      float inertia,
      float health,
      float armor,
      float speed,
      float cameraDistance,
      float cameraShake,
      float floatLine,
      float mass,
      float lift,
      float loadDrag
   ) {
   }
}
