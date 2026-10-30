package net.magister.liberanimalium.anim.validate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.magister.liberanimalium.anim.molang.Expression;
import net.magister.liberanimalium.anim.rig.ChainSpec;
import net.magister.liberanimalium.anim.rig.StepsSpec;
import net.magister.liberanimalium.anim.rig.SwimSpec;
import net.magister.liberanimalium.body.BodyPlan;
import net.magister.liberanimalium.fauna.api.ExpressionKey;
import net.magister.liberanimalium.fauna.core.ExpressionSpace;

public final class BindingValidator {
   private static final int NEAR_MISSES = 3;

   private BindingValidator() {
   }

   public static BindingReport validate(
      String species,
      JsonObject root,
      Predicate<String> clipExists,
      Predicate<String> signalExists,
      Predicate<String> voiceExists,
      Predicate<String> passExists,
      Predicate<String> scalarExists,
      Predicate<String> easingExists
   ) {
      BindingReport.Builder report = new BindingReport.Builder(species);
      if (root == null) {
         report.problem("the file could not be parsed at all");
         return report.build();
      }

      String prefix = "animation." + species + ".";
      Set<String> declared = declaredLayers(root);
      Predicate<String> layerExists = declared::contains;
      if (root.has("events") && root.get("events").isJsonObject()) {
         events(root.getAsJsonObject("events"), prefix, report, clipExists, layerExists, voiceExists);
      }

      for (String retired : new String[]{"slots", "expressions"}) {
         if (root.has(retired)) {
            report.problem("block '" + retired + "' is a v1 block and nothing reads it any more - move it into 'controllers' as a state machine");
         }
      }

      if (root.has("layers") && root.get("layers").isJsonArray()) {
         layers(root.getAsJsonArray("layers"), root, report, easingExists);
      }

      if (root.has("controllers") && root.get("controllers").isJsonObject()) {
         controllers(root.getAsJsonObject("controllers"), prefix, report, clipExists, signalExists, layerExists, scalarExists);
      }

      if (root.has("rig") && root.get("rig").isJsonObject()) {
         rig(root.getAsJsonObject("rig"), report, passExists);
      }

      if (root.has("steps") && root.get("steps").isJsonObject()) {
         report.checked();

         for (String problem : StepsSpec.parse(root.getAsJsonObject("steps"), scalarExists).problems()) {
            report.problem("steps: " + problem);
         }
      }

      return report.build();
   }

   private static void rig(JsonObject rig, BindingReport.Builder report, Predicate<String> passExists) {
      if (rig.has("passes") && rig.get("passes").isJsonArray()) {
         Set<String> seen = new LinkedHashSet<>();

         for (JsonElement el : rig.getAsJsonArray("passes")) {
            report.checked();
            if (!el.isJsonPrimitive()) {
               report.problem("a rig passes entry is not a name");
            } else {
               String pass = el.getAsString();
               if (!passExists.test(pass)) {
                  report.problem("rig pass " + quote(pass) + " is not a pass this build runs");
               }

               if (!seen.add(pass)) {
                  report.problem("rig pass " + quote(pass) + " is declared twice");
               }
            }
         }
      }

      report.checked();

      for (String problem : BodyPlan.parse(rig).problems()) {
         report.problem("rig body: " + problem);
      }

      if (rig.has("swim") && rig.get("swim").isJsonObject()) {
         report.checked();

         for (String problem : SwimSpec.problems(rig.getAsJsonObject("swim"))) {
            report.problem("rig swim: " + problem);
         }
      }

      if (rig.has("chain") && rig.get("chain").isJsonObject()) {
         report.checked();

         for (String problem : ChainSpec.parse(rig.getAsJsonObject("chain")).problems()) {
            report.problem("rig chain: " + problem);
         }
      }
   }

