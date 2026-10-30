package net.magister.liberanimalium.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import mod.azure.azurelib.animation.AzAnimator;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.controller.keyframe.AzBoneAnimation;
import mod.azure.azurelib.animation.primitive.AzQueuedAnimation;
import mod.azure.azurelib.cache.object.GeoCube;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.anim.rig.BoneAxis;
import net.magister.liberanimalium.anim.rig.BoneFk;
import net.magister.liberanimalium.anim.rig.EyesSpec;
import net.magister.liberanimalium.anim.rig.RigFrame;
import net.magister.liberanimalium.anim.rig.RigPass;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.FaunaHeadTrackingSystem;
import net.magister.liberanimalium.chassis.LookFrame;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

final class EyesPass implements RigPass {
   static final float EDGE_OVERFLOW = 0.00375F;
   private static final BoneAxis[] CHANNELS = new BoneAxis[]{
      BoneAxis.POS_X, BoneAxis.POS_Y, BoneAxis.POS_Z, BoneAxis.SCALE_X, BoneAxis.SCALE_Y, BoneAxis.SCALE_Z
   };
   private static final Map<Integer, Map<String, float[]>> MEMORY = new ConcurrentHashMap<>();
   private static final int POSITION_KEYED = 1;
   private static final int SCALE_KEYED = 2;
   private static final Map<AzBakedModel, EyesPass.Shape> SHAPES = Collections.synchronizedMap(new WeakHashMap<>());
   private static final float DART_CELL_TICKS = 8.0F;
   private static final float DART_CHANCE = 0.35F;
   private static final int DART_LOOKBACK = 16;
   private static final float DART_REST_SHARE = 0.4F;
   private static final float DART_YAW = 18.0F;
   private static final float DART_PITCH = 10.0F;
   private static final float DART_SNAP_TICKS = 2.0F;
   private static final float DART_FOCUSED = 0.25F;

   @Override
   public String name() {
      return "eyes";
   }

   @Override
   public void solve(RigFrame frame) {
      if (frame.body instanceof FaunaBase body) {
         EyesSpec spec = frame.spec.eyes();
         if (!spec.isEmpty() && !body.hasFaunaPose(PoseFlag.SLEEPING) && !body.getSyncedIsIncapacitated()) {
            List<EyesPass.Eye> eyes = shape(frame, spec);
            if (!eyes.isEmpty()) {
               float mood = body.eyeState().mood(frame.partialTick);
               double[] bodyLook = gaze(body, frame.partialTick);
               double reach = Math.sin(spec.range() * (float) (Math.PI / 180.0));

               for (EyesPass.Eye eye : eyes) {
                  AzBone pupil = frame.model.getBoneOrNull(eye.pupil);
                  if (pupil != null) {
                     float[] own = ownHead(frame, body, pupil);
                     double[] look = own == null ? bodyLook : headGaze(body, frame.partialTick, own);
                     double[] g = LookFrame.apply(LookFrame.transpose(toModel(frame.model, pupil)), look);
                     double along = g[eye.out] * eye.outSign;
                     double[] moved = new double[]{g[0], g[1], g[2]};
                     moved[eye.out] = 0.0;
                     if (along < 0.0) {
                        double across = Math.sqrt(moved[0] * moved[0] + moved[1] * moved[1] + moved[2] * moved[2]);
                        if (across > 1.0E-6) {
                           for (int a = 0; a < 3; a++) {
                              moved[a] /= across;
                           }
                        }
                     }

                     int keyed = keyedByClip(body, eye.pupil);
                     float[] shift = new float[3];
                     float[] scale = new float[]{1.0F, 1.0F, 1.0F};
                     if ((keyed & 2) == 0) {
                        scale[eye.width] = toward(mood, spec.narrow()[0], spec.wide()[0]);
                        scale[eye.length] = toward(mood, spec.narrow()[1], spec.wide()[1]);
                     }

                     for (int axis = 0; axis < 3; axis++) {
                        if (axis != eye.out) {
                           float opening = Math.max(0.0F, Math.min(-eye.lo[axis], eye.hi[axis]));
                           if (eye.half[axis] > 0.0F) {
                              scale[axis] = Math.min(scale[axis], Math.max(opening, eye.half[axis]) / eye.half[axis]);
                           }

                           float room = Math.max(0.0F, opening - eye.half[axis] * scale[axis]) + 0.00375F;
                           float gaze = (float)Mth.clamp(moved[axis] / reach, -1.0, 1.0) * spec.travel();
                           shift[axis] = Mth.clamp(gaze, -1.0F, 1.0F) * room;
                        }

                        shift[axis] += eye.middle[axis] * (1.0F - scale[axis]);
                     }

                     if ((keyed & 1) != 0) {
                        shift = new float[3];
                     }

                     float[] mem = MEMORY.computeIfAbsent(body.getId(), id -> new HashMap<>()).computeIfAbsent(eye.pupil, p -> unset());
                     write(frame, eye.pupil, mem, 0, -shift[0] * 16.0F, false);
                     write(frame, eye.pupil, mem, 1, shift[1] * 16.0F, false);
                     write(frame, eye.pupil, mem, 2, shift[2] * 16.0F, false);

                     for (int axis = 0; axis < 3; axis++) {
                        write(frame, eye.pupil, mem, 3 + axis, scale[axis], true);
                     }
                  }
               }
            }
         }
      }
   }

