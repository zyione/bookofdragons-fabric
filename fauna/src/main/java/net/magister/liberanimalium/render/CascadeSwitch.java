package net.magister.liberanimalium.render;

public enum CascadeSwitch {
   ON,
   OFF,
   GROUND;

   private static volatile CascadeSwitch mode = GROUND;

   public static CascadeSwitch get() {
      return mode;
   }

   public static void set(CascadeSwitch value) {
      mode = value == null ? GROUND : value;
   }
}
