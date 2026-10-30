package net.magister.liberanimalium.anim.v2;

import com.google.gson.JsonObject;
import java.util.function.Predicate;
import net.magister.liberanimalium.anim.binding.AnimationPredicate;

public record PoolEntry(String play, String clip, boolean optional, AnimationPredicate when, int weight, int cooldown) {
   public static PoolEntry parse(JsonObject json, Predicate<String> isScalarKey) {
      return new PoolEntry(
         json.has("play") ? json.get("play").getAsString() : null,
         json.has("clip") ? json.get("clip").getAsString() : null,
         json.has("optional") && json.get("optional").getAsBoolean(),
         json.has("when") ? AnimationPredicate.parse(json.getAsJsonObject("when"), isScalarKey) : AnimationPredicate.ALWAYS,
         json.has("weight") ? Math.max(1, json.get("weight").getAsInt()) : 1,
         json.has("cooldown") ? json.get("cooldown").getAsInt() : 0
      );
   }

   public String resolveClip(String speciesPrefix) {
      if (this.clip != null) {
         return this.clip.contains(".") ? this.clip : speciesPrefix + this.clip;
      } else {
         return this.play != null ? speciesPrefix + this.play : null;
      }
   }
}
