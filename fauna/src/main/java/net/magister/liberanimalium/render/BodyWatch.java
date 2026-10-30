package net.magister.liberanimalium.render;

import com.mojang.logging.LogUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.Map.Entry;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.anim.rig.Attitude;
import net.magister.liberanimalium.anim.rig.BoneFk;
import net.magister.liberanimalium.anim.rig.LimbDef;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.AttitudeComponent;
import net.magister.liberanimalium.chassis.component.LimbReachComponent;
import net.magister.liberanimalium.debug.client.LegReachDebugRenderer;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

public final class BodyWatch {
   static final int WINDOW = 600;
   static final int TRACE = 180;
   static final int TRAIL = 60;
   static final double BOUNCE = 0.03;
   static final double MISS = 0.05;
   static final double TARGET_JUMP_DEG = 2.0;
   static final double PX_PER_BLOCK = 14.0;
   static final double PX_PER_DEG = 0.5;
   static final int TRACE_H = 34;
   static final int FEET = 4;
   private static int watched = Integer.MIN_VALUE;
   private static String watchedName = "";
   private static final ArrayDeque<BodyWatch.Frame> FRAMES = new ArrayDeque<>();
   private static long lastKey = Long.MIN_VALUE;
   private static BufferedWriter csv;
   private static Path csvPath;
   private static int csvRows;
   private static int lastConsoleTick = Integer.MIN_VALUE;
   static final String HEADER = header();
   private static final Logger LOGGER = LogUtils.getLogger();

   private BodyWatch() {
   }

   public static void clientInit() {
      ClientEvents.HUD_AFTER_HOTBAR.register(BodyWatch::onRenderGui);
   }

   private static String header() {
      StringBuilder h = new StringBuilder(
         "frame,tick,pt,evaluated,blended,ridden,transport,flying,on_ground,gate,falling,x,z,y,yo,y_old,hitbox_y,height_o,height,expected,held_x,held_y,held_z,bob_y,stack_y,lift,asked,miss,jump,pitch_deg,roll_deg,lean_deg,ask_pitch_deg,ask_roll_deg,ask_height,g_front,g_back,g_left,g_right,start_front,start_back,start_left,start_right,probes_reused,front_y,back_y,body"
      );

      for (int i = 1; i <= 4; i++) {
         h.append(",foot").append(i).append(",foot").append(i).append("_y,foot").append(i).append("_target,foot").append(i).append("_ground");
      }

      return h.toString();
   }

   public static Path start(int entityId, String name) {
      stop();
      watched = entityId;
      watchedName = name;
      FRAMES.clear();
      lastKey = Long.MIN_VALUE;
      lastConsoleTick = Integer.MIN_VALUE;
      WatchFiles.Opened opened = WatchFiles.open("bodywatch", name, entityId, HEADER, LOGGER, "[BodyWatch]");
      csv = opened == null ? null : opened.writer();
      csvPath = opened == null ? null : opened.path();
      if (opened != null) {
         csvRows = 0;
      }

      return csvPath;
   }

   public static void stop() {
      watched = Integer.MIN_VALUE;
      FRAMES.clear();
      WatchFiles.close(csv);
      csv = null;
   }

   public static boolean watching(int entityId) {
      return watched == entityId;
   }

   public static Path file() {
      return csvPath;
   }

   public static int rows() {
      return csvRows;
   }

   static void forget(int entityId) {
      if (watched == entityId) {
         LOGGER.debug("[BodyWatch] {} left; the watch stopped ({} rows in {})", new Object[]{watchedName, csvRows, csvPath});
         stop();
      }
   }