   private static void events(
      JsonObject events,
      String prefix,
      BindingReport.Builder report,
      Predicate<String> clipExists,
      Predicate<String> layerExists,
      Predicate<String> voiceExists
   ) {
      for (Entry<String, JsonElement> entry : events.entrySet()) {
         report.checked();
         String key = entry.getKey();
         if (!ExpressionSpace.contains(key)) {
            if (ExpressionKey.familyOf(key) == null) {
               report.problem("events key '" + key + "' names no family; expected one of " + familyWords());
            } else {
               report.problem("events key '" + key + "' is not a moment this engine can reach - did you mean " + ExpressionSpace.nearMisses(key, 3) + "?");
            }
         } else if (!entry.getValue().isJsonObject()) {
            report.problem("events key '" + key + "' must map to an object");
         } else {
            JsonObject row = entry.getValue().getAsJsonObject();
            if (row.has("layer") && !layerExists.test(row.get("layer").getAsString())) {
               report.problem("events key '" + key + "' plays on layer '" + row.get("layer").getAsString() + "', which this file does not declare");
            }

            if ((row.has("play") || row.has("clip")) && !row.has("layer")) {
               report.problem("events key '" + key + "' plays a clip and names no layer");
            }

            clip(row, prefix, "events key '" + key + "'", report, clipExists);
            voice(row, "events key '" + key + "'", report, voiceExists);
            if (!row.has("play") && !row.has("clip") && !row.has("sound") && !row.has("particle") && !row.has("wash")) {
               report.problem("events key '" + key + "' asks for nothing at all");
            }
         }
      }
   }

   public static Set<String> declaredLayers(JsonObject root) {
      Set<String> out = new LinkedHashSet<>();
      if (root != null && root.has("layers") && root.get("layers").isJsonArray()) {
         for (JsonElement el : root.getAsJsonArray("layers")) {
            if (el.isJsonObject() && el.getAsJsonObject().has("name")) {
               out.add(el.getAsJsonObject().get("name").getAsString());
            }
         }

         return out;
      } else {
         return out;
      }
   }

   private static void layers(JsonArray layers, JsonObject root, BindingReport.Builder report, Predicate<String> easingExists) {
      Set<String> seen = new LinkedHashSet<>();

      for (JsonElement el : layers) {
         if (el.isJsonObject() && el.getAsJsonObject().has("name")) {
            JsonObject entry = el.getAsJsonObject();
            String name = entry.get("name").getAsString();
            if (entry.has("transition") && (!entry.get("transition").isJsonPrimitive() || !entry.get("transition").getAsJsonPrimitive().isNumber())) {
               report.problem("layer " + quote(name) + " has a transition that is not a number of ticks");
            }

            if (entry.has("easing") && !entry.get("easing").isJsonNull()) {
               String easing = entry.get("easing").getAsString();
               if (!easingExists.test(easing)) {
                  report.problem("layer " + quote(name) + " names an easing AzureLib does not register: " + quote(easing));
               }
            }

            if (!seen.add(name)) {
               report.problem("layer " + quote(name) + " is declared twice");
            }
         } else {
            report.problem("a layers entry has no name");
         }
      }
   }