   static void forget(int entityId) {
      MEMORY.remove(entityId);
   }

   private static float[] unset() {
      float[] mem = new float[12];
      Arrays.fill(mem, 0, 6, Float.NaN);
      return mem;
   }

   private static void write(RigFrame frame, String bone, float[] mem, int c, float share, boolean scale) {
      BoneAxis axis = CHANNELS[c];
      float cur = frame.write.read(bone, axis);
      boolean ours = !Float.isNaN(mem[c]) && cur == mem[c];
      float base = !ours ? cur : (scale ? cur / mem[6 + c] : cur - mem[6 + c]);
      float value = scale ? base * share : base + share;
      frame.write.set(bone, axis, value);
      mem[c] = frame.write.read(bone, axis);
      mem[6 + c] = share;
   }

   static float toward(float mood, float narrow, float wide) {
      return mood < 0.0F ? Mth.lerp(-mood, 1.0F, narrow) : Mth.lerp(mood, 1.0F, wide);
   }

   static double[] gaze(FaunaBase body, float partialTick) {
      FaunaHeadTrackingSystem head = body.getHeadTrackingSystem();
      boolean ridden = body.getControllingPassenger() instanceof Player;
      float yaw = body.getNetworkHeadYaw();
      float pitch = body.getNetworkHeadPitch();
      if (ridden) {
         yaw = head == null ? 0.0F : head.getSmoothedHeadYaw();
         pitch = head == null ? 0.0F : head.getSmoothedHeadPitch();
      }

      FaunaHeadTrackingSystem.LookPriority rung = body.getNetworkLookRung();
      float darting = rung != FaunaHeadTrackingSystem.LookPriority.NONE && rung != FaunaHeadTrackingSystem.LookPriority.AMBIENT ? 0.25F : 1.0F;
      float[] dart = dart(body.getId(), body.tickCount + partialTick);
      return direction(yaw + dart[0] * darting, pitch + dart[1] * darting);
   }

   private static float[] ownHead(RigFrame frame, FaunaBase body, AzBone pupil) {
      List<String> head = frame.spec.chain("head");
      if (head.isEmpty()) {
         return null;
      }

      for (AzBone bone : BoneFk.ancestorsTopDown(pupil, frame.model)) {
         for (String prefix : frame.spec.prefixes()) {
            if (bone.getName().equals(prefix + head.get(0))) {
               return body.getHeadTrackingSystem().gazes().angles(prefix, frame.partialTick);
            }
         }
      }

      return null;
   }

   static double[] headGaze(FaunaBase body, float partialTick, float[] angles) {
      float[] dart = dart(body.getId(), body.tickCount + partialTick);
      return direction(angles[0] + dart[0], angles[1] + dart[1]);
   }

