package net.magister.liberanimalium.render;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.anim.rig.Attitude;
import net.magister.liberanimalium.anim.rig.BlockChanges;
import net.magister.liberanimalium.anim.rig.BlockSampler;
import net.magister.liberanimalium.anim.rig.BoneAxis;
import net.magister.liberanimalium.anim.rig.BoneFk;
import net.magister.liberanimalium.anim.rig.Footing;
import net.magister.liberanimalium.anim.rig.GatherCache;
import net.magister.liberanimalium.anim.rig.LevelGroundOracle;
import net.magister.liberanimalium.anim.rig.Penetration;
import net.magister.liberanimalium.anim.rig.PointChain;
import net.magister.liberanimalium.anim.rig.RigFrame;
import net.magister.liberanimalium.anim.rig.RigPass;
import net.magister.liberanimalium.anim.rig.RigSpec;
import net.magister.liberanimalium.anim.rig.TargetLift;
import net.magister.liberanimalium.anim.rig.VoxelView;
import net.magister.liberanimalium.body.BodyPlan;
import net.magister.liberanimalium.body.PartSpec;
import net.magister.liberanimalium.body.locomotion.WaterEngine;
import net.magister.liberanimalium.body.state.TransportMode;
import net.magister.liberanimalium.body.stats.FaunaStatSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.debug.client.FaunaLayers;
import net.magister.liberanimalium.debug.client.LegReachDebugRenderer;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

final class ChainPass implements RigPass {
   private static final Map<Integer, ChainPass.Chains> CHAINS = new ConcurrentHashMap<>();
   private static final double STIFF_MUSCLE = 1000000.0;
   private static volatile int logTicks = 0;
   private static volatile int logEntity = -1;
   private static int logTick = Integer.MIN_VALUE;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Matrix4f LONE = new Matrix4f();
   private static final double TIMING_RATE = 0.03333333333333333;
   private static final PerfClock.Section CLOCK_STEP = PerfClock.client("rig.chain.step");
   private static final PerfClock.Section CLOCK_PROBE = PerfClock.client("rig.chain.probe");
   private static final PerfClock.Section CLOCK_GATHER = PerfClock.client("rig.chain.gather");
   private static final PerfClock.Section CLOCK_FK = PerfClock.client("rig.chain.fk");
   private static final PerfClock.Section CLOCK_AIM = PerfClock.client("rig.chain.aim");
   private static final float AIM_REFUSE_COS = -0.94F;
   private static final Vector3f AIM_A = new Vector3f();
   private static final Vector3f AIM_B = new Vector3f();
   private static final Matrix4f ACCUM = new Matrix4f();
   private static final Vector3f JOINT = new Vector3f();

   static void forget(int entityId) {
      CHAINS.remove(entityId);
      ChainWatch.forget(entityId);
   }

   static void forgetAll() {
      CHAINS.clear();
   }

   static Map<String, ChainStatus> snapshot(int entityId) {
      ChainPass.Chains chains = CHAINS.get(entityId);
      Map<String, ChainStatus> out = new HashMap<>();
      if (chains == null) {
         return out;
      }

      for (ChainPass.ChainState st : chains.chains) {
         PointChain c = st.chain;
         int contacts = 0;
         int stuck = 0;
         double worst = 0.0;

         for (int i = 0; i < c.total(); i++) {
            if (c.contact(i)) {
               contacts++;
            }

            if (c.stuck(i)) {
               stuck++;
            }

            worst = Math.max(worst, c.offset(i, 1.0F).length());
         }

         out.put(
            st.key,
            new ChainStatus(
               st.muscle,
               c.flowStiffness(),
               c.flowShare(),
               contacts,
               stuck,
               worst,
               c.anchored(),
               st.baseDeviation,
               st.baseCap,
               st.worstBend,
               st.inside,
               st.stepMicros,
               st.frameMicros
            )
         );
      }

      return out;
   }

   static void log(int entityId, int ticks) {
      logEntity = entityId;
      logTicks = ticks;
   }

   @Override
   public String name() {
      return "chain";
   }

   @Override
   public void solve(RigFrame frame) {
      ChainForce force = ChainForceSwitch.get();
      if (force != ChainForce.OFF) {
         Entity self = frame.body.animEntity();
         if (self != null) {
            FaunaBase body = self instanceof FaunaBase fauna ? fauna : null;
            RigLod.Rung rung = RigLod.of(self);
            if (!rung.chainActive()) {
               forget(self.getId());
            } else {
               BodyPlan plan = body == null ? null : body.bodyPlan();
               if (!frame.spec.physics().isEmpty()) {
                  ChainPass.Chains chains = CHAINS.computeIfAbsent(self.getId(), id -> new ChainPass.Chains());
                  if (chains.model != frame.model || chains.spec != frame.spec || chains.plan != plan) {
                     resolve(chains, frame.model, frame.spec, plan);
                  }

                  Matrix4f toWorld = frame.modelToWorld;
                  chains.toModel.set(toWorld).invert();
                  double scale = renderScale(toWorld);

                  for (ChainPass.ChainState state : chains.chains) {
                     this.solveChain(frame, self, body, plan, state, force, rung.chainCadence(), scale, chains.toModel);
                  }
               }
            }
         }
      }
   }

