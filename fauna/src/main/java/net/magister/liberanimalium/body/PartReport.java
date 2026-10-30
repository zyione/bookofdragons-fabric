package net.magister.liberanimalium.body;

import java.util.List;
import net.minecraft.world.phys.Vec3;

public record PartReport(long tick, Vec3[] offsets) {
   public static short[] encode(List<Vec3> world, Vec3 body) {
      short[] out = new short[world.size() * 3];
      int i = 0;

      for (Vec3 point : world) {
         out[i++] = quantise(point.x - body.x);
         out[i++] = quantise(point.y - body.y);
         out[i++] = quantise(point.z - body.z);
      }

      return out;
   }

   private static short quantise(double blocks) {
      double mm = Math.round(blocks * 1000.0);
      return (short)Math.max(-32768.0, Math.min(32767.0, mm));
   }

   public static Vec3[] decode(short[] raw) {
      Vec3[] out = new Vec3[raw.length / 3];

      for (int i = 0; i < out.length; i++) {
         out[i] = new Vec3(raw[i * 3] / 1000.0, raw[i * 3 + 1] / 1000.0, raw[i * 3 + 2] / 1000.0);
      }

      return out;
   }

   public static boolean accept(short[] raw, int slots) {
      if (raw != null && raw.length == slots * 3) {
         double reach = 32000.0;
         double reachSqr = reach * reach;

         for (int i = 0; i < raw.length; i += 3) {
            double x = raw[i];
            double y = raw[i + 1];
            double z = raw[i + 2];
            if (x * x + y * y + z * z > reachSqr) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean stale(long now) {
      return now - this.tick > 5L;
   }

   public Vec3 world(int slot, Vec3 body) {
      return body.add(this.offsets[slot]);
   }
}
