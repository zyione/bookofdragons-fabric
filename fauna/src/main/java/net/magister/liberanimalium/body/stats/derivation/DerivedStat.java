package net.magister.liberanimalium.body.stats.derivation;

import java.util.HashMap;
import java.util.Map;

public enum DerivedStat {
   HEALTH("health"),
   ARMOR("armor"),
   REGEN("regen"),
   METABOLISM("metabolism"),
   AIR_SPEED("air_speed"),
   GROUND_SPEED("ground_speed"),
   WATER_SPEED("water_speed"),
   ACCELERATION("acceleration"),
   AGILITY("agility"),
   GROUND_TURN("ground_turn"),
   MELEE_DAMAGE("melee_damage"),
   KNOCKBACK_RESIST("knockback_resist"),
   BREATH_POWER("breath_power"),
   BREATH_COST("breath_cost"),
   STAMINA("stamina"),
   STAMINA_REGEN("stamina_regen");

   private final String jsonKey;
   private static final Map<String, DerivedStat> BY_KEY = new HashMap<>();

   DerivedStat(String jsonKey) {
      this.jsonKey = jsonKey;
   }

   public String jsonKey() {
      return this.jsonKey;
   }

   public static DerivedStat byKey(String key) {
      return BY_KEY.get(key);
   }

   static {
      for (DerivedStat s : values()) {
         BY_KEY.put(s.jsonKey, s);
      }
   }
}
