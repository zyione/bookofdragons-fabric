package net.magister.liberanimalium.body.anim.binding;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

public final class BindingSource {
   public static final String TEMPLATES = "animation_bindings/templates/";
   public static final String SWIM_PRESETS = "swim";
   private static final List<List<String>> CONTAINERS = List.of(
      List.of(),
      List.of("controllers"),
      List.of("controllers", "*"),
      List.of("controllers", "*", "states"),
      List.of("events"),
      List.of("bone_groups"),
      List.of("steps"),
      List.of("steps", "limbs"),
      List.of("rig"),
      List.of("rig", "chains"),
      List.of("rig", "chain"),
      List.of("params"),
      List.of("clips")
   );
   private static final Pattern PARAM = Pattern.compile("\\$([a-z_][a-z0-9_]*)");
   private static final int MAX_DEPTH = 8;

   private BindingSource() {
   }

   public static JsonObject resolve(JsonObject file, Function<String, JsonObject> templates, Supplier<BindingSource.Model> model, List<String> problems) {
      if (file != null && file.has("extends")) {
         Set<String> removed = new HashSet<>();
         JsonObject merged = chain(file, templates, removed, problems, new LinkedHashSet<>());
         JsonObject params = take(merged, "params");
         JsonObject clips = take(merged, "clips");
         if (merged.has("layers") && merged.get("layers").isJsonArray()) {
            JsonArray kept = new JsonArray();

            for (JsonElement layer : merged.getAsJsonArray("layers")) {
               String name = layer.isJsonObject() && layer.getAsJsonObject().has("name") ? layer.getAsJsonObject().get("name").getAsString() : null;
               if (name == null || !removed.contains(name)) {
                  kept.add(layer);
               }
            }

            merged.add("layers", kept);
         }

         if (merged.has("controllers") && merged.get("controllers").isJsonObject()) {
            for (Entry<String, JsonElement> c : merged.getAsJsonObject("controllers").entrySet()) {
               if (c.getValue().isJsonObject()) {
                  dropDangling(c.getValue().getAsJsonObject());
               }
            }
         }

         Set<String> missing = new LinkedHashSet<>();
         JsonObject out = substitute(merged, params, missing).getAsJsonObject();

         for (String name : missing) {
            problems.add("no param named $" + name);
         }

         rename(out, clips);
         swimPreset(out, templates, problems);
         BindingSource.Model m = model == null ? null : model.get();
         if (m != null) {
            derive(out, m);
         }

         return out;
      } else {
         return file;
      }
   }

   private static JsonObject chain(JsonObject file, Function<String, JsonObject> templates, Set<String> removed, List<String> problems, Set<String> seen) {
      if (!file.has("extends")) {
         return file.deepCopy();
      }

      String name = file.get("extends").getAsString();
      JsonObject over = file.deepCopy();
      over.remove("extends");
      if (seen.add(name) && seen.size() <= 8) {
         JsonObject template = templates.apply(name);
         if (template == null) {
            problems.add("no template " + name);
            return over;
         } else {
            JsonObject base = chain(template, templates, removed, problems, seen);
            return merge(base, over, new ArrayList<>(), removed);
         }
      } else {
         problems.add("template " + name + " extends itself");
         return over;
      }
   }

   private static JsonObject merge(JsonObject base, JsonObject over, List<String> path, Set<String> removed) {
      JsonObject out = new JsonObject();

      for (Entry<String, JsonElement> e : base.entrySet()) {
         String key = e.getKey();
         if (!over.has(key)) {
            out.add(key, e.getValue().deepCopy());
         } else {
            JsonElement o = over.get(key);
            List<String> at = with(path, key);
            if (o.isJsonNull()) {
               if (at.size() == 2 && at.get(0).equals("controllers")) {
                  removed.add(key);
               }
            } else if (o.isJsonObject() && e.getValue().isJsonObject() && container(at)) {
               out.add(key, merge(e.getValue().getAsJsonObject(), o.getAsJsonObject(), at, removed));
            } else {
               out.add(key, o.deepCopy());
            }
         }
      }

      for (Entry<String, JsonElement> e : over.entrySet()) {
         if (!base.has(e.getKey()) && !e.getValue().isJsonNull()) {
            out.add(e.getKey(), e.getValue().deepCopy());
         }
      }

      return out;
   }

