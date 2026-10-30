package net.magister.liberanimalium.body.stats.derivation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.magister.liberanimalium.body.stats.PrimaryAttribute;

public final class StatDerivationTable {
   private final Map<DerivedStat, StatCurve> curves = new EnumMap<>(DerivedStat.class);

   public StatDerivationTable(Map<DerivedStat, StatCurve> curves) {
      this.curves.putAll(curves);
   }

   public static StatDerivationTable parse(JsonObject derived) {
      Map<DerivedStat, StatCurve> map = new EnumMap<>(DerivedStat.class);

      for (Entry<String, JsonElement> entry : derived.entrySet()) {
         if (!entry.getKey().startsWith("__")) {
            DerivedStat key = DerivedStat.byKey(entry.getKey());
            if (key == null) {
               throw new JsonSyntaxException("unknown derived stat '" + entry.getKey() + "'");
            }

            map.put(key, StatCurve.parse(entry.getValue().getAsJsonObject()));
         }
      }

      List<DerivedStat> missing = new ArrayList<>();

      for (DerivedStat s : DerivedStat.values()) {
         if (!map.containsKey(s)) {
            missing.add(s);
         }
      }

      if (!missing.isEmpty()) {
         throw new JsonSyntaxException("stats is missing " + missing);
      } else {
         return new StatDerivationTable(map);
      }
   }

   public float evaluate(DerivedStat stat, Map<PrimaryAttribute, Float> effective) {
      StatCurve curve = this.curves.get(stat);
      if (curve == null) {
         throw new IllegalStateException("No derivation curve for " + stat.jsonKey());
      } else {
         return curve.evaluate(effective);
      }
   }
}