   private static double waterTopSpeed(FaunaBase body) {
      FaunaStatSheet stats = body.getStatSheet();
      return stats == null ? 1.0 : Math.max(0.001, WaterEngine.topSpeed(body, stats));
   }

   static double submerged(Level level, Vec3 centre, double r) {
      BlockPos at = BlockPos.containing(centre);
      FluidState here = level.getFluidState(at);
      double surface;
      if (here.is(FluidTags.WATER)) {
         surface = level.getFluidState(at.above()).is(FluidTags.WATER) ? at.getY() + 2.0 : at.getY() + here.getHeight(level, at);
      } else {
         BlockPos below = at.below();
         FluidState under = level.getFluidState(below);
         if (!under.is(FluidTags.WATER)) {
            return 0.0;
         }

         surface = below.getY() + under.getHeight(level, below);
      }

      double radius = Math.max(0.001, r);
      return Math.max(0.0, Math.min(1.0, (surface - (centre.y - radius)) / (2.0 * radius)));
   }

   private static void resolve(ChainPass.Chains chains, AzBakedModel model, RigSpec spec, BodyPlan plan) {
      List<ChainPass.ChainState> out = new ArrayList<>();

      for (Entry<String, PointChain.Params> entry : spec.physics().entrySet()) {
         String name = entry.getKey();
         List<String> names = new ArrayList<>(plan == null ? spec.chain(name) : plan.chain(name));
         if (entry.getValue().headLed()) {
            names.addAll(plan == null ? spec.chain("head") : plan.chain("head"));
         }

         if (!names.isEmpty() && (names.size() >= 2 || !entry.getValue().headLed())) {
            for (String prefix : plan == null ? spec.prefixes() : plan.prefixes()) {
               AzBone[] bones = new AzBone[names.size()];
               boolean whole = true;

               for (int i = 0; i < bones.length; i++) {
                  bones[i] = model.getBoneOrNull(prefix + names.get(i));
                  if (bones[i] == null) {
                     whole = false;
                     break;
                  }
               }

               if (whole) {
                  Vector3f reach = bones.length == 1 ? CubeMesh.farEnd(bones[0]) : null;
                  if (bones.length != 1 || reach != null) {
                     List<AzBone> ancestors = BoneFk.ancestorsTopDown(bones[bones.length - 1], model);
                     int[] anc = ancestorIndex(ancestors, bones);
                     ChainPass.ChainState kept = null;

                     for (ChainPass.ChainState old : chains.chains) {
                        if (old.key.equals(prefix + name) && old.bones.length == bones.length) {
                           kept = old;
                           break;
                        }
                     }

                     ChainPass.ChainState state = kept;
                     if (state != null) {
                        state.rebind(List.copyOf(names), bones, ancestors, anc, entry.getValue());
                     } else {
                        state = new ChainPass.ChainState(prefix, name, List.copyOf(names), bones, ancestors, anc, entry.getValue());
                     }

                     state.tipRest = reach;
                     out.add(state);
                  }
               }
            }
         }
      }

      chains.chains = out.toArray(new ChainPass.ChainState[0]);
      chains.model = model;
      chains.spec = spec;
      chains.plan = plan;
   }