   static double[] direction(float yaw, float pitch) {
      double r = yaw * (float) (Math.PI / 180.0);
      double p = pitch * (float) (Math.PI / 180.0);
      return new double[]{Math.sin(r) * Math.cos(p), -Math.sin(p), -Math.cos(r) * Math.cos(p)};
   }

   private static int keyedByClip(FaunaBase body, String pupil) {
      AzAnimator<UUID, FaunaBase> animator = body.getAnimatorOrNull();
      AzAnimationControllerContainer<FaunaBase> container = animator == null ? null : animator.getAnimationControllerContainer();
      if (container == null) {
         return 0;
      }

      int keyed = 0;

      for (AzAnimationController<FaunaBase> controller : container.getAll()) {
         AzQueuedAnimation playing = controller.currentAnimation();
         if (playing != null && !controller.stateMachine().isStopped()) {
            for (AzBoneAnimation track : playing.animation().boneAnimations()) {
               if (pupil.equals(track.boneName())) {
                  if (!track.positionKeyframes().xKeyframes().isEmpty()) {
                     keyed |= 1;
                  }

                  if (!track.scaleKeyframes().xKeyframes().isEmpty()) {
                     keyed |= 2;
                  }
               }
            }
         }
      }

      return keyed;
   }

   static float[] dart(int id, float ticks) {
      int cell = Mth.floor(ticks / 8.0F);
      int at = fixationAt(id, cell);
      int before = fixationAt(id, at - 1);
      float[] now = fixation(id, at);
      float[] was = fixation(id, before);
      float snap = Mth.clamp((ticks - at * 8.0F) / 2.0F, 0.0F, 1.0F);
      return new float[]{Mth.lerp(snap, was[0], now[0]), Mth.lerp(snap, was[1], now[1])};
   }

   private static int fixationAt(int id, int cell) {
      for (int back = 0; back < 16; back++) {
         if (unit(id, cell - back, 0) < 0.35F) {
            return cell - back;
         }
      }

      return cell - 16;
   }

   private static float[] fixation(int id, int cell) {
      return unit(id, cell, 1) < 0.4F
         ? new float[]{0.0F, 0.0F}
         : new float[]{(unit(id, cell, 2) * 2.0F - 1.0F) * 18.0F, (unit(id, cell, 3) * 2.0F - 1.0F) * 10.0F};
   }

   private static float unit(int id, int cell, int salt) {
      long h = id * -7046029254386353131L + cell * -4417276706812531889L + salt * 1609587929392839161L;
      h = (h ^ h >>> 33) * -49064778989728563L;
      h = (h ^ h >>> 33) * -4265267296055464877L;
      h ^= h >>> 33;
      return (float)(h >>> 40) / 1.6777216E7F;
   }

   private static double[] toModel(AzBakedModel model, AzBone pupil) {
      List<AzBone> chain = BoneFk.ancestorsTopDown(pupil, model);
      double[] product = LookFrame.IDENTITY;

      for (int i = 0; i < chain.size() - 1; i++) {
         AzBone b = chain.get(i);
         product = LookFrame.mul(product, LookFrame.of(b.getRotX(), b.getRotY(), b.getRotZ()));
      }

      return product;
   }

   private static List<EyesPass.Eye> shape(RigFrame frame, EyesSpec spec) {
      EyesPass.Shape known = SHAPES.get(frame.model);
      if (known != null && known.spec == spec) {
         return known.eyes;
      }

      List<EyesPass.Eye> eyes = new ArrayList<>();

      for (String name : spec.pupils()) {
         AzBone pupil = frame.model.getBoneOrNull(name);
         EyesPass.Eye eye = pupil == null ? null : measure(pupil, frame.model);
         if (eye != null) {
            eyes.add(eye);
         }
      }

      SHAPES.put(frame.model, new EyesPass.Shape(spec, List.copyOf(eyes)));
      return eyes;
   }

