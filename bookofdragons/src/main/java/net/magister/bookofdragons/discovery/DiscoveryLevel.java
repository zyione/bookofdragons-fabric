package net.magister.bookofdragons.discovery;

import net.minecraft.util.StringRepresentable;

public enum DiscoveryLevel implements StringRepresentable {
   UNDISCOVERED(-1, "undiscovered"),
   ENCOUNTERED(1, "encountered"),
   KILLED(2, "killed"),
   TAMED(2, "tamed"),
   BOTH_KILLED_AND_TAMED(2, "both_killed_and_tamed"),
   BONDED(3, "bonded");

   private final int tier;
   private final String name;

   DiscoveryLevel(int tier, String name) {
      this.tier = tier;
      this.name = name;
   }

   public int getTier() {
      return this.tier;
   }

   public String getSerializedName() {
      return this.name;
   }

   public static DiscoveryLevel fromString(String name) {
      for (DiscoveryLevel level : values()) {
         if (level.name.equals(name)) {
            return level;
         }
      }

      return UNDISCOVERED;
   }

   public static DiscoveryLevel fromFlags(boolean hasKilled, boolean hasTamed, boolean isBonded) {
      if (isBonded && hasTamed) {
         return BONDED;
      } else if (hasKilled && hasTamed) {
         return BOTH_KILLED_AND_TAMED;
      } else if (hasTamed) {
         return TAMED;
      } else {
         return hasKilled ? KILLED : UNDISCOVERED;
      }
   }
}