   private void solveChain(
      RigFrame frame, Entity self, FaunaBase body, BodyPlan plan, ChainPass.ChainState state, ChainForce force, int cadence, double scale, Matrix4f toModel
   ) {
      boolean asleep = body != null && body.asleep();
      AzBakedModel model = frame.model;
      AzBone[] bones = state.bones;
      List<String> names = state.names;
      String prefix = state.prefix;
      String name = state.name;
      List<AzBone> ancestors = state.ancestors;
      int[] anc = state.anc;
      Vec3[] animated = state.animated;
      Matrix4f[] parents = state.parents;
      long fkClock = CLOCK_FK.start();
      fk(model, bones, animated, parents);
      CLOCK_FK.end(fkClock);
      Vec3 loneTip = state.tipRest == null ? null : loneTip(state, parents[0], bones[0]);
      Matrix4f toWorld = frame.modelToWorld;
      PointChain chain = state.chain;
      if (state.scaledFor != scale) {
         state.scaled = state.authored.scaled(scale);
         state.scaledFor = scale;
      }

      PointChain.Params params = state.scaled;
      long passStart = System.nanoTime();
      int stale = 2 * RigLod.Rung.QUARTER.chainCadence();
      if (body != null && state.rodeTick != self.tickCount && state.steppedTick != Integer.MIN_VALUE && self.tickCount - state.steppedTick <= stale) {
         state.rodeTick = self.tickCount;
         UnaryOperator<Vec3> ground = body.groundRide();
         if (ground != null) {
            UnaryOperator<Vec3> before = state.ride;
            state.ride = before == null ? ground : p -> ground.apply(before.apply(p));
         }
      }

      boolean due = RigLod.due(self.tickCount, cadence, self.getId());
      if (state.steppedTick != self.tickCount && due && force != ChainForce.FREEZE) {
         if (state.ride != null) {
            chain.ride(state.ride);
            if (state.worldAtStep != null) {
               for (int i = 0; i < state.worldAtStep.length; i++) {
                  state.worldAtStep[i] = state.ride.apply(state.worldAtStep[i]);
               }
            }

            state.ride = null;
         }

         long stepStart = System.nanoTime();
         Vec3[] world = new Vec3[bones.length];

         for (int i = 0; i < bones.length; i++) {
            world[i] = toWorld(toWorld, frame.drawnPosition, animated[i]);
         }

         int sinceStep = state.steppedTick == Integer.MIN_VALUE ? 1 : self.tickCount - state.steppedTick;
         state.chain.spans(Math.min(sinceStep, stale), sinceStep > stale);
         state.steppedTick = self.tickCount;
         state.prevStepTime = state.stepTime;
         state.stepTime = self.tickCount + frame.partialTick;
         boolean slack = asleep || force == ChainForce.SLACK;
         double want = slack ? params.asleepFactor() : 1.0;
         state.muscle = state.muscle + (want - state.muscle) * 0.1;
         state.asleep = state.asleep + ((asleep ? 1.0 : 0.0) - state.asleep) * 0.1;

         double muscle = switch (force) {
            case ROPE -> 0.0;
            case STIFF -> 1000000.0;
            default -> state.muscle;
         };
         final double[] radii = new double[chain.total()];

         for (int i = 0; i < chain.total(); i++) {
            radii[i] = i >= bones.length ? params.radiusTip() : params.radiusAt(i, bones.length);
            if (i < bones.length && plan != null) {
               for (PartSpec part : plan.parts()) {
                  if (part.bone().equals(names.get(i))) {
                     radii[i] = part.width() * 0.5 * scale;
                     break;
                  }
               }
            }
         }

         double window = 0.0;

         for (int i = 1; i < bones.length; i++) {
            window = Math.max(window, world[i].distanceTo(world[i - 1]));
         }

         double widest = 0.0;

         for (double radius : radii) {
            widest = Math.max(widest, radius);
         }

         AABB span = new AABB(world[0], world[0]);

         for (Vec3 at : world) {
            span = span.minmax(new AABB(at, at));
         }

         if (chain.primed()) {
            for (int i = 0; i < chain.total(); i++) {
               Vec3 at = chain.worldPoint(i);
               span = span.minmax(new AABB(at, at));
            }
         }

         state.sampler.begin(self, span.inflate(window + 2.0 * widest + 0.75 + 1.0));
         if (self.getY() != state.contextY || self.isDescending() != state.contextDescending) {
            state.gathers.invalidateContextual();
            state.liftGathers.invalidateContextual();
            state.contextY = self.getY();
            state.contextDescending = self.isDescending();
         }

         state.liftPrev = state.lift;
         if (params.headLed() && bones.length >= 2 && body != null) {
            double needed = 0.0;
            double speed = Math.hypot(self.getX() - self.xo, self.getZ() - self.zo);
            double moving = body.isFlying() ? 0.0 : Math.min(1.0, speed / 0.1);
            Vec3 axis = TargetLift.axis(world);
            if (moving > 0.0 && axis != null) {
               double reach = 0.0;

               for (int i = 1; i < bones.length; i++) {
                  reach += world[i].distanceTo(world[i - 1]);
               }

               Vec3 flatLead = new Vec3(self.getX() - self.xo, 0.0, self.getZ() - self.zo).scale(8.0);
               Vec3 lead = flatLead.add(0.0, -flatLead.horizontalDistance() * Math.tan(AttitudePass.terrainPitchRadians(body)), 0.0);
               AABB region = new AABB(world[0], world[0]).inflate(reach + radii[0] + 0.1).expandTowards(lead);
               List<AABB> boxes = state.liftBoxes;
               boxes.clear();

               for (AABB box : state.liftGathers.boxes(0, region, state.source)) {
                  if (box.intersects(region)) {
                     boxes.add(box);
                  }
               }

               needed = moving * 0.75 * TargetLift.lift(world, radii, boxes, lead, Math.toRadians(90.0), 0.1);
            }

            state.lift = state.lift + (needed - state.lift) * (needed > state.lift ? 0.75 : 0.3);
            if (state.lift > 1.0E-6 && axis != null) {
               world = TargetLift.arced(world, axis, state.lift);
            }
         }

         double[] support = new double[chain.total()];
         long probeClock = CLOCK_PROBE.start();

         for (int i = 0; i < chain.total(); i++) {
            Vec3 at = chain.primed() ? chain.worldPoint(i) : world[Math.min(i, bones.length - 1)];
            support[i] = LevelGroundOracle.INSTANCE.chainSupport(self, at.x, at.z, at.y, 1.0);
            if (FaunaLayers.drawing(FaunaLayers.PARTS) && !Double.isNaN(support[i])) {
               LegReachDebugRenderer.cross(self.getId(), at.x, support[i], at.z, 0.1F, 0.3F, 0.85F, 1.0F);
            }
         }

         CLOCK_PROBE.end(probeClock);
         Vec3[] carried = null;
         if (params.headLed()) {
            if (state.worldAtStep != null && state.worldAtStep.length == bones.length) {
               carried = state.worldAtStep;
            }
         } else if (state.animatedAtStep != null && state.animatedAtStep.length == bones.length) {
            carried = new Vec3[bones.length];

            for (int i = 0; i < bones.length; i++) {
               carried[i] = toWorld(toWorld, frame.drawnPosition, state.animatedAtStep[i]);
            }
         }

         state.animatedAtStep = (Vec3[])animated.clone();
         state.worldAtStep = (Vec3[])world.clone();
         boolean grounded = body == null ? self.onGround() : body.getTransportMode() == TransportMode.GROUNDED || body.isClimbing();
         double bodySpeed = grounded
            ? 0.0
            : Math.sqrt(
               (self.getX() - self.xo) * (self.getX() - self.xo)
                  + (self.getY() - self.yo) * (self.getY() - self.yo)
                  + (self.getZ() - self.zo) * (self.getZ() - self.zo)
            );
         double fullSpeed = params.flowSpeed() > 0.0
            ? params.flowSpeed()
            : (body == null ? Double.POSITIVE_INFINITY : Math.max(0.15F, body.getAttributeValue(Attributes.FLYING_SPEED)));
         final AABB bodyBox = !params.headLed() && body != null ? self.getBoundingBox().deflate(0.1) : null;
         final boolean[] bodyOwn = new boolean[chain.total()];
         if (bodyBox != null) {
            for (int i = 0; i < chain.total(); i++) {
               Vec3 at = world[Math.min(i, bones.length - 1)];
               bodyOwn[i] = Penetration.sideways(at, radii[i], bodyBox).lengthSqr() > 0.0;
            }
         }

         final GatherCache gathers = state.gathers;
         final GatherCache.Source blocks = state.source;
         PointChain.PushOut pushOut = new PointChain.PushOut() {
            @Override
            public Vec3 apply(Vec3 centre, double r) {
               return ChainPass.pushOut(blocks, centre, r, bodyBox, gathers, -1);
            }

            @Override
            public double depth(Vec3 centre, double r, int particle) {
               AABB query = Penetration.sphereQuery(centre, 2.0 * r);
               VoxelView grid = gathers.voxels(particle, query, blocks);
               double deep = grid != null
                  ? Penetration.signedDepth(centre, r, r, grid)
                  : Penetration.signedDepth(centre, r, r, ChainPass.boxes(blocks, gathers, particle, query));
               if (bodyBox != null && !bodyOwn[particle]) {
                  deep = Math.max(deep, Penetration.sideways(centre, r, bodyBox).length());
               }

               return deep;
            }

            @Override
            public void reach(Vec3 centre, double radius, int particle) {
               gathers.reach(particle, Penetration.sphereQuery(centre, radius + radii[particle] + 0.25).inflate(0.5), blocks);
            }

            @Override
            public boolean crosses(Vec3 a, Vec3 b, int particle) {
               AABB query = new AABB(a, b);
               VoxelView grid = gathers.voxels(particle, query, blocks);
               return grid != null
                  ? Penetration.segmentCrosses(a, b, grid)
                  : Penetration.segmentCrosses(a, b, ChainPass.boxes(blocks, gathers, particle, query));
            }

            @Override
            public boolean clear(Vec3 centre, double r, int particle) {
               AABB query = Penetration.sphereQuery(centre, 2.0 * r);
               VoxelView grid = gathers.voxels(particle, query, blocks);
               return (grid != null ? Penetration.clear(centre, r, grid) : Penetration.clear(centre, r, ChainPass.boxes(blocks, gathers, particle, query)))
                  ? bodyBox == null || bodyOwn[particle] || Penetration.sideways(centre, r, bodyBox).length() <= 0.0
                  : false;
            }
         };
         int reprimes = chain.reprimes();
         boolean aquatic = body != null && body.isAquatic();
         double flow = bodySpeed / (aquatic ? waterTopSpeed(body) : fullSpeed);
         PointChain.Water water = aquatic ? frame.spec.chainWater().getOrDefault(name, PointChain.Water.DEFAULT) : null;
         Vec3[] targets = world;
         Vec3[] carriedTargets = carried;
         if (loneTip != null) {
            targets = new Vec3[]{world[0], toWorld(toWorld, frame.drawnPosition, loneTip)};
            carriedTargets = carried != null && state.tipAtStep != null ? new Vec3[]{carried[0], toWorld(toWorld, frame.drawnPosition, state.tipAtStep)} : null;
            state.tipAtStep = loneTip;
         }

         chain.step(
            targets, carriedTargets, support, pushOut, params, muscle, flow, radii, water, aquatic ? (centre, r) -> submerged(self.level(), centre, r) : null
         );
         if (chain.reprimes() != reprimes && chain.lastReprime() >= 0.0) {
            LOGGER.debug(
               "[ChainPass] {} {} re-seated on its clip at tick {}: the root jumped {} blocks in one step (vehicle {})",
               new Object[]{self.getName().getString(), state.key, self.tickCount, String.format(Locale.ROOT, "%.2f", chain.lastReprime()), self.isVehicle()}
            );
         }

         state.stepMicros = state.stepMicros + ((System.nanoTime() - stepStart) / 1000.0 - state.stepMicros) * 0.03333333333333333;
         CLOCK_STEP.end(stepStart);
         if (ChainWatch.watching(self.getId())) {
            double[] inside = new double[chain.total()];

            for (int i = 0; i < chain.total(); i++) {
               Vec3 at = chain.worldPoint(i);
               double r = chain.radius(i);
               inside[i] = Penetration.sphereDepth(at, r, boxes(blocks, gathers, i, Penetration.sphereQuery(at, r)));
            }

            double clipMax = 0.0;
            if (carried != null) {
               for (int i = 0; i < bones.length; i++) {
                  clipMax = Math.max(clipMax, carried[i].distanceTo(world[i]));
               }
            }

            ChainWatch.sample(self.tickCount, state.key, chain, params, bones.length, inside, clipMax, state.stepMicros, RigLod.of(self).name());
         }

         state.baseDeviation = chain.baseDeviation();
         state.baseCap = params.bendAt(Math.min(1, bones.length - 1), bones.length);
         state.worstBend = chain.worstBend();
         AABB box = self.getBoundingBox();
         int inside = 0;

         for (int i = 1; i < chain.total(); i++) {
            if (box.contains(chain.worldPoint(i))) {
               inside++;
            }
         }

         state.inside = inside;
         if (logEntity == self.getId() && logTicks > 0) {
            StringBuilder flags = new StringBuilder();

            for (int i = 0; i < chain.total(); i++) {
               flags.append(i).append(chain.stuck(i) ? "S" : (chain.contact(i) ? "c" : ".")).append(box.contains(chain.worldPoint(i)) ? "!" : "").append(' ');
            }

            double tickSpeed = Math.sqrt(
               (self.getX() - self.xo) * (self.getX() - self.xo)
                  + (self.getY() - self.yo) * (self.getY() - self.yo)
                  + (self.getZ() - self.zo) * (self.getZ() - self.zo)
            );
            double clipMoved = 0.0;
            if (carried != null) {
               for (int i = 0; i < bones.length; i++) {
                  clipMoved = Math.max(clipMoved, carried[i].distanceTo(world[i]));
               }
            }

            StringBuilder lift = new StringBuilder();

            for (int i = 0; i < chain.total(); i++) {
               lift.append(String.format(Locale.ROOT, "%+.2f ", chain.offset(i, 1.0F).y));
            }

            LOGGER.debug(
               "[ChainLog] {} {} t={} transport={} yBodyRot={} body {} b/t clip moved {} b | {} | particles {}| lift y {}",
               new Object[]{
                  self.getName().getString(),
                  prefix + name,
                  self.tickCount,
                  body == null ? "-" : body.getTransportMode(),
                  String.format(Locale.ROOT, "%.1f", body == null ? self.getYRot() : body.yBodyRot),
                  String.format(Locale.ROOT, "%.3f", tickSpeed),
                  String.format(Locale.ROOT, "%.3f", clipMoved),
                  new ChainStatus(
                        state.muscle,
                        chain.flowStiffness(),
                        chain.flowShare(),
                        0,
                        0,
                        0.0,
                        chain.anchored(),
                        state.baseDeviation,
                        state.baseCap,
                        state.worstBend,
                        inside,
                        state.stepMicros,
                        state.frameMicros
                     )
                     .line(),
                  flags,
                  lift
               }
            );
            if (logTick != self.tickCount) {
               logTick = self.tickCount;
               logTicks--;
            }
         }
      }

      float blend = 1.0F;
      if (!Double.isNaN(state.prevStepTime) && state.stepTime > state.prevStepTime) {
         double now = self.tickCount + frame.partialTick;
         blend = (float)Math.max(0.0, Math.min(1.0, (now - state.stepTime) / (state.stepTime - state.prevStepTime)));
      }

      if (params.headLed() && (state.lift > 1.0E-6 || state.liftPrev > 1.0E-6)) {
         arcPose(animated, Mth.lerp(blend, state.liftPrev, state.lift), toWorld, toModel, frame.drawnPosition);
      }

      Vec3[] corrected = state.corrected;
      Vector3f off = JOINT;
      double drawnNow = self.tickCount + frame.partialTick;
      double easeTicks = 8.0 * (state.asleep < 0.001 ? 0.0 : state.asleep);
      float ease = !(easeTicks <= 0.0) && !Double.isNaN(state.drawnTime) && state.drawn != null
         ? (float)(1.0 - Math.exp(-Math.max(0.0, drawnNow - state.drawnTime) / easeTicks))
         : 1.0F;
      if (state.drawn == null) {
         state.drawn = new Vector3f[bones.length + 1];

         for (int i = 0; i < state.drawn.length; i++) {
            state.drawn[i] = new Vector3f();
         }
      }

      state.drawnTime = drawnNow;

      for (int i = 0; i < bones.length; i++) {
         chain.offset(i, blend, off);
         if (ease >= 1.0F) {
            state.drawn[i].set(off);
         } else {
            off.set(state.drawn[i].lerp(off, ease));
         }

         toModel.transformDirection(off);
         Vec3 a = animated[i];
         corrected[i] = new Vec3(a.x + off.x, a.y + off.y, a.z + off.z);
      }

      Vec3 last = animated[bones.length - 1];
      double tipModel;
      Vec3 tipAnimated;
      if (loneTip != null) {
         tipAnimated = loneTip;
         tipModel = loneTip.distanceTo(last);
      } else {
         Vec3 dir = last.subtract(animated[bones.length - 2]);
         double len = dir.length();
         tipModel = (params.tip() > 0.0 ? params.tip() : len * scale) / Math.max(scale, 1.0E-6);
         tipAnimated = len < 1.0E-9 ? last : last.add(dir.scale(tipModel / len));
      }

      chain.offset(bones.length, blend, off);
      if (ease >= 1.0F) {
         state.drawn[bones.length].set(off);
      } else {
         off.set(state.drawn[bones.length].lerp(off, ease));
      }

      toModel.transformDirection(off);
      corrected[bones.length] = new Vec3(tipAnimated.x + off.x, tipAnimated.y + off.y, tipAnimated.z + off.z);
      if (FaunaLayers.drawing(FaunaLayers.PARTS)) {
         drawParticles(self.getId(), state, params, bones.length);
      }

      ChainWatch.drawTrails(self.getId());
      Vec3[] current = state.current;
      boolean headLocked = params.headLed() && chain.anchored();
      int lastAimed = headLocked ? bones.length - 2 : bones.length - 1;
      AzBone headBone = bones[bones.length - 1];
      Quaternionf headAnimWorld = headLocked ? new Quaternionf(BoneFk.orthonormalRotation(parents[bones.length - 1])).mul(BoneFk.localQuat(headBone)) : null;
      if (headLocked && body != null) {
         double bodyRoll = AttitudePass.rollRadians(body);
         if (Math.abs(bodyRoll) > 1.0E-4) {
            Footing foot = frame.footing();
            Quaternionf unroll = Attitude.modelRotation(frame.modelToStand(), Mth.rotLerp(frame.partialTick, foot.yawO(), foot.yaw()), 0.0, bodyRoll)
               .conjugate();
            headAnimWorld = unroll.mul(headAnimWorld);
         }
      }

      long aimClock = CLOCK_AIM.start();

      for (int i = 0; i <= lastAimed; i++) {
         fkFrom(ancestors, anc, bones, Math.max(0, i - 1), Math.min(i + 1, bones.length - 1), current, parents);
         Vec3 childNow;
         if (i + 1 < bones.length) {
            childNow = current[i + 1];
         } else if (loneTip != null) {
            childNow = loneTip(state, parents[0], bones[0]);
         } else {
            Vec3 lastNow = current[i].subtract(current[i - 1]);
            double lenNow = lastNow.length();
            childNow = lenNow < 1.0E-9 ? current[i].add(0.0, 0.0, tipModel) : current[i].add(lastNow.scale(tipModel / lenNow));
         }

         aim(bones[i], parents[i], current[i], childNow, corrected[i + 1]);
         capture(frame, bones[i].getName());
      }

      if (headLocked) {
         fkFrom(ancestors, anc, bones, Math.max(0, bones.length - 2), bones.length - 1, current, parents);
         BoneFk.lockWorldOrientation(headBone, parents[bones.length - 1], headAnimWorld);
         capture(frame, headBone.getName());
      }

      CLOCK_AIM.end(aimClock);
      state.frameMicros = state.frameMicros + ((System.nanoTime() - passStart) / 1000.0 - state.frameMicros) * 0.03333333333333333;
   }