   private static boolean container(List<String> path) {
      label30:
      for (List<String> c : CONTAINERS) {
         if (c.size() == path.size()) {
            for (int i = 0; i < c.size(); i++) {
               if (!c.get(i).equals("*") && !c.get(i).equals(path.get(i))) {
                  continue label30;
               }
            }

            return true;
         }
      }

      return false;
   }

   private static List<String> with(List<String> path, String key) {
      List<String> out = new ArrayList<>(path);
      out.add(key);
      return out;
   }

   private static void swimPreset(JsonObject file, Function<String, JsonObject> templates, List<String> problems) {
      JsonObject rig = file.has("rig") && file.get("rig").isJsonObject() ? file.getAsJsonObject("rig") : null;
      if (rig != null && rig.has("swim") && rig.get("swim").isJsonObject()) {
         JsonObject swim = rig.getAsJsonObject("swim");
         if (swim.has("preset")) {
            String name = swim.get("preset").getAsString();
            JsonObject sheet = templates.apply("swim");
            if (sheet != null && sheet.has(name) && sheet.get(name).isJsonObject()) {
               JsonObject over = swim.deepCopy();
               over.remove("preset");
               JsonObject preset = sheet.getAsJsonObject(name);
               JsonObject out = lay(preset, over);
               roles(out, preset, over);
               if (out.has("under")
                  && out.get("under").isJsonObject()
                  && preset.has("under")
                  && preset.get("under").isJsonObject()
                  && over.has("under")
                  && over.get("under").isJsonObject()) {
                  roles(out.getAsJsonObject("under"), preset.getAsJsonObject("under"), over.getAsJsonObject("under"));
               }

               rig.add("swim", out);
            } else {
               problems.add("no swim preset " + name);
            }
         }
      }
   }

   private static void roles(JsonObject out, JsonObject preset, JsonObject over) {
      if (over.has("roles") && over.get("roles").isJsonObject() && out.has("roles")) {
         JsonObject theirs = preset.has("roles") && preset.get("roles").isJsonObject() ? preset.getAsJsonObject("roles") : new JsonObject();
         JsonObject roles = out.getAsJsonObject("roles");

         for (Entry<String, JsonElement> e : over.getAsJsonObject("roles").entrySet()) {
            if (e.getValue().isJsonObject()) {
               JsonObject mine = e.getValue().getAsJsonObject();
               boolean scaleOnly = mine.size() == 1 && mine.has("scale");
               if (scaleOnly && theirs.has(e.getKey()) && theirs.get(e.getKey()).isJsonObject()) {
                  JsonObject scaled = theirs.getAsJsonObject(e.getKey()).deepCopy();
                  scaled.add("scale", mine.get("scale"));
                  roles.add(e.getKey(), scaled);
               } else {
                  roles.add(e.getKey(), mine.deepCopy());
               }
            }
         }
      }
   }

   static JsonObject lay(JsonObject base, JsonObject over) {
      JsonObject out = base.deepCopy();

      for (Entry<String, JsonElement> e : over.entrySet()) {
         JsonElement o = e.getValue();
         if (o.isJsonNull()) {
            out.remove(e.getKey());
         } else if (o.isJsonObject() && out.has(e.getKey()) && out.get(e.getKey()).isJsonObject()) {
            out.add(e.getKey(), lay(out.getAsJsonObject(e.getKey()), o.getAsJsonObject()));
         } else {
            out.add(e.getKey(), o.deepCopy());
         }
      }

      return out;
   }

   private static JsonObject take(JsonObject json, String key) {
      JsonElement e = json.remove(key);
      return e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
   }

   private static void dropDangling(JsonObject controller) {
      JsonObject states = controller.has("states") && controller.get("states").isJsonObject() ? controller.getAsJsonObject("states") : new JsonObject();
      if (controller.has("any") && controller.get("any").isJsonArray()) {
         JsonArray kept = new JsonArray();

         for (JsonElement row : controller.getAsJsonArray("any")) {
            if (!row.isJsonObject() || !row.getAsJsonObject().has("to") || states.has(row.getAsJsonObject().get("to").getAsString())) {
               kept.add(row);
            }
         }

         controller.add("any", kept);
      }

      for (Entry<String, JsonElement> s : states.entrySet()) {
         if (s.getValue().isJsonObject()) {
            JsonObject state = s.getValue().getAsJsonObject();
            if (state.has("transitions") && state.get("transitions").isJsonObject()) {
               JsonObject kept = new JsonObject();

               for (Entry<String, JsonElement> t : state.getAsJsonObject("transitions").entrySet()) {
                  if (states.has(t.getKey())) {
                     kept.add(t.getKey(), t.getValue());
                  }
               }

               state.add("transitions", kept);
            }
         }
      }
   }

