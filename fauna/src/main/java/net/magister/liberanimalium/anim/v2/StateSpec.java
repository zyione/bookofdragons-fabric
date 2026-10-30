package net.magister.liberanimalium.anim.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import net.magister.liberanimalium.anim.binding.AnimationPredicate;
import net.magister.liberanimalium.anim.molang.Expression;

public record StateSpec(
   String name,
   String play,
   String clip,
   boolean optional,
   String behavior,
   Expression animSpeed,
   Expression animWeight,
   double weightIn,
   int weightOutTicks,
   boolean weightOnWake,
   String onEnterEvent,
   String onExitEvent,
   List<String> suppress,
   Map<String, AnimationPredicate> transitions,
   int dwellMin,
   int dwellMax,
   int cooldown,
   boolean exitOnClipEnd,
   String speedFrom,
   String syncTo,
   List<PoolEntry> pool,
   boolean steps,
   boolean stepsMoving,
   List<String> limbsOff
) {
   public static final double DEFAULT_WEIGHT_IN = 0.1;
   public static final int DEFAULT_WEIGHT_OUT_TICKS = 20;

   public boolean weighted() {
      return this.animWeight != null && this.animWeight.present();
   }

   public static StateSpec parse(String name, JsonObject json, Predicate<String> isScalarKey) {
      String play = json.has("play") ? json.get("play").getAsString() : null;
      String clip = json.has("clip") ? json.get("clip").getAsString() : null;
      boolean optional = json.has("optional") && json.get("optional").getAsBoolean();
      String behavior = json.has("behavior") ? json.get("behavior").getAsString() : "LOOP";
      Expression animSpeed = Expression.parse(json.has("anim_speed") ? json.get("anim_speed").getAsString() : null);
      Expression animWeight = Expression.parse(json.has("anim_weight") ? json.get("anim_weight").getAsString() : null);
      double weightIn = json.has("weight_in") ? json.get("weight_in").getAsDouble() : 0.1;
      int weightOutTicks = json.has("weight_out_ticks") ? json.get("weight_out_ticks").getAsInt() : 20;
      boolean weightOnWake = json.has("weight_on_wake") && json.get("weight_on_wake").getAsBoolean();
      String onEnter = event(json, "on_enter");
      String onExit = event(json, "on_exit");
      List<String> suppress = new ArrayList<>();
      if (json.has("suppress") && json.get("suppress").isJsonArray()) {
         for (JsonElement el : json.getAsJsonArray("suppress")) {
            suppress.add(el.getAsString());
         }
      }

      Map<String, AnimationPredicate> transitions = new LinkedHashMap<>();
      if (json.has("transitions") && json.get("transitions").isJsonObject()) {
         for (Entry<String, JsonElement> entry : json.getAsJsonObject("transitions").entrySet()) {
            if (entry.getValue().isJsonObject()) {
               transitions.put(entry.getKey(), AnimationPredicate.parse(entry.getValue().getAsJsonObject(), isScalarKey));
            }
         }
      }

      int dwellMin = 0;
      int dwellMax = 0;
      if (json.has("dwell") && json.get("dwell").isJsonObject()) {
         JsonObject dwell = json.getAsJsonObject("dwell");
         dwellMin = dwell.has("min") ? dwell.get("min").getAsInt() : 0;
         dwellMax = dwell.has("max") ? dwell.get("max").getAsInt() : dwellMin;
         if (dwellMax < dwellMin) {
            dwellMax = dwellMin;
         }
      }

      int cooldown = json.has("cooldown") ? json.get("cooldown").getAsInt() : 0;
      boolean exitOnClipEnd = json.has("exit_on_clip_end") && json.get("exit_on_clip_end").getAsBoolean();
      String speedFrom = json.has("speed_from") ? json.get("speed_from").getAsString() : null;
      String syncTo = json.has("sync_to") ? json.get("sync_to").getAsString() : null;
      boolean steps = json.has("steps") && json.get("steps").getAsBoolean();
      boolean stepsMoving = steps && json.has("steps_moving") && json.get("steps_moving").getAsBoolean();
      List<String> limbsOff = new ArrayList<>();
      if (json.has("limbs_off") && json.get("limbs_off").isJsonArray()) {
         for (JsonElement el : json.getAsJsonArray("limbs_off")) {
            limbsOff.add(el.getAsString());
         }
      }

      List<PoolEntry> pool = new ArrayList<>();
      if (json.has("pool") && json.get("pool").isJsonArray()) {
         for (JsonElement el : json.getAsJsonArray("pool")) {
            if (el.isJsonObject()) {
               pool.add(PoolEntry.parse(el.getAsJsonObject(), isScalarKey));
            }
         }
      }

      return new StateSpec(
         name,
         play,
         clip,
         optional,
         behavior,
         animSpeed,
         animWeight,
         weightIn,
         weightOutTicks,
         weightOnWake,
         onEnter,
         onExit,
         List.copyOf(suppress),
         Map.copyOf(transitions),
         dwellMin,
         dwellMax,
         cooldown,
         exitOnClipEnd,
         speedFrom,
         syncTo,
         List.copyOf(pool),
         steps,
         stepsMoving,
         List.copyOf(limbsOff)
      );
   }

   public boolean borrowsSpeed() {
      return this.speedFrom != null;
   }

   public boolean resyncing() {
      return this.syncTo != null;
   }

   public int rollDwell(IntUnaryOperator random) {
      return this.dwellMax <= this.dwellMin ? this.dwellMin : this.dwellMin + random.applyAsInt(this.dwellMax - this.dwellMin + 1);
   }

   private static String event(JsonObject json, String key) {
      if (json.has(key) && json.get(key).isJsonObject()) {
         JsonObject block = json.getAsJsonObject(key);
         return block.has("event") ? block.get("event").getAsString() : null;
      } else {
         return null;
      }
   }

   public String resolveClip(String speciesPrefix) {
      if (this.clip != null) {
         return this.clip.contains(".") ? this.clip : speciesPrefix + this.clip;
      } else {
         return this.play != null ? speciesPrefix + this.play : null;
      }
   }

   public boolean silent() {
      return this.play == null && this.clip == null && this.pool.isEmpty();
   }
}