   static void sample(FaunaBase body, AzBakedModel model, float partialTick, boolean evaluated) {
      long key = (long)body.tickCount << 32 | Float.floatToRawIntBits(partialTick) & 4294967295L;
      if (key != lastKey) {
         lastKey = key;
         AttitudeComponent st = AttitudeComponent.of(body);
         List<String> root = body.animRig().chain("root");
         AzBone bone = !root.isEmpty() && model != null ? model.getBoneOrNull(root.get(0)) : null;
         Matrix4f toWorld = FaunaProceduralAnimator.modelToWorld();
         double drawnX = Mth.lerp(partialTick, body.xOld, body.getX());
         double hitboxY = Mth.lerp(partialTick, body.yOld, body.getY());
         double drawnZ = Mth.lerp(partialTick, body.zOld, body.getZ());
         double heldX = drawnX;
         double heldY = hitboxY;
         double heldZ = drawnZ;
         double bobY = 0.0;
         if (bone != null) {
            Vector3f held = heldOffset(toWorld, BoneFk.ancestorsTopDown(bone, model));
            heldX = drawnX + held.x;
            heldY = hitboxY + held.y;
            heldZ = drawnZ + held.z;
            float[] add = st == null ? null : st.bodyWrites.get(bone.getName());
            if (add != null) {
               bobY = toWorld.transformDirection(new Vector3f(0.0F, add[4] / 16.0F, 0.0F)).y;
            }
         }

         double height = st == null ? Double.NaN : st.height;
         double heightO = st == null ? Double.NaN : st.heightO;
         double expected = st == null || !st.wrote || Double.isNaN(height)
            ? Double.NaN
            : (Double.isNaN(heightO) ? height : Mth.lerp(partialTick, heightO, height));
         BodyWatch.Pose pose = pose(body, model, st, toWorld, partialTick, hitboxY);
         BodyWatch.Frame f = new BodyWatch.Frame(
            PerfClock.frameId(),
            body.tickCount,
            partialTick,
            evaluated,
            PoseInterpolator.blended(body),
            body.isVehicle(),
            body.getTransportMode().name(),
            body.isFlying(),
            body.onGround(),
            st != null && st.gate,
            st != null && st.falling,
            body.getX(),
            body.getZ(),
            body.getY(),
            body.yo,
            body.yOld,
            hitboxY,
            heightO,
            height,
            expected,
            heldX,
            heldY,
            heldZ,
            bobY,
            toWorld.m31(),
            pose,
            bodyChannels(st)
         );
         BodyWatch.Frame prev = FRAMES.peekLast();
         FRAMES.addLast(f);

         while (FRAMES.size() > 600) {
            FRAMES.removeFirst();
         }

         writeCsv(f, prev);
         drawTrails(body.getId());
         if (body.tickCount - lastConsoleTick >= 20) {
            lastConsoleTick = body.tickCount;
            LOGGER.debug("[BodyWatch] {} t={} last 1 s: {}", new Object[]{watchedName, body.tickCount, BodyWatch.Stats.of(last(60)).line()});
         }
      }
   }

   private static String bodyChannels(AttitudeComponent st) {
      if (st != null && !st.bodyWrites.isEmpty()) {
         StringBuilder out = new StringBuilder();

         for (Entry<String, float[]> e : new TreeMap<>(st.bodyWrites).entrySet()) {
            float[] d = e.getValue();

            for (int i = 0; i < 6; i++) {
               if (d[i] != 0.0F) {
                  double v = i < 3 ? Math.toDegrees(d[i]) : d[i];
                  if (out.length() > 0) {
                     out.append(';');
                  }

                  out.append(e.getKey())
                     .append('.')
                     .append((char)(i < 3 ? 'r' : 't'))
                     .append("xyz".charAt(i % 3))
                     .append('=')
                     .append(String.format(Locale.ROOT, "%.3f", v));
               }
            }
         }

         return out.toString();
      } else {
         return "";
      }
   }

