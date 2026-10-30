package net.magister.liberanimalium.genetics;

import java.util.List;
import java.util.Objects;

public final class PaletteCoord {
   private static final List<String> HUE_RING = List.of("red", "orange", "yellow", "green", "blue", "purple");
   public static final int NEUTRAL_PENALTY = 3;
   public static final int HUE_PENALTY_MULTIPLIER = 2;
   public static final String NEUTRAL = "neutral";
   private final String primary;
   private final int sat;
   private final int temp;
   private final int light;

   public PaletteCoord(String primary, int sat, int temp, int light) {
      this.primary = Objects.requireNonNull(primary, "Primary hue cannot be null").toLowerCase();
      this.sat = sat;
      this.temp = temp;
      this.light = light;
   }

   public static int hueDistance(String primary1, String primary2) {
      if (!"neutral".equals(primary1) && !"neutral".equals(primary2)) {
         int idx1 = HUE_RING.indexOf(primary1);
         int idx2 = HUE_RING.indexOf(primary2);
         if (idx1 >= 0 && idx2 >= 0) {
            int size = HUE_RING.size();
            int clockwise = (idx2 - idx1 + size) % size;
            int counterclockwise = (idx1 - idx2 + size) % size;
            return Math.min(clockwise, counterclockwise);
         } else {
            return 3;
         }
      } else {
         return 3;
      }
   }

   public int tierDistance(PaletteCoord other) {
      int satDiff = Math.abs(this.sat - other.sat);
      int tempDiff = Math.abs(this.temp - other.temp);
      int lightDiff = Math.abs(this.light - other.light);
      int huePenalty = hueDistance(this.primary, other.primary) * 2;
      return Math.max(Math.max(satDiff, tempDiff), lightDiff) + huePenalty;
   }

   public int fineDistance(PaletteCoord other) {
      int satDiff = Math.abs(this.sat - other.sat);
      int tempDiff = Math.abs(this.temp - other.temp);
      int lightDiff = Math.abs(this.light - other.light);
      int huePenalty = hueDistance(this.primary, other.primary) * 2;
      return satDiff + tempDiff + lightDiff + huePenalty;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof PaletteCoord that)
            ? false
            : this.sat == that.sat && this.temp == that.temp && this.light == that.light && this.primary.equals(that.primary);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.primary, this.sat, this.temp, this.light);
   }

   @Override
   public String toString() {
      return "PaletteCoord{" + this.primary + ", sat=" + this.sat + ", temp=" + this.temp + ", light=" + this.light + "}";
   }
}