   private static void controllers(
      JsonObject controllers,
      String prefix,
      BindingReport.Builder report,
      Predicate<String> clipExists,
      Predicate<String> signalExists,
      Predicate<String> layerExists,
      Predicate<String> scalarExists
   ) {
      for (Entry<String, JsonElement> entry : controllers.entrySet()) {
         String layer = entry.getKey();
         if (!layerExists.test(layer)) {
            report.problem("controller " + quote(layer) + " is not a declared layer - the animator registers nothing by that name");
         }

         if (entry.getValue().isJsonObject()) {
            JsonObject spec = entry.getValue().getAsJsonObject();
            String where = "controller " + quote(layer);
            if (spec.has("active_when") && spec.get("active_when").isJsonObject()) {
               signals(spec.getAsJsonObject("active_when"), where + " active_when", report, signalExists);
            }

            Set<String> states = spec.has("states") && spec.get("states").isJsonObject() ? spec.getAsJsonObject("states").keySet() : Set.of();
            if (states.isEmpty()) {
               report.problem(where + " declares no states");
            } else {
               if (spec.has("initial") && !states.contains(spec.get("initial").getAsString())) {
                  report.problem(where + " starts in " + quote(spec.get("initial").getAsString()) + ", which is not one of its states");
               }

               if (spec.has("any") && spec.get("any").isJsonArray()) {
                  for (JsonElement el : spec.getAsJsonArray("any")) {
                     if (el.isJsonObject()) {
                        JsonObject rung = el.getAsJsonObject();
                        if (!rung.has("to")) {
                           report.problem(where + " has a ladder rung with no target");
                        } else {
                           target(rung.get("to").getAsString(), states, where + " ladder", report);
                           if (rung.has("when") && rung.get("when").isJsonObject()) {
                              signals(rung.getAsJsonObject("when"), where + " ladder", report, signalExists);
                           }
                        }
                     }
                  }
               }

               for (Entry<String, JsonElement> state : spec.getAsJsonObject("states").entrySet()) {
                  if (state.getValue().isJsonObject()) {
                     report.checked();
                     JsonObject body = state.getValue().getAsJsonObject();
                     String stateWhere = where + " state " + quote(state.getKey());
                     clip(body, prefix, stateWhere, report, clipExists);
                     animSpeed(body, stateWhere, report, scalarExists);
                     if (body.has("transitions") && body.get("transitions").isJsonObject()) {
                        for (Entry<String, JsonElement> t : body.getAsJsonObject("transitions").entrySet()) {
                           target(t.getKey(), states, stateWhere, report);
                           if (t.getValue().isJsonObject()) {
                              signals(t.getValue().getAsJsonObject(), stateWhere, report, signalExists);
                           }
                        }
                     }

                     for (String block : new String[]{"on_enter", "on_exit"}) {
                        if (body.has(block) && body.get(block).isJsonObject()) {
                           JsonObject event = body.getAsJsonObject(block);
                           if (!event.has("event")) {
                              report.problem(stateWhere + " has an empty " + block);
                           } else {
                              String key = event.get("event").getAsString();
                              if (!ExpressionSpace.contains(key)) {
                                 report.problem(
                                    stateWhere
                                       + " "
                                       + block
                                       + " fires "
                                       + quote(key)
                                       + ", which is not a moment this engine can reach - did you mean "
                                       + ExpressionSpace.nearMisses(key, 3)
                                       + "?"
                                 );
                              }
                           }
                        }
                     }

                     for (String suppressed : suppressList(body)) {
                        if (!layerExists.test(suppressed)) {
                           report.problem(stateWhere + " suppresses " + quote(suppressed) + ", which is not a registered controller");
                        }
                     }

                     speedFrom(body, layer, stateWhere, report, layerExists);
                     syncTo(body, layer, stateWhere, report, layerExists);
                  }
               }
            }
         }
      }
   }

   private static void animSpeed(JsonObject state, String where, BindingReport.Builder report, Predicate<String> scalarExists) {
      if (state.has("anim_speed")) {
         Expression expression = Expression.parse(state.get("anim_speed").getAsString());
         if (!expression.valid()) {
            report.problem(where + " anim_speed " + expression.error());
         } else {
            for (String query : expression.queries()) {
               if (!scalarExists.test(query)) {
                  report.problem(
                     where + " anim_speed reads " + quote("query." + query) + ", which is not a registered scalar signal - it will read zero forever"
                  );
               }
            }
         }
      }
   }

   private static void speedFrom(JsonObject state, String layer, String where, BindingReport.Builder report, Predicate<String> layerExists) {
      if (state.has("speed_from") && state.get("speed_from").isJsonPrimitive()) {
         String named = state.get("speed_from").getAsString();
         if (state.has("anim_speed")) {
            report.problem(
               where
                  + " borrows its rate from "
                  + quote(named)
                  + " and authors an anim_speed as well - two answers to how fast this clip plays, and the borrow is the one that wins"
            );
         }

         if (named.equals(layer)) {
            report.problem(where + " borrows its rate from its own layer " + quote(named) + " - name the layer it was authored to play against");
         } else if (!layerExists.test(named)) {
            report.problem(where + " borrows its rate from " + quote(named) + ", which is not a registered controller - it will play at 1.0 for ever");
         }
      }
   }