   private static BodyWatch.Pose pose(FaunaBase body, AzBakedModel model, AttitudeComponent st, Matrix4f toWorld, float partialTick, double hitboxY) {
      double pitch = Double.NaN;
      double roll = Double.NaN;
      double lean = Double.NaN;
      double askPitch = Double.NaN;
      double askRoll = Double.NaN;
      double askHeight = Double.NaN;
      double[] grounds = new double[]{Double.NaN, Double.NaN, Double.NaN, Double.NaN};
      double[] starts = new double[]{Double.NaN, Double.NaN, Double.NaN, Double.NaN};
      int reused = 0;
      if (st != null) {
         float pt = Attitude.sampleTick(PoseInterpolator.blended(body), partialTick);
         pitch = Math.toDegrees(Mth.lerp(pt, st.pitchO, st.pitch));
         roll = Math.toDegrees(Mth.lerp(pt, st.rollO, st.roll));
         lean = Math.toDegrees(Mth.lerp(pt, st.leanO, st.lean));
         askPitch = Math.toDegrees(st.cross.pitch());
         askRoll = Math.toDegrees(st.cross.roll());
         askHeight = st.cross.height();

         for (int i = 0; i < 4; i++) {
            grounds[i] = st.grounds[i];
            starts[i] = st.centreY + st.starts[i];
         }

         reused = st.reused;
      }

      double frontY = Double.NaN;
      double backY = Double.NaN;
      List<String> spine = body.animRig().chain("spine");
      if (model != null && !spine.isEmpty()) {
         frontY = pivotY(toWorld, model, spine.get(0), hitboxY);
         backY = pivotY(toWorld, model, spine.get(spine.size() - 1), hitboxY);
      }

      String[] feet = new String[4];
      double[] footY = new double[4];
      double[] footTarget = new double[4];
      double[] footGround = new double[4];
      Arrays.fill(feet, "");
      Arrays.fill(footY, Double.NaN);
      Arrays.fill(footTarget, Double.NaN);
      Arrays.fill(footGround, Double.NaN);
      List<LimbDef> limbs = body.animSteps().limbs;
      LimbReachComponent reach = LimbReachComponent.of(body);

      for (int i = 0; i < Math.min(4, limbs.size()); i++) {
         LimbDef L = limbs.get(i);
         feet[i] = L.name();
         if (model != null) {
            footY[i] = pivotY(toWorld, model, L.foot(), hitboxY);
         }

         if (reach != null) {
            double[] w = reach.footWatch(L.name());
            footTarget[i] = w[0];
            footGround[i] = w[1];
         }
      }

      return new BodyWatch.Pose(pitch, roll, lean, askPitch, askRoll, askHeight, grounds, starts, reused, frontY, backY, feet, footY, footTarget, footGround);
   }

   private static double pivotY(Matrix4f toWorld, AzBakedModel model, String name, double hitboxY) {
      AzBone b = model.getBoneOrNull(name);
      if (b == null) {
         return Double.NaN;
      }

      List<AzBone> chain = BoneFk.ancestorsTopDown(b, model);
      Matrix4f accum = new Matrix4f();

      for (int i = 0; i < chain.size() - 1; i++) {
         BoneFk.mulLocal(accum, chain.get(i));
      }

      Vector3f p = toWorld.transformPosition(accum.transformPosition(BoneFk.offsetPoint(b)));
      return hitboxY + p.y;
   }

   static Vector3f heldOffset(Matrix4f toWorld, List<AzBone> chain) {
      Matrix4f accum = new Matrix4f();

      for (AzBone b : chain) {
         BoneFk.mulLocal(accum, b);
      }

      Vector3f p = new Matrix4f(toWorld).invert().transformPosition(new Vector3f());
      return toWorld.transformPosition(accum.transformPosition(p));
   }

   private static void writeCsv(BodyWatch.Frame f, BodyWatch.Frame prev) {
      if (csv != null) {
         try {
            StringBuilder row = new StringBuilder(
               String.format(
                  Locale.ROOT,
                  "%d,%d,%.3f,%d,%d,%d,%s,%d,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%s,%s,%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%s,%s,%s",
                  f.frameId,
                  f.tick,
                  f.pt,
                  f.evaluated ? 1 : 0,
                  f.blended ? 1 : 0,
                  f.ridden ? 1 : 0,
                  f.transport,
                  f.flying ? 1 : 0,
                  f.onGround ? 1 : 0,
                  f.gate ? 1 : 0,
                  f.falling ? 1 : 0,
                  f.x,
                  f.z,
                  f.y,
                  f.yo,
                  f.yOld,
                  f.hitboxY,
                  num(f.heightO),
                  num(f.height),
                  num(f.expected),
                  f.heldX,
                  f.heldY,
                  f.heldZ,
                  f.bobY,
                  f.stackY,
                  f.lift(),
                  num(f.asked()),
                  num(f.miss()),
                  prev == null ? "" : String.format(Locale.ROOT, "%.4f", f.heldY - prev.heldY)
               )
            );
            BodyWatch.Pose p = f.pose;
            row.append(',')
               .append(num(p.pitchDeg))
               .append(',')
               .append(num(p.rollDeg))
               .append(',')
               .append(num(p.leanDeg))
               .append(',')
               .append(num(p.askPitchDeg))
               .append(',')
               .append(num(p.askRollDeg))
               .append(',')
               .append(num(p.askHeight));

            for (double g : p.grounds) {
               row.append(',').append(num(g));
            }

            for (double s : p.starts) {
               row.append(',').append(num(s));
            }

            row.append(',').append(p.reused).append(',').append(num(p.frontY)).append(',').append(num(p.backY));

            for (int i = 0; i < 4; i++) {
               row.append(',')
                  .append(p.feet[i])
                  .append(',')
                  .append(num(p.footY[i]))
                  .append(',')
                  .append(num(p.footTarget[i]))
                  .append(',')
                  .append(num(p.footGround[i]));
            }

            row.append(',').append(f.body);
            csv.write(row.append('\n').toString());
            csvRows++;
            if (csvRows % 60 == 0) {
               csv.flush();
            }
         } catch (IOException e) {
            LOGGER.warn("[BodyWatch] the watch file failed; the panel keeps running", e);
            csv = null;
         }
      }
   }

