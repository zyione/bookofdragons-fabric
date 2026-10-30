package net.magister.liberanimalium.render.membrane;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.magister.liberanimalium.chassis.FaunaBase;
import org.joml.Vector3f;

public final class MembraneFlex {
   private static final float CREASE_FRAC = 0.075F;
   private static final float FLIGHT_FRAC_MULT = 2.0F;
   private static final float BASELINE_FRAC = 0.035F;
   private static final float TURB_FREQ = 0.35F;
   private static final float TURB_SPEED_REF = 0.8F;
   private static final float TURB_FREQ_EXP = 2.2F;
   private static final float TURB_FREQ_SPEED_CAP = 3.0F;
   private static final Map<UUID, float[]> TURB_PHASE = new ConcurrentHashMap<>();

   private MembraneFlex() {
   }

   public static MembraneFlex.FrameParams beginFrame(FaunaBase body, float partialTick) {
      boolean flying = body.isFlying();
      float frac = flying ? 0.15F : 0.075F;
      float now = body.tickCount + partialTick;
      float[] st = TURB_PHASE.computeIfAbsent(body.getUUID(), k -> new float[]{0.0F, now});
      float dt = Math.max(0.0F, now - st[1]);
      st[1] = now;
      float turbAmpFrac = 0.0F;
      if (flying && !body.isAccelerating()) {
         float sn = (float)body.getDeltaMovement().length() / 0.8F;
         turbAmpFrac = 0.035F * (float)Math.pow(Math.min(1.0F, sn), 2.2F);
         float freqMul = (float)Math.pow(Math.min(sn, 3.0F), 2.2F);
         st[0] += 0.35F * freqMul * dt;
      }

      return new MembraneFlex.FrameParams(frac, turbAmpFrac, st[0]);
   }

   public static float creaseHeight(MembranePanel panel, Map<String, Vector3f> positions, Vector3f p1, Vector3f p2, Vector3f p4, MembraneFlex.FrameParams f) {
      float fshaft = p1.distance(p2);
      float fouter = p1.distance(p4);
      if (panel.foldRef() != null) {
         Vector3f fa = positions.get(panel.foldRef()[0]);
         Vector3f fb = positions.get(panel.foldRef()[1]);
         Vector3f fd = positions.get(panel.foldRef()[2]);
         if (fa != null && fb != null && fd != null) {
            fshaft = fa.distance(fb);
            fouter = fa.distance(fd);
         }
      }

      float fold = 0.0F;
      if (fshaft > 1.0E-4F && panel.restRatio() > 1.0E-4F) {
         fold = Math.max(0.0F, Math.min(1.0F, 1.0F - fouter / fshaft / panel.restRatio()));
      }

      float steadyH = fold * f.frac() * fshaft;
      int h = panel.corner()[0].hashCode();
      float ph0 = (h & 65535) / 65536.0F * (float) (Math.PI * 2);
      float fj = 0.75F + (h >>> 16 & 65535) / 65536.0F * 0.5F;
      float phase = f.basePhase() * fj + ph0;
      float osc = (float)(Math.sin(phase) + 0.45 * Math.sin(phase * 2.3 + ph0 * 1.7)) / 1.45F;
      float ampH = f.turbAmpFrac() * (1.0F - fold) * fshaft;
      return steadyH + ampH * osc;
   }

   public static void forget(UUID id) {
      TURB_PHASE.remove(id);
   }

   public static void clear() {
      TURB_PHASE.clear();
   }

   public record FrameParams(float frac, float turbAmpFrac, float basePhase) {
   }
}
