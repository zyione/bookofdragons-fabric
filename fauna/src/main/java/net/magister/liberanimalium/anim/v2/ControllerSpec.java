package net.magister.liberanimalium.anim.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.magister.liberanimalium.anim.binding.AnimationPredicate;

public record ControllerSpec(String layer, String initial, AnimationPredicate activeWhen, List<ControllerSpec.Transition> any, Map<String, StateSpec> states) {
   public static ControllerSpec parse(String layer, JsonObject json, Predicate<String> isScalarKey) {
      AnimationPredicate activeWhen = json.has("active_when")
         ? AnimationPredicate.parse(json.getAsJsonObject("active_when"), isScalarKey)
         : AnimationPredicate.ALWAYS;
      Map<String, StateSpec> states = new LinkedHashMap<>();
      if (json.has("states") && json.get("states").isJsonObject()) {
         for (Entry<String, JsonElement> entry : json.getAsJsonObject("states").entrySet()) {
            if (entry.getValue().isJsonObject()) {
               states.put(entry.getKey(), StateSpec.parse(entry.getKey(), entry.getValue().getAsJsonObject(), isScalarKey));
            }
         }
      }

      List<ControllerSpec.Transition> any = new ArrayList<>();
      if (json.has("any") && json.get("any").isJsonArray()) {
         for (JsonElement el : json.getAsJsonArray("any")) {
            if (el.isJsonObject()) {
               JsonObject step = el.getAsJsonObject();
               if (step.has("to")) {
                  AnimationPredicate when = step.has("when") ? AnimationPredicate.parse(step.getAsJsonObject("when"), isScalarKey) : AnimationPredicate.ALWAYS;
                  boolean edge = step.has("edge") && step.get("edge").getAsBoolean();
                  any.add(new ControllerSpec.Transition(step.get("to").getAsString(), when, edge));
               }
            }
         }
      }

      String initial = json.has("initial") ? json.get("initial").getAsString() : (states.isEmpty() ? null : states.keySet().iterator().next());
      return new ControllerSpec(layer, initial, activeWhen, List.copyOf(any), Map.copyOf(states));
   }

   public StateSpec state(String name) {
      return this.states.get(name);
   }

   public record Transition(String to, AnimationPredicate when, boolean edge) {
   }
}