   private static String num(double v) {
      return Double.isNaN(v) ? "" : String.format(Locale.ROOT, "%.4f", v);
   }

   private static BodyWatch.Frame[] last(int n) {
      int size = FRAMES.size();
      int skip = Math.max(0, size - n);
      BodyWatch.Frame[] out = new BodyWatch.Frame[size - skip];
      Iterator<BodyWatch.Frame> it = FRAMES.iterator();

      for (int i = 0; i < skip; i++) {
         it.next();
      }

      for (int i = 0; i < out.length; i++) {
         out[i] = it.next();
      }

      return out;
   }

   static boolean bounce(BodyWatch.Frame[] s, int k) {
      if (k < 2) {
         return false;
      }

      double a = s[k - 1].heldY - s[k - 2].heldY;
      double b = s[k].heldY - s[k - 1].heldY;
      return Math.abs(a) > 0.03 && Math.abs(b) > 0.03 && a * b < 0.0 && s[k].tick - s[k - 2].tick <= 1;
   }

   private static void drawTrails(int entityId) {
      BodyWatch.Frame[] s = last(60);

      for (int k = 1; k < s.length; k++) {
         BodyWatch.Frame a = s[k - 1];
         BodyWatch.Frame b = s[k];
         boolean red = bounce(s, k);
         LegReachDebugRenderer.line(entityId, a.heldX, a.heldY, a.heldZ, b.heldX, b.heldY, b.heldZ, 1.0F, red ? 0.2F : 0.3F, red ? 0.2F : 1.0F);
         LegReachDebugRenderer.line(entityId, a.heldX, a.hitboxY, a.heldZ, b.heldX, b.hitboxY, b.heldZ, 0.6F, 0.6F, 0.6F);
      }
   }

   public static void onRenderGui(GuiGraphics g, float partialTick) {
      if (watched != Integer.MIN_VALUE) {
         Minecraft mc = Minecraft.getInstance();
         Font font = mc.font;
         int lineH = 9;
         int panelW = Math.max(290, 330);
         int x0 = g.guiWidth() - panelW - 4;
         int y = 4;
         int height = lineH * 5 + 4 * (34 + lineH + 2);
         g.fill(x0 - 2, y - 2, x0 + panelW + 2, y + height + 2, -1072689136);
         g.drawString(font, "BODY WATCH  " + watchedName, x0, y, -12192, false);
         y += lineH;
         g.drawString(font, "per frame  |  /rig body watch off  |  " + csvRows + " rows to file", x0, y, -7303024, false);
         y += lineH;
         BodyWatch.Frame[] all = last(600);
         if (all.length == 0) {
            g.drawString(font, "not drawn yet (is it in view?)", x0, y, -32640, false);
         } else {
            BodyWatch.Stats st = BodyWatch.Stats.of(all);
            g.drawString(
               font,
               String.format(
                  Locale.ROOT,
                  "last %d frames: steps %d  BOUNCE %d  MISS %d  pitch %.0f..%.0f  target jumps %d",
                  st.frames,
                  st.steps,
                  st.bounces,
                  st.misses,
                  st.pitchMin,
                  st.pitchMax,
                  st.targetJumps
               ),
               x0,
               y,
               st.bounces + st.misses + st.targetJumps > 0 ? -40864 : -8323200,
               false
            );
            y += lineH;
            BodyWatch.Frame now = all[all.length - 1];
            g.drawString(
               font,
               String.format(
                  Locale.ROOT,
                  "%s  gate %s  falling %s  %s  %s  gate-off %d  falling %d  flying %d",
                  now.transport,
                  now.gate ? "on" : "OFF",
                  now.falling ? "YES" : "no",
                  now.blended ? "blended" : "per-frame",
                  now.ridden ? "ridden" : "unridden",
                  st.gateOff,
                  st.falling,
                  st.flying
               ),
               x0,
               y,
               -5197648,
               false
            );
            y += lineH + 2;
            BodyWatch.Frame[] s = last(180);
            y = trace(g, font, s, x0, y, "lift: drawn floor - hitbox", 0, lineH);
            y = trace(g, font, s, x0, y, "asked: attitude height - hitbox", 1, lineH);
            y = trace(g, font, s, x0, y, "pitch: drawn (cyan), asked (grey), deg", 2, lineH);
            trace(g, font, s, x0, y, "ends: torso front (cyan), back (green) - floor", 3, lineH);
         }
      }
   }

