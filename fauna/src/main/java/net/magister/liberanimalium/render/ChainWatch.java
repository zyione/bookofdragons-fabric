package net.magister.liberanimalium.render;

import com.mojang.logging.LogUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.magister.liberanimalium.anim.rig.PointChain;
import net.magister.liberanimalium.debug.client.LegReachDebugRenderer;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class ChainWatch {
   static final int WINDOW = 200;
   static final int TRACE = 120;
   static final int TRAIL = 40;
   static final double BOUNCE_DY = 0.02;
   static final double FLIP_PUSH = 0.005;
   static final double OVER_CAP_DEG = 1.0;
   static final double INSIDE_DEPTH = 0.05;
   static final double TRACE_PX_PER_BLOCK = 16.0;
   private static int watched = Integer.MIN_VALUE;
   private static String watchedName = "";
   private static String rung = "";
   private static final Map<String, ChainWatch.History> CHAINS = new LinkedHashMap<>();
   private static BufferedWriter csv;
   private static Path csvPath;
   private static int csvRows;
   private static int lastConsoleTick = Integer.MIN_VALUE;
   private static final Logger LOGGER = LogUtils.getLogger();

   private ChainWatch() {
   }

   public static void clientInit() {
      ClientEvents.HUD_AFTER_HOTBAR.register(ChainWatch::onRenderGui);
   }

   public static Path start(int entityId, String name) {
      stop();
      watched = entityId;
      watchedName = name;
      CHAINS.clear();
      lastConsoleTick = Integer.MIN_VALUE;
      WatchFiles.Opened opened = WatchFiles.open(
         "chainwatch",
         name,
         entityId,
         "tick,chain,particle,x,y,z,motion,contact,asleep,dev_deg,cap_deg,push,push_nx,push_ny,push_nz,inside,support,clip_max,step_us,lod,abs_deg,bend_deg,cone_deg,branch",
         LOGGER,
         "[ChainWatch]"
      );
      csv = opened == null ? null : opened.writer();
      csvPath = opened == null ? null : opened.path();
      if (opened != null) {
         csvRows = 0;
      }

      return csvPath;
   }

   public static void stop() {
      watched = Integer.MIN_VALUE;
      CHAINS.clear();
      WatchFiles.close(csv);
      csv = null;
   }

   public static boolean watching(int entityId) {
      return watched == entityId;
   }

   public static boolean active() {
      return watched != Integer.MIN_VALUE;
   }

   public static Path file() {
      return csvPath;
   }

   public static int rows() {
      return csvRows;
   }

   static void forget(int entityId) {
      if (watched == entityId) {
         LOGGER.debug("[ChainWatch] {} left; the watch stopped ({} rows in {})", new Object[]{watchedName, csvRows, csvPath});
         stop();
      }
   }

   static void sample(
      int tick, String key, PointChain chain, PointChain.Params params, int bones, double[] inside, double clipMax, double stepMicros, String lod
   ) {
      rung = lod;
      ChainWatch.Particle[] ps = new ChainWatch.Particle[chain.total()];

      for (int i = 0; i < ps.length; i++) {
         int bone = Math.min(i, bones - 1);
         ps[i] = new ChainWatch.Particle(
            chain.worldPoint(i),
            chain.motion(i),
            chain.contact(i),
            chain.asleep(i),
            chain.deviationDegrees(i),
            i == 0 ? 0.0 : params.bendAt(bone, bones),
            chain.pushed(i),
            inside[i],
            chain.support(i),
            chain.absoluteDeviationDegrees(i),
            chain.coneDeviationDegrees(i),
            i == 0 ? 0.0 : params.coneAt(bone, bones),
            chain.branch(i)
         );
      }

      ChainWatch.History h = CHAINS.computeIfAbsent(key, k -> new ChainWatch.History());
      h.add(new ChainWatch.Step(tick, ps, clipMax, stepMicros));
      writeCsv(tick, key, ps, clipMax, stepMicros, lod);
      if (tick - lastConsoleTick >= 20) {
         lastConsoleTick = tick;

         for (Entry<String, ChainWatch.History> e : CHAINS.entrySet()) {
            ChainWatch.Stats s = ChainWatch.Stats.of(e.getValue(), 20);
            LOGGER.debug("[ChainWatch] {} {} t={} last 1 s: {}", new Object[]{watchedName, e.getKey(), tick, s.line()});
         }
      }
   }

   private static void writeCsv(int tick, String key, ChainWatch.Particle[] ps, double clipMax, double stepMicros, String lod) {
      if (csv != null) {
         try {
            for (int i = 0; i < ps.length; i++) {
               ChainWatch.Particle p = ps[i];
               double len = p.push.length();
               Vec3 n = len > 1.0E-9 ? p.push.scale(1.0 / len) : Vec3.ZERO;
               csv.write(
                  String.format(
                     Locale.ROOT,
                     "%d,%s,%d,%.4f,%.4f,%.4f,%.4f,%d,%d,%.2f,%.2f,%.4f,%.3f,%.3f,%.3f,%.4f,%s,%.4f,%.1f,%s,%.2f,%.2f,%.2f,%d%n",
                     tick,
                     key,
                     i,
                     p.at.x,
                     p.at.y,
                     p.at.z,
                     p.motion,
                     p.contact ? 1 : 0,
                     p.asleep ? 1 : 0,
                     p.dev,
                     p.cap,
                     len,
                     n.x,
                     n.y,
                     n.z,
                     p.inside,
                     Double.isNaN(p.support) ? "" : String.format(Locale.ROOT, "%.3f", p.support),
                     clipMax,
                     stepMicros,
                     lod,
                     p.abs,
                     p.bend,
                     p.cone,
                     p.branch
                  )
               );
               csvRows++;
            }

            if (tick % 20 == 0) {
               csv.flush();
            }
         } catch (IOException e) {
            LOGGER.warn("[ChainWatch] the watch file failed; the panel keeps running", e);
            csv = null;
         }
      }
   }

   static boolean reversal(ChainWatch.Step[] s, int k, int i) {
      if (k >= 2 && i < s[k].particles.length && i < s[k - 1].particles.length && i < s[k - 2].particles.length) {
         double a = s[k - 1].particles[i].at.y - s[k - 2].particles[i].at.y;
         double b = s[k].particles[i].at.y - s[k - 1].particles[i].at.y;
         return Math.abs(a) > 0.02 && Math.abs(b) > 0.02 && a * b < 0.0;
      } else {
         return false;
      }
   }

   static boolean flip(ChainWatch.Step[] s, int k, int i) {
      if (k >= 1 && i < s[k].particles.length && i < s[k - 1].particles.length) {
         Vec3 a = s[k - 1].particles[i].push;
         Vec3 b = s[k].particles[i].push;
         return a.length() > 0.005 && b.length() > 0.005 && a.dot(b) < 0.0;
      } else {
         return false;
      }
   }

   private static ChainWatch.Step[] last(ChainWatch.History h, int n) {
      int size = h.steps.size();
      int skip = Math.max(0, size - n);
      ChainWatch.Step[] out = new ChainWatch.Step[size - skip];
      Iterator<ChainWatch.Step> it = h.steps.iterator();

      for (int i = 0; i < skip; i++) {
         it.next();
      }

      for (int i = 0; i < out.length; i++) {
         out[i] = it.next();
      }

      return out;
   }

   static void drawTrails(int entityId) {
      if (watched == entityId) {
         for (ChainWatch.History h : CHAINS.values()) {
            ChainWatch.Step[] s = last(h, 40);

            for (int k = 1; k < s.length; k++) {
               int n = Math.min(s[k].particles.length, s[k - 1].particles.length);

               for (int i = 1; i < n; i++) {
                  Vec3 a = s[k - 1].particles[i].at;
                  Vec3 b = s[k].particles[i].at;
                  int c = colour(s, k, i);
                  LegReachDebugRenderer.line(
                     entityId,
                     a.x,
                     a.y,
                     a.z,
                     b.x,
                     b.y,
                     b.z,
                     (c >> 16 & 0xFF) / 255.0F,
                     (c >> 8 & 0xFF) / 255.0F,
                     (c & 0xFF) / 255.0F
                  );
               }
            }
         }
      }
   }

   static int colour(ChainWatch.Step[] s, int k, int i) {
      ChainWatch.Particle p = s[k].particles[i];
      if (reversal(s, k, i)) {
         return 16724016;
      } else if (flip(s, k, i)) {
         return 16769072;
      } else if (p.inside > 0.05) {
         return 16728319;
      } else if (p.asleep) {
         return 4227327;
      } else {
         return p.contact ? 16751152 : 10132122;
      }
   }

   public static void onRenderGui(GuiGraphics g, float partialTick) {
      if (watched != Integer.MIN_VALUE) {
         Minecraft mc = Minecraft.getInstance();
         Font font = mc.font;
         int lineH = 9;
         int traceW = 120;
         int labelW = font.width("00 ");
         int numsW = font.width(" 000 000 0.00 000/00 0.00");
         int panelW = Math.max(labelW + traceW + numsW + 8, 330);
         int x0 = g.guiWidth() - panelW - 4;
         int y = 4;
         int rows = 2;

         for (ChainWatch.History h : CHAINS.values()) {
            rows += 4 + (h.steps.isEmpty() ? 0 : h.steps.peekLast().particles.length - 1);
         }

         g.fill(x0 - 2, y - 2, x0 + panelW + 2, y + rows * lineH + 2, -1072689136);
         g.drawString(font, "CHAIN WATCH  " + watchedName + "  LOD " + rung, x0, y, -12192, false);
         y += lineH;
         g.drawString(font, "last 10 s  |  /rig chain watch off  |  " + csvRows + " rows to file", x0, y, -7303024, false);
         y += lineH;
         if (CHAINS.isEmpty()) {
            g.drawString(font, "no chain has stepped yet (is it in view and inside the chain LOD?)", x0, y, -32640, false);
         } else {
            for (Entry<String, ChainWatch.History> e : CHAINS.entrySet()) {
               ChainWatch.History h = e.getValue();
               ChainWatch.Stats st = ChainWatch.Stats.of(h, 200);
               y += 2;
               g.drawString(font, e.getKey(), x0, y, -1, false);
               String head = String.format(Locale.ROOT, "bounce %d  flips %d  move<=%.2f  step %.0fus", st.bounces, st.flips, st.maxMove, st.stepMicros);
               g.drawString(font, head, x0 + font.width(e.getKey()) + 6, y, st.bounces + st.flips > 0 ? -40864 : -8323200, false);
               y += lineH;
               String tail = String.format(
                  Locale.ROOT,
                  "overcap %.0f%% (+%.0f)  inside %.0f%% (%.2f)  sleep %.0f%%  clip<=%.2f",
                  st.overShare * 100.0,
                  st.overMax,
                  st.insideShare * 100.0,
                  st.insideMax,
                  st.sleepShare * 100.0,
                  st.clipMax
               );
               g.drawString(font, tail, x0, y, st.overShare + st.insideShare > 0.01 ? -20384 : -5197648, false);
               y += lineH;
               g.drawString(font, " #", x0, y, -9408400, false);
               g.drawString(font, "height, last 6 s", x0 + labelW, y, -9408400, false);
               g.drawString(font, " bnc flp move dev/cap  in", x0 + labelW + traceW + 2, y, -9408400, false);
               y += lineH;
               ChainWatch.Step[] s = last(h, Math.max(120, 200) + 2);
               int n = s.length == 0 ? 0 : s[s.length - 1].particles.length;

               for (int i = 1; i < n; i++) {
                  drawRow(g, font, s, i, x0, y, labelW, traceW, lineH);
                  y += lineH;
               }
            }
         }
      }
   }

   private static void drawRow(GuiGraphics g, Font font, ChainWatch.Step[] s, int i, int x0, int y, int labelW, int traceW, int lineH) {
      g.drawString(font, String.format(Locale.ROOT, "%2d", i), x0, y, -5197648, false);
      int tx = x0 + labelW;
      g.fill(tx, y, tx + traceW, y + lineH - 1, 1090519039);
      int from = Math.max(0, s.length - traceW);
      double mean = 0.0;
      int count = 0;

      for (int k = from; k < s.length; k++) {
         if (i < s[k].particles.length) {
            mean += s[k].particles[i].at.y;
            count++;
         }
      }

      mean = count == 0 ? 0.0 : mean / count;
      int mid = y + (lineH - 1) / 2;
      int half = (lineH - 1) / 2;

      for (int k = from; k < s.length; k++) {
         if (i < s[k].particles.length) {
            double dy = (s[k].particles[i].at.y - mean) * 16.0;
            int py = mid - (int)Math.round(Math.max(-half, Math.min(half, dy)));
            int px = tx + (k - from);
            g.fill(px, py, px + 1, py + 1, 0xFF000000 | colour(s, k, i));
         }
      }

      int bounces = 0;
      int flips = 0;
      int over = 0;
      int in = 0;
      double move = 0.0;
      double dev = 0.0;
      double cap = 0.0;
      int start = Math.max(0, s.length - 200);

      for (int k = start; k < s.length; k++) {
         if (i < s[k].particles.length) {
            ChainWatch.Particle p = s[k].particles[i];
            if (reversal(s, k, i)) {
               bounces++;
            }

            if (flip(s, k, i)) {
               flips++;
            }

            if (p.dev - p.cap > 1.0) {
               over++;
            }

            if (p.inside > 0.05) {
               in++;
            }

            move = Math.max(move, p.motion);
         }
      }

      ChainWatch.Particle now = s[s.length - 1].particles.length > i ? s[s.length - 1].particles[i] : null;
      if (now != null) {
         dev = now.dev;
         cap = now.cap;
      }

      String nums = String.format(Locale.ROOT, " %3d %3d %4.2f %3.0f/%2.0f %4.2f", bounces, flips, move, dev, cap, now == null ? 0.0 : now.inside);
      int colour = bounces + flips > 0 ? -40864 : (over + in > 0 ? -20384 : -5197648);
      g.drawString(font, nums, tx + traceW + 2, y, colour, false);
   }

   static final class History {
      final ArrayDeque<ChainWatch.Step> steps = new ArrayDeque<>();

      void add(ChainWatch.Step s) {
         this.steps.addLast(s);

         while (this.steps.size() > 200) {
            this.steps.removeFirst();
         }
      }
   }

   record Particle(
      Vec3 at,
      double motion,
      boolean contact,
      boolean asleep,
      double dev,
      double cap,
      Vec3 push,
      double inside,
      double support,
      double abs,
      double bend,
      double cone,
      int branch
   ) {
   }

   record Stats(
      int steps,
      int bounces,
      int flips,
      double maxMove,
      double overShare,
      double overMax,
      double insideShare,
      double insideMax,
      double sleepShare,
      double clipMax,
      double stepMicros
   ) {
      static ChainWatch.Stats of(ChainWatch.History h, int span) {
         int n = Math.min(span, h.steps.size());
         if (n == 0) {
            return new ChainWatch.Stats(0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
         }

         ChainWatch.Step[] s = ChainWatch.last(h, n + 2);
         int from = s.length - n;
         int bounces = 0;
         int flips = 0;
         int over = 0;
         int in = 0;
         int sleep = 0;
         int cells = 0;
         double maxMove = 0.0;
         double overMax = 0.0;
         double inMax = 0.0;
         double clip = 0.0;
         double micros = 0.0;

         for (int k = from; k < s.length; k++) {
            ChainWatch.Step st = s[k];
            clip = Math.max(clip, st.clipMax);
            micros += st.stepMicros;

            for (int i = 1; i < st.particles.length; i++) {
               ChainWatch.Particle p = st.particles[i];
               cells++;
               maxMove = Math.max(maxMove, p.motion);
               double excess = p.dev - p.cap;
               if (excess > 1.0) {
                  over++;
               }

               overMax = Math.max(overMax, excess);
               if (p.inside > 0.05) {
                  in++;
               }

               inMax = Math.max(inMax, p.inside);
               if (p.asleep) {
                  sleep++;
               }

               if (ChainWatch.reversal(s, k, i)) {
                  bounces++;
               }

               if (ChainWatch.flip(s, k, i)) {
                  flips++;
               }
            }
         }

         return new ChainWatch.Stats(
            n,
            bounces,
            flips,
            maxMove,
            (double)over / Math.max(1, cells),
            Math.max(0.0, overMax),
            (double)in / Math.max(1, cells),
            inMax,
            (double)sleep / Math.max(1, cells),
            clip,
            micros / n
         );
      }

      String line() {
         return String.format(
            Locale.ROOT,
            "bounce %d  flips %d  move<=%.2f  overcap %.0f%% (+%.0f deg)  inside %.0f%% (%.2f)  sleep %.0f%%  clip<=%.2f  step %.0fus",
            this.bounces,
            this.flips,
            this.maxMove,
            this.overShare * 100.0,
            this.overMax,
            this.insideShare * 100.0,
            this.insideMax,
            this.sleepShare * 100.0,
            this.clipMax,
            this.stepMicros
         );
      }
   }

   record Step(int tick, ChainWatch.Particle[] particles, double clipMax, double stepMicros) {
   }
}
