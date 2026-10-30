package net.magister.liberanimalium.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.anim.sample.PoseCadence;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.util.PerfClock;

public final class PoseInterpolator {
   private static volatile PoseInterpolator.Path path = PoseInterpolator.Path.TICK;
   public static final PerfClock.Section CLOCK = PerfClock.client("anim.pose");
   private static final Map<Integer, PoseInterpolator.State> STATES = new ConcurrentHashMap<>();
   private static final int MARK_ROT = 1;
   private static final int MARK_POS = 2;
   private static final int MARK_SCALE = 4;

   private PoseInterpolator() {
   }

   public static PoseInterpolator.Path path() {
      return path;
   }

   public static void path(PoseInterpolator.Path value) {
      path = value == null ? PoseInterpolator.Path.TICK : value;
   }

   private static void readMarks(AzBone[] bones, byte[] marks) {
      for (int i = 0; i < bones.length; i++) {
         AzBone b = bones[i];
         int m = 0;
         if (b.hasRotationChanged()) {
            m |= 1;
         }

         if (b.hasPositionChanged()) {
            m |= 2;
         }

         if (b.hasScaleChanged()) {
            m |= 4;
         }

         marks[i] = (byte)m;
      }
   }

   private static void restoreMarks(AzBone[] bones, byte[] marks) {
      for (int i = 0; i < bones.length; i++) {
         AzBone b = bones[i];
         b.resetStateChanges();
         int m = marks[i];
         if ((m & 1) != 0) {
            b.markRotationAsChanged();
         }

         if ((m & 2) != 0) {
            b.markPositionAsChanged();
         }

         if ((m & 4) != 0) {
            b.markScaleAsChanged();
         }
      }
   }

   public static boolean evaluate(FaunaBase body, AzBakedModel model, float partialTick) {
      return evaluate(body.getId(), body.tickCount, forced(body), model, partialTick);
   }

   private static boolean forced(FaunaBase body) {
      return body.isVehicle();
   }

   public static boolean blended(FaunaBase body) {
      return path == PoseInterpolator.Path.TICK && !forced(body);
   }

   public static boolean evaluate(int entityId, int tickCount, boolean forced, AzBakedModel model, float partialTick) {
      if (path == PoseInterpolator.Path.TICK && model != null) {
         long t0 = CLOCK.start();

         try {
            PoseInterpolator.State state = STATES.computeIfAbsent(entityId, id -> new PoseInterpolator.State());
            state.resolve(model);
            if (state.cadence.due(tickCount, state.bones.length, forced)) {
               if (state.cadence.primed()) {
                  state.cadence.authoritative(state.scratch);
                  write(state.bones, state.scratch);
                  restoreMarks(state.bones, state.marks);
               }

               return true;
            } else {
               state.cadence.blend(partialTick, state.scratch);
               write(state.bones, state.scratch);
               restoreMarks(state.bones, state.marks);
               return false;
            }
         } finally {
            CLOCK.end(t0);
         }
      } else {
         return true;
      }
   }

   public static void captured(FaunaBase body, AzBakedModel model, float partialTick) {
      captured(body.getId(), body.tickCount, forced(body), model, partialTick);
   }

   public static void captured(int entityId, int tickCount, boolean forced, AzBakedModel model, float partialTick) {
      if (path == PoseInterpolator.Path.TICK && model != null) {
         PoseInterpolator.State state = STATES.get(entityId);
         if (state != null && state.forModel == model) {
            long t0 = CLOCK.start();

            try {
               read(state.bones, state.scratch);
               readMarks(state.bones, state.marks);
               state.cadence.capture(state.scratch, state.bones.length, tickCount);
               if (!forced) {
                  state.cadence.blend(partialTick, state.scratch);
                  write(state.bones, state.scratch);
                  restoreMarks(state.bones, state.marks);
                  return;
               }
            } finally {
               CLOCK.end(t0);
            }
         }
      }
   }

   public static void refreshMarks(int entityId, AzBakedModel model) {
      if (path == PoseInterpolator.Path.TICK && model != null) {
         PoseInterpolator.State state = STATES.get(entityId);
         if (state != null && state.forModel == model) {
            readMarks(state.bones, state.marks);
         }
      }
   }

   public static void forget(int entityId) {
      STATES.remove(entityId);
   }

   public static void clear() {
      STATES.clear();
   }

   public static int tracked() {
      return STATES.size();
   }

   private static void read(AzBone[] bones, float[] out) {
      int i = 0;

      for (AzBone b : bones) {
         out[i++] = b.getRotX();
         out[i++] = b.getRotY();
         out[i++] = b.getRotZ();
         out[i++] = b.getPosX();
         out[i++] = b.getPosY();
         out[i++] = b.getPosZ();
         out[i++] = b.getScaleX();
         out[i++] = b.getScaleY();
         out[i++] = b.getScaleZ();
      }
   }

   private static void write(AzBone[] bones, float[] in) {
      int i = 0;

      for (AzBone b : bones) {
         b.setRotX(in[i++]);
         b.setRotY(in[i++]);
         b.setRotZ(in[i++]);
         b.setPosX(in[i++]);
         b.setPosY(in[i++]);
         b.setPosZ(in[i++]);
         b.setScaleX(in[i++]);
         b.setScaleY(in[i++]);
         b.setScaleZ(in[i++]);
      }
   }

   public enum Path {
      TICK,
      FRAME;
   }

   private static final class State {
      final PoseCadence cadence = new PoseCadence();
      AzBakedModel forModel;
      AzBone[] bones = new AzBone[0];
      float[] scratch = new float[0];
      byte[] marks = new byte[0];

      void resolve(AzBakedModel model) {
         if (model != this.forModel) {
            this.forModel = model;
            this.bones = model.getBonesByName().values().toArray(new AzBone[0]);
            this.scratch = new float[this.bones.length * 9];
            this.marks = new byte[this.bones.length];
            this.cadence.reset();
         }
      }
   }
}