   private static int trace(GuiGraphics g, Font font, BodyWatch.Frame[] s, int x0, int y, String label, int which, int lineH) {
      g.drawString(font, label, x0, y, -1, false);
      y += lineH;
      g.fill(x0, y, x0 + 180, y + 34, 1090519039);
      int mid = y + 17;
      g.fill(x0, mid, x0 + 180, mid + 1, 1627389951);
      double lo = Double.POSITIVE_INFINITY;
      double hi = Double.NEGATIVE_INFINITY;

      for (int k = 0; k < s.length; k++) {
         BodyWatch.Frame f = s[k];
         int px = x0 + k;
         if (f.stepping()) {
            g.fill(px, y, px + 1, y + 3, -1);
         }

         double second = Double.NaN;
         double scale = 14.0;
         double v;
         if (which == 0) {
            v = f.lift();
         } else if (which == 1) {
            v = f.asked();
         } else if (which == 2) {
            v = -f.pose.pitchDeg;
            second = -f.pose.askPitchDeg;
            scale = 0.5;
         } else {
            v = f.pose.frontY - f.heldY;
            second = f.pose.backY - f.heldY;
         }

         if (Double.isNaN(v)) {
            g.fill(px, mid - 1, px + 1, mid + 2, -28624);
         } else {
            lo = Math.min(lo, v);
            hi = Math.max(hi, v);
            if (!Double.isNaN(second)) {
               int dy2 = (int)Math.round(Mth.clamp(second * scale, -16.0, 16.0));
               g.fill(px, mid - dy2, px + 1, mid - dy2 + 1, which == 2 ? -7303024 : -12517536);
            }

            int dy = (int)Math.round(Mth.clamp(v * scale, -16.0, 16.0));
            int colour = bounce(s, k) ? -53200 : (!f.gate ? -28624 : (f.falling ? -8144 : -12529409));
            g.fill(px, mid - dy, px + 1, mid - dy + 1, colour);
         }
      }

      String range = lo > hi ? " - " : String.format(Locale.ROOT, "%+.2f .. %+.2f", lo, hi);
      g.drawString(font, range, x0 + 180 + 4, mid - lineH / 2, -5197648, false);
      return y + 34 + 2;
   }

   record Frame(
      int frameId,
      int tick,
      float pt,
      boolean evaluated,
      boolean blended,
      boolean ridden,
      String transport,
      boolean flying,
      boolean onGround,
      boolean gate,
      boolean falling,
      double x,
      double z,
      double y,
      double yo,
      double yOld,
      double hitboxY,
      double heightO,
      double height,
      double expected,
      double heldX,
      double heldY,
      double heldZ,
      double bobY,
      double stackY,
      BodyWatch.Pose pose,
      String body
   ) {
      double lift() {
         return this.heldY - this.hitboxY;
      }

      double asked() {
         return Double.isNaN(this.expected) ? Double.NaN : this.expected - this.hitboxY;
      }

      double miss() {
         return Double.isNaN(this.expected) ? Double.NaN : this.heldY - this.bobY - this.expected;
      }

      boolean stepping() {
         return Math.abs(this.y - this.yo) > 0.3;
      }
   }

   record Pose(
      double pitchDeg,
      double rollDeg,
      double leanDeg,
      double askPitchDeg,
      double askRollDeg,
      double askHeight,
      double[] grounds,
      double[] starts,
      int reused,
      double frontY,
      double backY,
      String[] feet,
      double[] footY,
      double[] footTarget,
      double[] footGround
   ) {
   }

