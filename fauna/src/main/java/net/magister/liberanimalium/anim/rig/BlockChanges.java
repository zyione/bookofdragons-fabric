package net.magister.liberanimalium.anim.rig;

public final class BlockChanges {
   static final int RING = 256;
   private static final long[] POS = new long[256];
   private static long count;

   private BlockChanges() {
   }

   public static void record(int x, int y, int z) {
      POS[(int)(count & 255L)] = pack(x, y, z);
      count++;
   }

   public static void reset() {
      count += 257L;
   }

   public static long mark() {
      return count;
   }

   public static boolean changedSince(long mark, int x0, int y0, int z0, int x1, int y1, int z1) {
      long since = count - mark;
      if (since <= 0L) {
         return false;
      }

      if (since > 256L) {
         return true;
      }

      for (long k = mark; k < count; k++) {
         long p = POS[(int)(k & 255L)];
         int x = unpackX(p);
         int y = unpackY(p);
         int z = unpackZ(p);
         if (x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1) {
            return true;
         }
      }

      return false;
   }

   private static long pack(int x, int y, int z) {
      return (x & 67108863L) << 38 | y & 4095L | (z & 67108863L) << 12;
   }

   private static int unpackX(long p) {
      return (int)(p << 0 >> 38);
   }

   private static int unpackY(long p) {
      return (int)(p << 52 >> 52);
   }

   private static int unpackZ(long p) {
      return (int)(p << 26 >> 38);
   }
}
