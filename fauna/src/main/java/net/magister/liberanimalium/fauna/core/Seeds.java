package net.magister.liberanimalium.fauna.core;

import java.util.UUID;

public final class Seeds {
   private Seeds() {
   }

   public static int mix(UUID id, long salt) {
      long wide = id.getMostSignificantBits();
      wide ^= id.getLeastSignificantBits() * 2654435769L;
      wide ^= salt * 2654435769L;
      int h = (int)(wide ^ wide >>> 32);
      h ^= h >>> 15;
      h *= -2048144789;
      h ^= h >>> 13;
      return h & 2147483647;
   }

   public static long mix(long z) {
      z = (z ^ z >>> 30) * -4658895280553007687L;
      z = (z ^ z >>> 27) * -7723592293110705685L;
      return z ^ z >>> 31;
   }

   public static float unit(UUID id, long salt) {
      return mix(id, salt) % 100000 / 100000.0F;
   }

   public static float unitPair(UUID a, UUID b, long salt) {
      UUID lo = a.compareTo(b) <= 0 ? a : b;
      UUID hi = a.compareTo(b) <= 0 ? b : a;
      return unit(lo, mix(hi, salt));
   }
}