   private static Vec3 loneTip(ChainPass.ChainState state, Matrix4f parent, AzBone bone) {
      Vector3f tip = BoneFk.mulLocal(parent, bone, LONE).transformPosition(state.tipRest, new Vector3f());
      return new Vec3(tip.x(), tip.y(), tip.z());
   }

   private static void aim(AzBone bone, Matrix4f parent, Vec3 joint, Vec3 childNow, Vec3 childTarget) {
      float jx = (float)joint.x;
      float jy = (float)joint.y;
      float jz = (float)joint.z;
      float cx = (float)childNow.x;
      float cy = (float)childNow.y;
      float cz = (float)childNow.z;
      float tx = (float)childTarget.x;
      float ty = (float)childTarget.y;
      float tz = (float)childTarget.z;
      Vector3f a = AIM_A.set(cx - jx, cy - jy, cz - jz);
      Vector3f b = AIM_B.set(tx - jx, ty - jy, tz - jz);
      if (!(a.lengthSquared() < 1.0E-9F) && !(b.lengthSquared() < 1.0E-9F)) {
         if (!(a.normalize().dot(b.normalize()) < -0.94F)) {
            BoneFk.aimBone(bone, parent, jx, jy, jz, cx, cy, cz, tx, ty, tz);
         }
      }
   }

