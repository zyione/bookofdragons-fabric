package net.magister.liberanimalium.render;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import mod.azure.azurelib.animation.primitive.AzBakedAnimation;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.model.AzBoneSnapshot;
import net.magister.liberanimalium.anim.rig.BoneAxis;
import net.magister.liberanimalium.anim.rig.BoneFk;
import net.magister.liberanimalium.anim.rig.Ease;
import net.magister.liberanimalium.anim.rig.LimbDef;
import net.magister.liberanimalium.anim.rig.RigFrame;
import net.magister.liberanimalium.anim.rig.RigPass;
import net.magister.liberanimalium.anim.rig.RigSpec;
import net.magister.liberanimalium.anim.rig.SwimRoles;
import net.magister.liberanimalium.anim.rig.SwimSpec;
import net.magister.liberanimalium.anim.sample.ClipIndex;
import net.magister.liberanimalium.anim.sample.ClipSampler;
import net.magister.liberanimalium.body.anim.binding.AnimationBindingLoader;
import net.magister.liberanimalium.body.locomotion.WaterDepth;
import net.magister.liberanimalium.body.locomotion.WaterEngine;
import net.magister.liberanimalium.body.stats.FaunaStatSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class SwimPass implements RigPass {
   private static final double FADE_TICKS = 8.0;
   private static final double SMOOTH_TICKS = 6.0;
   private static final double MAX_STEP_TICKS = 4.0;
   private static final double SURFACE_DEPTH = 1.5;
   private static final double FLOAT_TICKS = 5.0;
   private static final int FLOAT_REACH = 4;
   private static final double NUDGE_MOVE = 1.0;
   private static final double NUDGE_TURN = 0.02;
   private static final double FULL_TURN = 2.0;
   private static final String STANCE = "swim_stance";
   private static final String STANCE_UNDER = "swim_stance_under";
   private static final Map<Integer, SwimPass.Stroke> STROKES = new ConcurrentHashMap<>();
   private static final BoneAxis[] ROT = new BoneAxis[]{BoneAxis.ROT_X, BoneAxis.ROT_Y, BoneAxis.ROT_Z};
   private static final BoneAxis[] CHANNELS = new BoneAxis[]{BoneAxis.ROT_X, BoneAxis.ROT_Y, BoneAxis.ROT_Z, BoneAxis.POS_X, BoneAxis.POS_Y, BoneAxis.POS_Z};

   static void forget(int entityId) {
      STROKES.remove(entityId);
   }

   @Override
   public String name() {
      return "swim";
   }

   @Override
   public void solve(RigFrame frame) {
      SwimSpec swim = frame.spec.swim();
      if (!swim.isEmpty() && frame.body instanceof FaunaBase body) {
         SwimPass.Stroke var38 = STROKES.computeIfAbsent(body.getId(), id -> new SwimPass.Stroke());
         double now = body.tickCount + frame.partialTick;
         double dt = Double.isNaN(var38.time) ? 0.0 : Mth.clamp(now - var38.time, 0.0, 4.0);
         var38.time = now;
         var38.weight = approach(var38.weight, body.isAquatic() ? 1.0 : 0.0, dt / 8.0);
         if (var38.weight <= 0.0) {
            giveBack(frame, swim, var38);
         } else {
            double ease = Ease.lag(dt, 6.0);
            var38.speed = var38.speed + (speedShare(body) - var38.speed) * ease;
            var38.turn = var38.turn + (Mth.wrapDegrees(body.getYRot() - body.yRotO) - var38.turn) * ease;
            double under = WaterDepth.depth(body) - body.getBbHeight();
            var38.surface = var38.surface + (1.0 - smoothstep(0.0, 1.5, under) - var38.surface) * ease;
            double share = Math.max(0.0, var38.speed);
            double rateShare = Math.min(1.0, share);
            var38.phase = (var38.phase + 360.0 * Mth.lerp(rateShare, swim.rate()[0], swim.rate()[1]) * dt / 20.0) % 360.0;
            double amplitude = Mth.lerp(Math.min(share, 1.5), swim.amplitude()[0], swim.amplitude()[1]);
            SwimSpec.Legs legs = swim.legs();
            double splay = 1.0 - smoothstep(0.0, legs.paddle(), share);
            double legWaves = amplitude * (1.0 - smoothstep(legs.tuck()[0], legs.tuck()[1], share));
            SwimSpec.Wings wings = swim.wings();
            double open = wings.open() * (1.0 - smoothstep(0.0, wings.fold(), share));
            double inside = wings.turn() * Math.min(1.0, Math.abs(var38.turn) / 2.0);
            double leftOpen = Mth.clamp(open + (var38.turn < 0.0 ? inside : 0.0), 0.0, 1.0);
            double rightOpen = Mth.clamp(open + (var38.turn > 0.0 ? inside : 0.0), 0.0, 1.0);
            stances(body, var38);
            SwimPass.Laid laid = laid(frame, body, swim, var38);
            SwimPass.Regime r = new SwimPass.Regime(amplitude, legWaves, splay, leftOpen, rightOpen);
            Set<String> posed = new HashSet<>();

            for (String bone : laid.top().keySet()) {
               if (posed.add(bone)) {
                  pose(frame, swim, laid, var38, r, bone);
               }
            }

            for (String bone : laid.deep().keySet()) {
               if (posed.add(bone)) {
                  pose(frame, swim, laid, var38, r, bone);
               }
            }

            for (String bone : var38.top.keySet()) {
               if (posed.add(bone)) {
                  pose(frame, swim, laid, var38, r, bone);
               }
            }

            for (String bone : var38.deep.keySet()) {
               if (posed.add(bone)) {
                  pose(frame, swim, laid, var38, r, bone);
               }
            }

            if (!swim.floats().isEmpty()) {
               floats(frame, body, swim.floats(), var38, dt, leftOpen, rightOpen);
            }
         }
      }
   }

   private static SwimPass.Laid laid(RigFrame frame, FaunaBase body, SwimSpec swim, SwimPass.Stroke s) {
      if (s.laid != null && s.laidModel == frame.model && s.laidSpec == frame.spec && s.laidTop == s.topClip && s.laidDeep == s.deepClip) {
         return s.laid;
      }

      Set<String> moved = new HashSet<>(s.top.keySet());
      moved.addAll(s.deep.keySet());
      Map<String, String> limbs = new LinkedHashMap<>();

      for (LimbDef limb : body.animSteps().limbs) {
         limbs.put(limb.name(), limb.root());
      }

      Map<String, String> parent = parents(frame.model);
      Map<String, double[]> pivot = new HashMap<>();

      for (String name : parent.keySet()) {
         AzBone b = frame.model.getBoneOrNull(name);
         if (b != null) {
            pivot.put(name, new double[]{b.getPivotX(), b.getPivotY(), b.getPivotZ()});
         }
      }

      SwimRoles.Shape shape = new SwimRoles.Shape(parent, moved, frame.spec.chains(), frame.spec.prefixes(), limbs, swim.roleRoots(), swim.still(), pivot);
      Map<String, SwimRoles.Place> places = SwimRoles.places(shape);
      Map<String, SwimSpec.Track> top = new LinkedHashMap<>(SwimRoles.tracks(swim.roles(), shape, places, swim.scale()));
      Map<String, SwimSpec.Track> deep = new LinkedHashMap<>(SwimRoles.tracks(swim.underRoles(), shape, places, swim.scale()));
      top.putAll(named(frame, swim.bones()));
      deep.putAll(named(frame, swim.under()));
      top = new LinkedHashMap<>(SwimRoles.scaled(top, swim.boneScale(), frame.spec.prefixes()));
      deep = new LinkedHashMap<>(SwimRoles.scaled(deep, swim.boneScale(), frame.spec.prefixes()));
      Set<String> legs = new HashSet<>();
      Set<String> left = new HashSet<>();
      Set<String> right = new HashSet<>();

      for (Entry<String, SwimRoles.Place> e : places.entrySet()) {
         SwimRoles.Place p = e.getValue();
         if (p.role().equals("front_legs") || p.role().equals("hind_legs")) {
            legs.add(e.getKey());
         }

         if (p.role().equals("wings")) {
            (p.side() < 0 ? right : left).add(e.getKey());
         }
      }

      if (!swim.legs().bones().isEmpty()) {
         legs = prefixed(frame, swim.legs().bones());
      }

      if (!swim.wings().left().isEmpty() || !swim.wings().right().isEmpty()) {
         left = prefixed(frame, swim.wings().left());
         right = prefixed(frame, swim.wings().right());
      }

      s.laid = new SwimPass.Laid(Map.copyOf(top), Map.copyOf(deep), Set.copyOf(legs), Set.copyOf(left), Set.copyOf(right));
      s.laidModel = frame.model;
      s.laidSpec = frame.spec;
      s.laidTop = s.topClip;
      s.laidDeep = s.deepClip;
      return s.laid;
   }

   private static Map<String, SwimSpec.Track> named(RigFrame frame, Map<String, SwimSpec.Track> bones) {
      Map<String, SwimSpec.Track> out = new LinkedHashMap<>();

      for (Entry<String, SwimSpec.Track> e : bones.entrySet()) {
         for (String prefix : frame.spec.prefixes()) {
            if (frame.model.getBoneOrNull(prefix + e.getKey()) != null) {
               out.putIfAbsent(prefix + e.getKey(), e.getValue());
            }
         }
      }

      return out;
   }

   private static Set<String> prefixed(RigFrame frame, Set<String> names) {
      Set<String> out = new HashSet<>();

      for (String name : names) {
         for (String prefix : frame.spec.prefixes()) {
            if (frame.model.getBoneOrNull(prefix + name) != null) {
               out.add(prefix + name);
            }
         }
      }

      return out;
   }

   private static Map<String, String> parents(AzBakedModel model) {
      Map<String, String> out = new LinkedHashMap<>();
      ArrayDeque<AzBone> todo = new ArrayDeque<>();

      for (AzBone top : model.getTopLevelBones()) {
         out.put(top.getName(), null);
         todo.push(top);
      }

      while (!todo.isEmpty()) {
         AzBone b = todo.pop();

         for (AzBone child : b.getChildBones()) {
            out.put(child.getName(), b.getName());
            todo.push(child);
         }
      }

      return out;
   }

   private static void pose(RigFrame frame, SwimSpec swim, SwimPass.Laid laid, SwimPass.Stroke s, SwimPass.Regime r, String bone) {
      AzBone az = frame.model.getBoneOrNull(bone);
      if (az != null) {
         SwimSpec.Legs legs = swim.legs();
         SwimSpec.Track top = laid.top().get(bone);
         SwimSpec.Track deep = laid.deep().get(bone);
         double[] held = s.top.get(bone);
         double[] heldDeep = s.deep.get(bone);
         double waves = laid.legs().contains(bone) ? r.legWaves() : r.amplitude();
         double toward = laid.left().contains(bone) ? r.leftOpen() : (laid.right().contains(bone) ? r.rightOpen() : 0.0);
         double[] spread = r.splay() > 0.0 ? legs.splay().get(bone) : null;
         AzBoneSnapshot rest = az.getInitialAzSnapshot();
         double[] target = new double[]{Double.NaN, Double.NaN, Double.NaN};
         double[] weight = new double[3];

         for (int i = 0; i < 3; i++) {
            SwimSpec.Wave a = axis(top, i, true);
            SwimSpec.Wave b = axis(deep, i, true);
            if (b == null && keyed(heldDeep, i)) {
               b = a;
            }

            boolean atTop = keyed(held, i) || a != null;
            boolean atDepth = keyed(heldDeep, i) || b != null;
            if (atTop || atDepth) {
               double sign = i < 2 ? -1.0 : 1.0;
               weight[i] = s.weight;
               double turn;
               if (atTop) {
                  double degrees = a == null ? 0.0 : a.at(s.phase, waves);
                  if (spread != null) {
                     degrees += spread[i] * r.splay();
                  }

                  turn = held(held, i) + Math.toRadians(degrees) * sign;
                  if (atDepth) {
                     double below = held(heldDeep, i) + Math.toRadians(b == null ? 0.0 : b.at(s.phase, waves)) * sign;
                     turn = Mth.lerp(s.surface, below, turn);
                  }
               } else {
                  turn = held(heldDeep, i) + Math.toRadians(b == null ? 0.0 : b.at(s.phase, waves)) * sign;
                  weight[i] *= 1.0 - s.surface;
               }

               target[i] = restRot(rest, i) + turn;
            }
         }

         if (toward > 0.0) {
            open(target, rest, toward);
         }

         for (int i = 0; i < 3; i++) {
            if (!Double.isNaN(target[i])) {
               blend(frame, s, bone, i, target[i], weight[i]);
            }
         }

         for (int i = 0; i < 3; i++) {
            SwimSpec.Wave a = axis(top, i, false);
            SwimSpec.Wave b = axis(deep, i, false);
            if (b == null && keyed(heldDeep, 3 + i)) {
               b = a;
            }

            boolean atTop = keyed(held, 3 + i) || a != null;
            boolean atDepth = keyed(heldDeep, 3 + i) || b != null;
            if (atTop || atDepth) {
               double up = held(held, 3 + i) + (a == null ? 0.0 : a.at(s.phase, waves));
               double down = held(heldDeep, 3 + i) + (b == null ? 0.0 : b.at(s.phase, waves));
               double posWeight = atTop ? s.weight : s.weight * (1.0 - s.surface);
               blend(frame, s, bone, 3 + i, atTop ? Mth.lerp(s.surface, down, up) : down, posWeight);
            }
         }
      }
   }

   private static boolean keyed(double[] held, int c) {
      return held != null && !Double.isNaN(held[c]);
   }

   private static double held(double[] held, int c) {
      return keyed(held, c) ? held[c] : 0.0;
   }

   private static void stances(FaunaBase body, SwimPass.Stroke s) {
      if (body.getAnimatorOrNull() instanceof AzEntityAnimator<?> any) {
         AzEntityAnimator var7 = any;
         String prefix = AnimationBindingLoader.get(body.animBindingKey()).speciesPrefix();
         AzBakedAnimation top = var7.getAnimation(body, prefix + "swim_stance");
         AzBakedAnimation deep = var7.getAnimation(body, prefix + "swim_stance_under");
         if (top != s.topClip) {
            s.topClip = top;
            s.top = stance(top);
         }

         if (deep != s.deepClip) {
            s.deepClip = deep;
            s.deep = stance(deep);
         }
      }
   }

   private static Map<String, double[]> stance(AzBakedAnimation clip) {
      if (clip == null) {
         return Map.of();
      }

      ClipIndex index = ClipIndex.of(clip);
      Map<String, double[]> out = new LinkedHashMap<>();

      for (int t = 0; t < index.trackCount(); t++) {
         double[] v = new double[6];
         Arrays.fill(v, Double.NaN);

         for (int i = 0; i < 3; i++) {
            if (index.rotates(t)) {
               v[i] = ClipSampler.value(index.channel(t, 0 + i), 0.0, true, i < 2, null);
            }

            if (index.moves(t)) {
               v[3 + i] = ClipSampler.value(index.channel(t, 3 + i), 0.0, false, false, null);
            }
         }

         if (index.rotates(t) || index.moves(t)) {
            out.put(index.boneName(t), v);
         }
      }

      return out;
   }

   private static void floats(RigFrame frame, FaunaBase body, List<SwimSpec.Floater> floaters, SwimPass.Stroke s, double dt, double leftOpen, double rightOpen) {
      if (s.floats.length != floaters.size()) {
         s.floats = new double[floaters.size()];
         s.left = new float[floaters.size()];
         Arrays.fill(s.left, Float.NaN);
      }

      double ease = Ease.lag(dt, 5.0);
      double pixel = pixelBlocks(frame);

      for (int f = 0; f < floaters.size(); f++) {
         SwimSpec.Floater fl = floaters.get(f);
         AzBone bone = frame.model.getBoneOrNull(fl.bone());
         AzBone point = frame.model.getBoneOrNull(fl.point());
         if (bone != null && point != null) {
            BoneAxis channel = fl.move() ? BoneAxis.POS_Y : ROT[fl.axis()];
            double c = s.floats[f];
            float read = frame.write.read(fl.bone(), channel);
            float base = !Float.isNaN(s.left[f]) && read == s.left[f] ? (float)(read - c) : read;
            write(frame, fl.bone(), channel, base + c);
            double weight = s.weight * s.surface * ("left".equals(fl.wing()) ? leftOpen : ("right".equals(fl.wing()) ? rightOpen : 1.0));
            double want = 0.0;
            if (weight > 0.0) {
               Vec3 at = joint(frame, point, null, null, 0.0);
               double line = WaterDepth.surface(body.level(), at.x, at.y, at.z, 4);
               if (!Double.isNaN(line)) {
                  double nudge = fl.move() ? 1.0 : 0.02;
                  double slope = (joint(frame, point, bone, channel, nudge).y - at.y) / nudge;
                  if (Math.abs(slope) > 1.0E-6) {
                     double error = line - fl.depth() * pixel - at.y;
                     double limit = fl.move() ? fl.max() : Math.toRadians(fl.max());
                     want = Mth.clamp(c + error / slope, -limit, limit) * weight;
                  }
               }
            }

            double next = c + (want - c) * ease;
            write(frame, fl.bone(), channel, base + next);
            s.floats[f] = next;
            s.left[f] = (float)(base + next);
         }
      }
   }

   private static void write(RigFrame frame, String bone, BoneAxis axis, double value) {
      frame.write.add(bone, axis, (float)(value - frame.write.read(bone, axis)));
   }

   private static Vec3 joint(RigFrame frame, AzBone point, AzBone nudged, BoneAxis axis, double by) {
      List<AzBone> chain = BoneFk.ancestorsTopDown(point, frame.model);
      Matrix4f accum = new Matrix4f();

      for (AzBone b : chain) {
         if (b.getName().equals(point.getName())) {
            break;
         }

         if (nudged != null && b.getName().equals(nudged.getName())) {
            float rx = b.getRotX();
            float ry = b.getRotY();
            float rz = b.getRotZ();
            float px = b.getPosX();
            float py = b.getPosY();
            float pz = b.getPosZ();
            switch (axis) {
               case ROT_X:
                  rx += (float)by;
                  break;
               case ROT_Y:
                  ry += (float)by;
                  break;
               case ROT_Z:
                  rz += (float)by;
                  break;
               case POS_Y:
                  py += (float)by;
            }

            BoneFk.mulLocal(accum, b, rx, ry, rz, px, py, pz, accum);
         } else {
            BoneFk.mulLocal(accum, b);
         }
      }

      Vector3f local = BoneFk.offsetPoint(point);
      if (nudged != null && nudged.getName().equals(point.getName()) && axis == BoneAxis.POS_Y) {
         local.y += (float)(by / 16.0);
      }

      Vector3f w = frame.modelToWorld.transformPosition(accum.transformPosition(local));
      return new Vec3(w.x + frame.drawnPosition.x, w.y + frame.drawnPosition.y, w.z + frame.drawnPosition.z);
   }

   private static double pixelBlocks(RigFrame frame) {
      Vector3f up = frame.modelToWorld.transformDirection(new Vector3f(0.0F, 1.0F, 0.0F));
      List<String> root = frame.spec.chain("root");
      double scale = !root.isEmpty() && frame.write.has(root.get(0)) ? Math.abs(frame.write.read(root.get(0), BoneAxis.SCALE_Y)) : 1.0;
      return up.length() * scale / 16.0;
   }

   private static void open(double[] t, AzBoneSnapshot rest, double toward) {
      if (!Double.isNaN(t[0]) && !Double.isNaN(t[1]) && !Double.isNaN(t[2])) {
         Quaternionf q = new Quaternionf().rotationZYX((float)t[2], (float)t[1], (float)t[0]);
         q.slerp(new Quaternionf().rotationZYX(rest.getRotZ(), rest.getRotY(), rest.getRotX()), (float)toward);
         float[] e = BoneFk.eulerZYX(q);
         double[][] forms = new double[][]{{e[0], e[1], e[2]}, {e[0] + Math.PI, Math.PI - e[1], e[2] + Math.PI}};
         double best = Double.MAX_VALUE;
         double[] keep = null;

         for (double[] form : forms) {
            double far = 0.0;

            for (int i = 0; i < 3; i++) {
               form[i] = t[i] + Mth.wrapDegrees(Math.toDegrees(form[i] - t[i])) * (float) (Math.PI / 180.0);
               far += Math.abs(form[i] - t[i]);
            }

            if (far < best) {
               best = far;
               keep = form;
            }
         }

         System.arraycopy(keep, 0, t, 0, 3);
      } else {
         for (int i = 0; i < 3; i++) {
            if (!Double.isNaN(t[i])) {
               t[i] = Mth.lerp(toward, t[i], restRot(rest, i));
            }
         }
      }
   }

   private static SwimSpec.Wave axis(SwimSpec.Track track, int i, boolean rotation) {
      if (track == null) {
         return null;
      }

      SwimSpec.Wave[] axes = rotation ? track.rot() : track.pos();
      return axes == null ? null : axes[i];
   }

   private static void blend(RigFrame frame, SwimPass.Stroke s, String bone, int c, double target, double weight) {
      BoneAxis axis = CHANNELS[c];
      float[] mem = s.channels.computeIfAbsent(bone, b -> unset());
      float cur = frame.write.read(bone, axis);
      float base = !Float.isNaN(mem[c]) && cur == mem[c] ? cur - mem[6 + c] : cur;
      float value = (float)(base + (target - base) * weight);
      frame.write.add(bone, axis, value - cur);
      mem[c] = frame.write.read(bone, axis);
      mem[6 + c] = mem[c] - base;
   }

   private static float[] unset() {
      float[] mem = new float[12];
      Arrays.fill(mem, 0, 6, Float.NaN);
      return mem;
   }

   private static void giveBack(RigFrame frame, SwimSpec swim, SwimPass.Stroke s) {
      List<SwimSpec.Floater> floaters = swim.floats();

      for (int f = 0; f < s.floats.length && f < floaters.size(); f++) {
         SwimSpec.Floater fl = floaters.get(f);
         if (!Float.isNaN(s.left[f]) && s.floats[f] != 0.0 && frame.write.has(fl.bone())) {
            BoneAxis channel = fl.move() ? BoneAxis.POS_Y : ROT[fl.axis()];
            if (frame.write.read(fl.bone(), channel) == s.left[f]) {
               frame.write.add(fl.bone(), channel, (float)(-s.floats[f]));
            }
         }
      }

      Arrays.fill(s.floats, 0.0);
      Arrays.fill(s.left, Float.NaN);
      if (!s.channels.isEmpty()) {
         for (Entry<String, float[]> e : s.channels.entrySet()) {
            float[] mem = e.getValue();

            for (int c = 0; c < 6; c++) {
               if (!Float.isNaN(mem[c]) && mem[6 + c] != 0.0F && frame.write.read(e.getKey(), CHANNELS[c]) == mem[c]) {
                  frame.write.add(e.getKey(), CHANNELS[c], -mem[6 + c]);
               }
            }
         }

         s.channels.clear();
      }
   }

   private static double restRot(AzBoneSnapshot rest, int axis) {
      return switch (axis) {
         case 0 -> rest.getRotX();
         case 1 -> rest.getRotY();
         default -> rest.getRotZ();
      };
   }

   private static double speedShare(FaunaBase body) {
      FaunaStatSheet stats = body.getStatSheet();
      double top = stats == null ? 1.0 : Math.max(0.001, WaterEngine.topSpeed(body, stats));
      double dx = body.getX() - body.xo;
      double dy = body.getY() - body.yo;
      double dz = body.getZ() - body.zo;
      return Math.sqrt(dx * dx + dy * dy + dz * dz) / top;
   }

   private static double approach(double value, double target, double step) {
      return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
   }

   static double smoothstep(double from, double to, double x) {
      if (to <= from) {
         return x >= to ? 1.0 : 0.0;
      }

      double t = Mth.clamp((x - from) / (to - from), 0.0, 1.0);
      return t * t * (3.0 - 2.0 * t);
   }

   private record Laid(Map<String, SwimSpec.Track> top, Map<String, SwimSpec.Track> deep, Set<String> legs, Set<String> left, Set<String> right) {
   }

   private record Regime(double amplitude, double legWaves, double splay, double leftOpen, double rightOpen) {
   }

   private static final class Stroke {
      double phase;
      double time = Double.NaN;
      double weight;
      double speed;
      double turn;
      double surface = 1.0;
      double[] floats = new double[0];
      float[] left = new float[0];
      final Map<String, float[]> channels = new HashMap<>();
      AzBakedAnimation topClip;
      AzBakedAnimation deepClip;
      Map<String, double[]> top = Map.of();
      Map<String, double[]> deep = Map.of();
      SwimPass.Laid laid;
      AzBakedModel laidModel;
      RigSpec laidSpec;
      AzBakedAnimation laidTop;
      AzBakedAnimation laidDeep;
   }
}
