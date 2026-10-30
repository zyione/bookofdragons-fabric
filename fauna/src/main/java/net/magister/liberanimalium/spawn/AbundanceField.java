package net.magister.liberanimalium.spawn;

public final class AbundanceField {
   private AbundanceField() {
   }

   public static double of(long seed, String kind, double x, double z, long gameTime, SpawnSettings.Abundance settings) {
      if (settings.floor() >= 1.0) {
         return 1.0;
      }

      double raw = noise(seed ^ kind.hashCode() * -7046029254386353131L, x / settings.regionBlocks(), z / settings.regionBlocks(), gameTime / ticks(settings));
      double t = (raw - 0.3) / 0.39999999999999997;
      t = Math.max(0.0, Math.min(1.0, t));
      return settings.floor() + (1.0 - settings.floor()) * t;
   }

   public static double tide(long seed, String host, long gameTime, SpawnSettings.Abundance settings) {
      if (settings.tideFloor() >= 1.0) {
         return 1.0;
      }

      double raw = noise(seed ^ host.hashCode() * -4417276706812531889L, 0.5, 0.5, gameTime / ticks(settings));
      return settings.tideFloor() + (1.0 - settings.tideFloor()) * raw;
   }

   private static double ticks(SpawnSettings.Abundance settings) {
      return settings.days() * 24000.0;
   }

   static double noise(long seed, double x, double y, double z) {
      long x0 = (long)Math.floor(x);
      long y0 = (long)Math.floor(y);
      long z0 = (long)Math.floor(z);
      double fx = smooth(x - x0);
      double fy = smooth(y - y0);
      double fz = smooth(z - z0);
      double c00 = lerp(fx, lattice(seed, x0, y0, z0), lattice(seed, x0 + 1L, y0, z0));
      double c10 = lerp(fx, lattice(seed, x0, y0 + 1L, z0), lattice(seed, x0 + 1L, y0 + 1L, z0));
      double c01 = lerp(fx, lattice(seed, x0, y0, z0 + 1L), lattice(seed, x0 + 1L, y0, z0 + 1L));
      double c11 = lerp(fx, lattice(seed, x0, y0 + 1L, z0 + 1L), lattice(seed, x0 + 1L, y0 + 1L, z0 + 1L));
      return lerp(fz, lerp(fy, c00, c10), lerp(fy, c01, c11));
   }

   private static double smooth(double t) {
      return t * t * (3.0 - 2.0 * t);
   }

   private static double lerp(double t, double a, double b) {
      return a + t * (b - a);
   }

   private static double lattice(long seed, long x, long y, long z) {
      long h = seed + x * 7146057691288625177L + y * 2232777461L + z * -7046029254386353131L;
      h = (h ^ h >>> 30) * -4658895280553007687L;
      h = (h ^ h >>> 27) * -7723592293110705685L;
      h ^= h >>> 31;
      return (h >>> 11) * 1.110223E-16F;
   }
}