   private static Vec3 pushOut(GatherCache.Source world, Vec3 centre, double r, AABB bodyBox, GatherCache gathers, int particle) {
      Penetration.Push push = Penetration.sphere(null, centre, r, 0, query -> boxes(world, gathers, particle, query));
      Vec3 out = push.push();
      if (bodyBox != null) {
         out = out.add(Penetration.sideways(centre.add(out), r, bodyBox));
      }

      return out;
   }

   private static List<AABB> boxes(GatherCache.Source world, GatherCache gathers, int particle, AABB query) {
      return particle < 0 ? world.gather(GatherCache.view(query)) : gathers.boxes(particle, query, world);
   }

   private static void drawParticles(int id, ChainPass.ChainState st, PointChain.Params params, int bones) {
      PointChain chain = st.chain;

      for (int i = 1; i < chain.total(); i++) {
         Vec3 a = chain.worldPoint(i - 1);
         Vec3 b = chain.worldPoint(i);
         LegReachDebugRenderer.line(id, a.x, a.y, a.z, b.x, b.y, b.z, 0.2F, 1.0F, 0.2F);
      }

      if (st.worldAtStep != null) {
         Vec3[] w = st.worldAtStep;

         for (int i = 1; i < w.length; i++) {
            LegReachDebugRenderer.line(
               id, w[i - 1].x, w[i - 1].y, w[i - 1].z, w[i].x, w[i].y, w[i].z, 0.5F, 0.5F, 0.5F
            );
         }

         if (w.length >= 2) {
            Vec3 dir = w[1].subtract(w[0]);
            if (dir.lengthSqr() > 1.0E-9) {
               Vec3 tip = w[0].add(dir.normalize().scale(2.0));
               LegReachDebugRenderer.line(id, w[0].x, w[0].y, w[0].z, tip.x, tip.y, tip.z, 1.0F, 0.2F, 1.0F);
            }
         }
      }

      for (int i = 0; i < chain.total(); i++) {
         Vec3 p = chain.worldPoint(i);
         double r = chain.radius(i);
         boolean tip = i >= bones;
         boolean head = params.headLed() && i == bones - 1 && chain.anchored();
         float red = chain.contact(i) ? 1.0F : 0.2F;
         float green = chain.contact(i) ? 0.6F : 1.0F;
         float blue = 0.2F;
         if (head) {
            red = 1.0F;
            green = 1.0F;
            blue = 0.3F;
         }

         LegReachDebugRenderer.sphere(id, p.x, p.y, p.z, tip ? r * 0.5 : r, red, green, blue);
         if (head) {
            LegReachDebugRenderer.sphere(id, p.x, p.y, p.z, r * 1.3, red, green, blue);
         }
      }
   }

