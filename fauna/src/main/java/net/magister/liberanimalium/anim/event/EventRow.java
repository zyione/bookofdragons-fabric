package net.magister.liberanimalium.anim.event;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public record EventRow(
   String layer,
   String play,
   String clip,
   boolean optional,
   String sound,
   String particle,
   int cooldown,
   int repeatEvery,
   int particleCount,
   double particleSpread,
   double particleRise,
   List<String> bones,
   int linger,
   int wash
) {
   public static final int DEFAULT_PARTICLE_COUNT = 6;
   public static final double DEFAULT_PARTICLE_SPREAD = 0.35;
   public static final double DEFAULT_PARTICLE_RISE = 0.02;

   public static EventRow parse(JsonObject json) {
      return new EventRow(
         json.has("layer") ? json.get("layer").getAsString() : null,
         json.has("play") ? json.get("play").getAsString() : null,
         json.has("clip") ? json.get("clip").getAsString() : null,
         json.has("optional") && json.get("optional").getAsBoolean(),
         json.has("sound") ? json.get("sound").getAsString() : null,
         json.has("particle") ? json.get("particle").getAsString() : null,
         json.has("cooldown") ? json.get("cooldown").getAsInt() : 0,
         json.has("repeat_every") ? json.get("repeat_every").getAsInt() : 0,
         json.has("particle_count") ? json.get("particle_count").getAsInt() : 6,
         json.has("particle_spread") ? json.get("particle_spread").getAsDouble() : 0.35,
         json.has("particle_rise") ? json.get("particle_rise").getAsDouble() : 0.02,
         bones(json),
         json.has("linger") ? json.get("linger").getAsInt() : 0,
         json.has("wash") ? json.get("wash").getAsInt() : 0
      );
   }

   private static List<String> bones(JsonObject json) {
      if (json.has("bone") && json.get("bone").isJsonPrimitive()) {
         return List.of(json.get("bone").getAsString());
      }

      if (json.has("bones") && json.get("bones").isJsonArray()) {
         List<String> out = new ArrayList<>();

         for (JsonElement el : json.getAsJsonArray("bones")) {
            if (el.isJsonPrimitive()) {
               out.add(el.getAsString());
            }
         }

         return List.copyOf(out);
      } else {
         return List.of();
      }
   }

   public boolean lingers() {
      return this.linger > 0 && this.repeatEvery > 0;
   }

   public String resolveClip(String speciesPrefix) {
      if (this.clip != null) {
         return this.clip;
      } else {
         return this.play != null ? speciesPrefix + this.play : null;
      }
   }

   public boolean isEmpty() {
      return this.play == null && this.clip == null && this.sound == null && this.particle == null;
   }
}