   private static EyesPass.Eye measure(AzBone pupil, AzBakedModel model) {
      AzBone parent = pupil.getParent() == null ? null : model.getBoneOrNull(pupil.getParent().getName());
      List<float[]> cubes = parent == null ? List.of() : boxes(parent);
      List<float[]> own = boxes(pupil);
      if (!cubes.isEmpty() && !own.isEmpty()) {
         float[] dot = union(own);
         float[] ball = union(cubes);
         int out = 0;
         float outSign = 1.0F;
         float best = -Float.MAX_VALUE;

         for (int axis = 0; axis < 3; axis++) {
            float above = dot[3 + axis] - ball[3 + axis];
            float below = ball[axis] - dot[axis];
            if (above > best) {
               best = above;
               out = axis;
               outSign = 1.0F;
            }

            if (below > best) {
               best = below;
               out = axis;
               outSign = -1.0F;
            }
         }

         float[] centre = new float[3];
         float[] half = new float[3];

         for (int axis = 0; axis < 3; axis++) {
            centre[axis] = (dot[axis] + dot[3 + axis]) / 2.0F;
            half[axis] = (dot[3 + axis] - dot[axis]) / 2.0F;
         }

         int a = (out + 1) % 3;
         int b = (out + 2) % 3;
         int width;
         if (Math.abs(half[a] - half[b]) < 1.0E-4F) {
            width = a == 1 ? b : a;
         } else {
            width = half[a] < half[b] ? a : b;
         }

         int length = width == a ? b : a;
         float[] lo = new float[3];
         float[] hi = new float[3];
         opening(cubes, ball, centre, half, a, b, lo, hi);
         opening(cubes, ball, centre, half, b, a, lo, hi);
         float[] middle = new float[]{centre[0] - pupil.getPivotX() / 16.0F, centre[1] - pupil.getPivotY() / 16.0F, centre[2] - pupil.getPivotZ() / 16.0F};
         return new EyesPass.Eye(pupil.getName(), out, outSign, width, length, half, lo, hi, middle);
      } else {
         return null;
      }
   }

   static void opening(List<float[]> cubes, float[] ball, float[] centre, float[] half, int axis, int across, float[] lo, float[] hi) {
      float c = centre[axis];
      float low = -Float.MAX_VALUE;
      float high = Float.MAX_VALUE;
      boolean on = false;
      boolean any = false;
      float onLow = Float.MAX_VALUE;
      float onHigh = -Float.MAX_VALUE;

      for (float[] box : cubes) {
         if (!(centre[across] < box[across]) && !(centre[across] > box[3 + across])) {
            any = true;
            if (c >= box[axis] && c <= box[3 + axis]) {
               on = true;
               onLow = Math.min(onLow, box[axis]);
               onHigh = Math.max(onHigh, box[3 + axis]);
            } else if (box[3 + axis] < c) {
               low = Math.max(low, box[3 + axis]);
            } else {
               high = Math.min(high, box[axis]);
            }
         }
      }

      if (on) {
         low = onLow;
         high = onHigh;
      } else if (!any) {
         low = ball[axis];
         high = ball[3 + axis];
      }

      lo[axis] = (low == -Float.MAX_VALUE ? c - half[axis] : low) - c;
      hi[axis] = (high == Float.MAX_VALUE ? c + half[axis] : high) - c;
   }

   private static List<float[]> boxes(AzBone bone) {
      List<float[]> out = new ArrayList<>();
      Vector3f min = new Vector3f();
      Vector3f max = new Vector3f();

      for (GeoCube cube : bone.getCubes()) {
         min.set(Float.MAX_VALUE);
         max.set(-Float.MAX_VALUE);
         CubeMesh.of(cube).bounds(min, max);
         if (min.x <= max.x) {
            out.add(new float[]{min.x, min.y, min.z, max.x, max.y, max.z});
         }
      }

      return out;
   }

   private static float[] union(List<float[]> boxes) {
      float[] u = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};

      for (float[] box : boxes) {
         for (int i = 0; i < 3; i++) {
            u[i] = Math.min(u[i], box[i]);
            u[3 + i] = Math.max(u[3 + i], box[3 + i]);
         }
      }

      return u;
   }

   record Eye(String pupil, int out, float outSign, int width, int length, float[] half, float[] lo, float[] hi, float[] middle) {
   }

   private record Shape(EyesSpec spec, List<EyesPass.Eye> eyes) {
   }
}