   private static List<AABB> gather(BlockSampler sampler, AABB region) {
      long gatherClock = CLOCK_GATHER.start();
      List<AABB> boxes = sampler.gather(region);
      CLOCK_GATHER.end(gatherClock);
      return boxes;
   }

   private static double renderScale(Matrix4f m) {
      double s = Math.sqrt((double)m.m00() * m.m00() + (double)m.m01() * m.m01() + (double)m.m02() * m.m02());
      return s < 1.0E-6 ? 1.0 : s;
   }

   private static Vec3 toWorld(Matrix4f toWorld, Vec3 drawn, Vec3 model) {
      Vector3f v = toWorld.transformPosition(toF(model));
      return new Vec3(v.x() + drawn.x, v.y() + drawn.y, v.z() + drawn.z);
   }

   private static Vec3 toModel(Matrix4f toModel, Vec3 drawn, Vec3 world) {
      Vector3f v = toModel.transformPosition(
         new Vector3f((float)(world.x - drawn.x), (float)(world.y - drawn.y), (float)(world.z - drawn.z))
      );
      return new Vec3(v.x(), v.y(), v.z());
   }

   private static void arcPose(Vec3[] animated, double lift, Matrix4f toWorld, Matrix4f toModel, Vec3 drawn) {
      Vec3[] world = new Vec3[animated.length];

      for (int i = 0; i < animated.length; i++) {
         world[i] = toWorld(toWorld, drawn, animated[i]);
      }

      Vec3 axis = TargetLift.axis(world);
      if (axis != null) {
         Vec3[] lifted = TargetLift.arced(world, axis, lift);

         for (int i = 0; i < animated.length; i++) {
            animated[i] = toModel(toModel, drawn, lifted[i]);
         }
      }
   }