   private static JsonElement substitute(JsonElement v, JsonObject params, Set<String> missing) {
      if (v.isJsonObject()) {
         JsonObject out = new JsonObject();

         for (Entry<String, JsonElement> e : v.getAsJsonObject().entrySet()) {
            out.add(e.getKey(), substitute(e.getValue(), params, missing));
         }

         return out;
      } else if (!v.isJsonArray()) {
         if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
            String text = v.getAsString();
            if (text.indexOf(36) < 0) {
               return v;
            }

            Matcher whole = PARAM.matcher(text);
            if (whole.matches()) {
               JsonElement value = params.get(whole.group(1));
               if (value == null) {
                  missing.add(whole.group(1));
                  return v;
               } else {
                  return value.deepCopy();
               }
            } else {
               Matcher m = PARAM.matcher(text);
               StringBuilder out = new StringBuilder();

               while (m.find()) {
                  JsonElement value = params.get(m.group(1));
                  if (value == null) {
                     missing.add(m.group(1));
                  }

                  m.appendReplacement(out, Matcher.quoteReplacement(value == null ? m.group() : value.getAsString()));
               }

               m.appendTail(out);
               return new JsonPrimitive(out.toString());
            }
         } else {
            return v;
         }
      } else {
         JsonArray out = new JsonArray();

         for (JsonElement e : v.getAsJsonArray()) {
            out.add(substitute(e, params, missing));
         }

         return out;
      }
   }

   private static void rename(JsonElement v, JsonObject clips) {
      if (clips.size() != 0) {
         if (v.isJsonObject()) {
            JsonObject o = v.getAsJsonObject();

            for (Entry<String, JsonElement> e : o.entrySet()) {
               JsonElement x = e.getValue();
               if (e.getKey().equals("play") && x.isJsonPrimitive() && clips.has(x.getAsString())) {
                  e.setValue(clips.get(x.getAsString()).deepCopy());
               } else {
                  rename(x, clips);
               }
            }
         } else if (v.isJsonArray()) {
            for (JsonElement e : v.getAsJsonArray()) {
               rename(e, clips);
            }
         }
      }
   }

   private static void derive(JsonObject file, BindingSource.Model model) {
      JsonObject steps = file.has("steps") && file.get("steps").isJsonObject() ? file.getAsJsonObject("steps") : null;
      if (steps != null && steps.has("body") && steps.get("body").isJsonObject()) {
         steps.add("body", BodyStyle.build(steps.getAsJsonObject("body"), file, model));
      }

      JsonObject rig = file.has("rig") && file.get("rig").isJsonObject() ? file.getAsJsonObject("rig") : null;
      if (rig != null) {
         if (rig.has("parts") && rig.get("parts").isJsonArray()) {
            parts(rig.getAsJsonArray("parts"), model);
         }

         if (passes(rig).contains("eyes")) {
            JsonObject eyes = rig.has("eyes") && rig.get("eyes").isJsonObject() ? rig.getAsJsonObject("eyes") : new JsonObject();
            if (!eyes.has("pupils")) {
               JsonArray pupils = new JsonArray();
               model.bones.stream().filter(n -> n.contains("pupil")).sorted().forEach(pupils::add);
               eyes.add("pupils", pupils);
            }

            rig.add("eyes", eyes);
         }

         JsonObject groups = file.has("bone_groups") && file.get("bone_groups").isJsonObject() ? file.getAsJsonObject("bone_groups") : null;
         if (groups != null && !groups.has("grip_limbs") && rig.has("grip") && rig.get("grip").isJsonObject()) {
            List<String> tops = new ArrayList<>();
            JsonObject grip = rig.getAsJsonObject("grip");
            if (grip.has("grippers") && grip.get("grippers").isJsonArray()) {
               for (JsonElement g : grip.getAsJsonArray("grippers")) {
                  if (g.isJsonObject()) {
                     JsonObject gripper = g.getAsJsonObject();
                     if (gripper.has("limb")) {
                        for (JsonElement b : gripper.getAsJsonArray("limb")) {
                           tops.add(b.getAsString());
                        }
                     }

                     if (gripper.has("bone")) {
                        tops.add(gripper.get("bone").getAsString());
                     }
                  }
               }
            }

            JsonArray allow = new JsonArray();

            for (String bone : model.bones) {
               if (tops.contains(bone) || tops.stream().anyMatch(t -> model.below(bone, t))) {
                  allow.add(bone);
               }
            }

            JsonObject group = new JsonObject();
            group.add("allow", allow);
            groups.add("grip_limbs", group);
         }
      }
   }

   private static void parts(JsonArray parts, BindingSource.Model model) {
      List<String> bones = new ArrayList<>();

      for (JsonElement p : parts) {
         bones.add(p.isJsonObject() && p.getAsJsonObject().has("bone") ? p.getAsJsonObject().get("bone").getAsString() : "");
      }

      for (int i = 0; i < parts.size(); i++) {
         if (parts.get(i).isJsonObject()) {
            JsonObject part = parts.get(i).getAsJsonObject();
            String bone = bones.get(i);
            if (!part.has("name")) {
               part.addProperty("name", bone);
            }

            if (!part.has("toward")) {
               String toward = null;
               if (part.has("head") && part.get("head").getAsBoolean()) {
                  String up = model.parent.get(bone);

                  while (up != null && !bones.contains(up)) {
                     up = model.parent.get(up);
                  }

                  toward = up;
               } else if (i + 1 < bones.size() && model.below(bones.get(i + 1), bone)) {
                  toward = bones.get(i + 1);
               }

               if (toward != null) {
                  part.addProperty("toward", toward);
               }
            } else if (part.get("toward").getAsString().isEmpty()) {
               part.remove("toward");
            }

            double[] pivot = model.pivot.get(bone);
            if (!part.has("rest") && pivot != null) {
               JsonArray rest = new JsonArray();
               rest.add(pivot[0] / 16.0);
               rest.add(pivot[1] / 16.0);
               rest.add(-pivot[2] / 16.0);
               part.add("rest", rest);
            }
         }
      }
   }

   private static List<String> passes(JsonObject rig) {
      List<String> out = new ArrayList<>();
      if (rig.has("passes") && rig.get("passes").isJsonArray()) {
         for (JsonElement e : rig.getAsJsonArray("passes")) {
            out.add(e.getAsString());
         }
      }

      return out;
   }

   public record Model(List<String> bones, Map<String, String> parent, Map<String, double[]> pivot) {
      @Nullable
      public static BindingSource.Model of(@Nullable JsonObject geo) {
         if (geo != null && geo.has("minecraft:geometry")) {
            JsonArray geometry = geo.getAsJsonArray("minecraft:geometry");
            if (!geometry.isEmpty() && geometry.get(0).getAsJsonObject().has("bones")) {
               List<String> bones = new ArrayList<>();
               Map<String, String> parent = new HashMap<>();
               Map<String, double[]> pivot = new HashMap<>();

               for (JsonElement e : geometry.get(0).getAsJsonObject().getAsJsonArray("bones")) {
                  JsonObject b = e.getAsJsonObject();
                  String name = b.get("name").getAsString();
                  bones.add(name);
                  if (b.has("parent")) {
                     parent.put(name, b.get("parent").getAsString());
                  }

                  if (b.has("pivot")) {
                     JsonArray p = b.getAsJsonArray("pivot");
                     pivot.put(name, new double[]{p.get(0).getAsDouble(), p.get(1).getAsDouble(), p.get(2).getAsDouble()});
                  }
               }

               return new BindingSource.Model(List.copyOf(bones), Map.copyOf(parent), Map.copyOf(pivot));
            } else {
               return null;
            }
         } else {
            return null;
         }
      }

      public boolean below(String bone, String top) {
         String up = this.parent.get(bone);

         for (int guard = 0; up != null && guard < 256; guard++) {
            if (up.equals(top)) {
               return true;
            }

            up = this.parent.get(up);
         }

         return false;
      }
   }
}