   private static void syncTo(JsonObject state, String layer, String where, BindingReport.Builder report, Predicate<String> layerExists) {
      if (state.has("sync_to") && state.get("sync_to").isJsonPrimitive()) {
         String named = state.get("sync_to").getAsString();
         if (named.equals(layer)) {
            report.problem(where + " syncs to its own layer " + quote(named) + " - name the layer it was authored to play against");
         } else if (!layerExists.test(named)) {
            report.problem(where + " syncs to " + quote(named) + ", which is not a registered controller - it will never restart");
         }
      }
   }

   private static void target(String to, Set<String> states, String where, BindingReport.Builder report) {
      if (!states.contains(to)) {
         report.problem(
            where + " transitions to " + quote(to) + ", which is not one of its states - the machine can never take it, and nothing at runtime would say so"
         );
      }
   }

   private static List<String> suppressList(JsonObject state) {
      if (state.has("suppress") && state.get("suppress").isJsonArray()) {
         List<String> out = new ArrayList<>();

         for (JsonElement el : state.getAsJsonArray("suppress")) {
            out.add(el.getAsString());
         }

         return out;
      } else {
         return List.of();
      }
   }

   private static String quote(String name) {
      return "'" + name + "'";
   }

   private static void voice(JsonObject row, String where, BindingReport.Builder report, Predicate<String> voiceExists) {
      if (row.has("sound")) {
         boolean optional = row.has("optional") && row.get("optional").getAsBoolean();
         if (!optional) {
            String name = row.get("sound").getAsString();
            if (name.indexOf(58) < 0) {
               if (!voiceExists.test(name)) {
                  report.problem(
                     where
                        + " names voice "
                        + quote(name)
                        + ", which this species' sound table does not hold - add it to the species' data/<host>/fauna/sounds table, or mark the row \"optional\": true"
                  );
               }
            }
         }
      }
   }

   private static void clip(JsonObject row, String prefix, String where, BindingReport.Builder report, Predicate<String> clipExists) {
      boolean optional = row.has("optional") && row.get("optional").getAsBoolean();
      String full = null;
      if (row.has("clip")) {
         String written = row.get("clip").getAsString();
         full = written.contains(".") ? written : prefix + written;
      } else if (row.has("play")) {
         full = prefix + row.get("play").getAsString();
      }

      if (full != null && !optional) {
         if (!clipExists.test(full)) {
            report.problem(where + " names clip '" + full + "', which this species does not define - add the clip, or mark the row \"optional\": true");
         }
      }
   }

   private static void signals(JsonObject when, String where, BindingReport.Builder report, Predicate<String> signalExists) {
      for (Entry<String, JsonElement> entry : when.entrySet()) {
         String key = entry.getKey();
         JsonElement value = entry.getValue();
         if (key.equals("not") && value.isJsonObject()) {
            signals(value.getAsJsonObject(), where + " (not)", report, signalExists);
         } else if (key.equals("scalar") && value.isJsonObject()) {
            for (String inner : value.getAsJsonObject().keySet()) {
               check(inner, where, report, signalExists);
            }
         } else {
            check(key.endsWith("In") ? key.substring(0, key.length() - 2) : key, where, report, signalExists);
         }
      }
   }

   private static void check(String signal, String where, BindingReport.Builder report, Predicate<String> signalExists) {
      if (!signalExists.test(signal)) {
         report.problem(where + " reads signal '" + signal + "', which nothing registers - the rule can never match");
      }
   }

   private static String familyWords() {
      StringBuilder out = new StringBuilder();

      for (ExpressionKey.Family family : ExpressionKey.Family.values()) {
         if (out.length() > 0) {
            out.append(", ");
         }

         out.append(family.word().toLowerCase(Locale.ROOT));
      }

      return out.toString();
   }
}