   static int[] ancestorIndex(List<AzBone> ancestors, AzBone[] bones) {
      int[] anc = new int[bones.length];
      Arrays.fill(anc, -1);
      int next = 0;

      for (int a = 0; a < ancestors.size() && next < bones.length; a++) {
         if (ancestors.get(a).getName().equals(bones[next].getName())) {
            anc[next++] = a;
         }
      }

      return anc;
   }

   static void fkFrom(List<AzBone> ancestors, int[] anc, AzBone[] bones, int from, int to, Vec3[] targets, Matrix4f[] parents) {
      if (anc[from] >= 0 && parents[from] != null) {
         Matrix4f accum = ACCUM.set(parents[from]);
         Vector3f joint = JOINT;
         int k = from;

         for (int a = anc[from]; a < ancestors.size() && k <= to; a++) {
            AzBone b = ancestors.get(a);
            if (a == anc[k]) {
               accum.transformPosition(BoneFk.offsetPoint(b, joint));
               targets[k] = new Vec3(joint.x(), joint.y(), joint.z());
               if (parents[k] == null) {
                  parents[k] = new Matrix4f(accum);
               } else {
                  parents[k].set(accum);
               }

               k++;
            }

            BoneFk.mulLocal(accum, b);
         }
      }
   }

   static void fk(AzBakedModel model, AzBone[] bones, Vec3[] targets, Matrix4f[] parents) {
      List<AzBone> chain = BoneFk.ancestorsTopDown(bones[bones.length - 1], model);
      Matrix4f accum = ACCUM.identity();
      Vector3f joint = JOINT;
      int next = 0;

      for (AzBone b : chain) {
         if (next < bones.length && b.getName().equals(bones[next].getName())) {
            accum.transformPosition(BoneFk.offsetPoint(b, joint));
            targets[next] = new Vec3(joint.x(), joint.y(), joint.z());
            if (parents[next] == null) {
               parents[next] = new Matrix4f(accum);
            } else {
               parents[next].set(accum);
            }

            next++;
         }

         BoneFk.mulLocal(accum, b);
      }

      for (int i = next; i < bones.length; i++) {
         if (targets[i] == null) {
            targets[i] = Vec3.ZERO;
         }

         if (parents[i] == null) {
            parents[i] = new Matrix4f();
         }
      }
   }

