package net.magister.liberanimalium.body.stats.derivation;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Set;
import net.magister.liberanimalium.body.stats.PrimaryAttribute;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

public record StatCurve(PrimaryAttribute stat, float at0, float at100) {
   private static final Set<String> KEYS = Set.of("stat", "at0", "at100");

   public float evaluate(Map<PrimaryAttribute, Float> effective) {
      float t = Mth.clamp(effective.getOrDefault(this.stat, 0.0F) / 100.0F, 0.0F, 1.0F);
      return Mth.lerp(t, this.at0, this.at100);
   }

   public static StatCurve parse(JsonObject obj) {
      for (String key : obj.keySet()) {
         if (!KEYS.contains(key)) {
            throw new JsonSyntaxException("stat curve key '" + key + "' is read by nothing");
         }
      }

      return new StatCurve(PrimaryAttribute.valueOf(GsonHelper.getAsString(obj, "stat")), GsonHelper.getAsFloat(obj, "at0"), GsonHelper.getAsFloat(obj, "at100"));
   }
}