   record Stats(
      int frames,
      int evaluated,
      int steps,
      int bounces,
      int misses,
      double missMax,
      double jumpMax,
      double liftMin,
      double liftMax,
      int gateOff,
      int falling,
      int flying,
      int blended,
      int ridden,
      double pitchMin,
      double pitchMax,
      double pitchStepMax,
      int targetJumps
   ) {
      static BodyWatch.Stats of(BodyWatch.Frame[] s) {
         int ev = 0;
         int steps = 0;
         int bounces = 0;
         int misses = 0;
         int gateOff = 0;
         int falling = 0;
         int flying = 0;
         int blended = 0;
         int ridden = 0;
         int targetJumps = 0;
         double missMax = 0.0;
         double jumpMax = 0.0;
         double liftMin = Double.POSITIVE_INFINITY;
         double liftMax = Double.NEGATIVE_INFINITY;
         double pitchMin = Double.POSITIVE_INFINITY;
         double pitchMax = Double.NEGATIVE_INFINITY;
         double pitchStepMax = 0.0;
         int lastStepTick = Integer.MIN_VALUE;

         for (int k = 0; k < s.length; k++) {
            BodyWatch.Frame f = s[k];
            if (f.evaluated) {
               ev++;
            }

            if (f.stepping() && f.tick != lastStepTick) {
               steps++;
               lastStepTick = f.tick;
            }

            if (BodyWatch.bounce(s, k)) {
               bounces++;
            }

            double miss = f.miss();
            if (!Double.isNaN(miss)) {
               missMax = Math.max(missMax, Math.abs(miss));
               if (Math.abs(miss) > 0.05) {
                  misses++;
               }
            }

            if (k > 0) {
               jumpMax = Math.max(jumpMax, Math.abs(f.heldY - s[k - 1].heldY));
               double dp = f.pose.pitchDeg - s[k - 1].pose.pitchDeg;
               if (!Double.isNaN(dp)) {
                  pitchStepMax = Math.max(pitchStepMax, Math.abs(dp));
               }

               if (f.tick != s[k - 1].tick) {
                  double dt = f.pose.askPitchDeg - s[k - 1].pose.askPitchDeg;
                  if (!Double.isNaN(dt) && Math.abs(dt) > 2.0) {
                     targetJumps++;
                  }
               }
            }

            if (!Double.isNaN(f.pose.pitchDeg)) {
               pitchMin = Math.min(pitchMin, f.pose.pitchDeg);
               pitchMax = Math.max(pitchMax, f.pose.pitchDeg);
            }

            liftMin = Math.min(liftMin, f.lift());
            liftMax = Math.max(liftMax, f.lift());
            if (!f.gate) {
               gateOff++;
            }

            if (f.falling) {
               falling++;
            }

            if (f.flying) {
               flying++;
            }

            if (f.blended) {
               blended++;
            }

            if (f.ridden) {
               ridden++;
            }
         }

         if (s.length == 0) {
            liftMax = 0.0;
            liftMin = 0.0;
         }

         if (pitchMin > pitchMax) {
            pitchMax = 0.0;
            pitchMin = 0.0;
         }

         return new BodyWatch.Stats(
            s.length,
            ev,
            steps,
            bounces,
            misses,
            missMax,
            jumpMax,
            liftMin,
            liftMax,
            gateOff,
            falling,
            flying,
            blended,
            ridden,
            pitchMin,
            pitchMax,
            pitchStepMax,
            targetJumps
         );
      }

      String line() {
         return String.format(
            Locale.ROOT,
            "frames %d (eval %d, blended %d, ridden %d)  steps %d  BOUNCE %d  MISS %d (max %.3f)  jump<=%.3f  lift %.2f..%.2f  pitch %.1f..%.1f (frame step<=%.1f, target jumps %d)  gate-off %d  falling %d  flying %d",
            this.frames,
            this.evaluated,
            this.blended,
            this.ridden,
            this.steps,
            this.bounces,
            this.misses,
            this.missMax,
            this.jumpMax,
            this.liftMin,
            this.liftMax,
            this.pitchMin,
            this.pitchMax,
            this.pitchStepMax,
            this.targetJumps,
            this.gateOff,
            this.falling,
            this.flying
         );
      }
   }
}