   private static Vector3f toF(Vec3 v) {
      return new Vector3f((float)v.x, (float)v.y, (float)v.z);
   }

   private static void capture(RigFrame frame, String bone) {
      if (frame.write.has(bone)) {
         frame.write.correct(bone, BoneAxis.ROT_X);
         frame.write.correct(bone, BoneAxis.ROT_Y);
         frame.write.correct(bone, BoneAxis.ROT_Z);
      }
   }

   private static final class ChainState {
      final PointChain chain;
      int steppedTick = Integer.MIN_VALUE;
      UnaryOperator<Vec3> ride;
      int rodeTick = Integer.MIN_VALUE;
      double stepTime = Double.NaN;
      double prevStepTime = Double.NaN;
      double muscle = 1.0;
      double asleep;
      Vector3f[] drawn;
      double drawnTime = Double.NaN;
      Vec3[] animatedAtStep;
      Vector3f tipRest;
      Vec3 tipAtStep;
      final GatherCache gathers;
      final BlockSampler sampler = new BlockSampler();
      final GatherCache.Source source = new GatherCache.Source() {
         @Override
         public List<AABB> gather(AABB region) {
            return ChainPass.gather(ChainState.this.sampler, region);
         }

         @Override
         public long mark() {
            return BlockChanges.mark();
         }

         @Override
         public boolean changedSince(AABB region, long mark) {
            return BlockChanges.changedSince(
               mark,
               Mth.floor(region.minX - 1.0E-7) - 1,
               Mth.floor(region.minY - 1.0E-7) - 1,
               Mth.floor(region.minZ - 1.0E-7) - 1,
               Mth.floor(region.maxX + 1.0E-7) + 1,
               Mth.floor(region.maxY + 1.0E-7) + 1,
               Mth.floor(region.maxZ + 1.0E-7) + 1
            );
         }

         @Override
         public boolean lastGatherVolatile() {
            return ChainState.this.sampler.lastGatherVolatile();
         }

         @Override
         public boolean lastGatherContextual() {
            return ChainState.this.sampler.lastGatherContextual();
         }
      };
      final GatherCache liftGathers = new GatherCache(1);
      final List<AABB> liftBoxes = new ArrayList<>();
      double contextY = Double.NaN;
      boolean contextDescending;
      double baseDeviation;
      double baseCap;
      double worstBend;
      int inside;
      Vec3[] worldAtStep;
      double stepMicros;
      double frameMicros;
      final String key;
      final String prefix;
      final String name;
      List<String> names;
      AzBone[] bones;
      List<AzBone> ancestors;
      int[] anc;
      PointChain.Params authored;
      PointChain.Params scaled;
      double scaledFor = Double.NaN;
      double lift;
      double liftPrev;
      final Vec3[] animated;
      final Vec3[] corrected;
      final Vec3[] current;
      final Matrix4f[] parents;

      ChainState(String prefix, String name, List<String> names, AzBone[] bones, List<AzBone> ancestors, int[] anc, PointChain.Params authored) {
         int count = bones.length;
         this.chain = new PointChain(count);
         this.gathers = new GatherCache(count + 1);
         this.key = prefix + name;
         this.prefix = prefix;
         this.name = name;
         this.names = names;
         this.bones = bones;
         this.ancestors = ancestors;
         this.anc = anc;
         this.authored = authored;
         this.animated = new Vec3[count];
         this.corrected = new Vec3[count + 1];
         this.current = new Vec3[count];
         this.parents = new Matrix4f[count];
      }

      void rebind(List<String> names, AzBone[] bones, List<AzBone> ancestors, int[] anc, PointChain.Params authored) {
         this.names = names;
         this.bones = bones;
         this.ancestors = ancestors;
         this.anc = anc;
         this.authored = authored;
         this.scaledFor = Double.NaN;
         Arrays.fill(this.parents, null);
      }
   }

   private static final class Chains {
      AzBakedModel model;
      RigSpec spec;
      BodyPlan plan;
      ChainPass.ChainState[] chains = new ChainPass.ChainState[0];
      final Matrix4f toModel = new Matrix4f();
   }
}
