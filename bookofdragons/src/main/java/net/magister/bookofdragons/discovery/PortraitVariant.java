package net.magister.bookofdragons.discovery;

import net.minecraft.util.StringRepresentable;

public enum PortraitVariant implements StringRepresentable {
   UNDISCOVERED("undiscovered"),
   TAMED("tamed");

   private final String name;

   PortraitVariant(String name) {
      this.name = name;
   }

   public String getSerializedName() {
      return this.name;
   }

   public static PortraitVariant fromString(String name) {
      for (PortraitVariant variant : values()) {
         if (variant.name.equals(name)) {
            return variant;
         }
      }

      return UNDISCOVERED;
   }

   public static PortraitVariant getDisplayVariant(boolean hasTamed) {
      return hasTamed ? TAMED : UNDISCOVERED;
   }
}
