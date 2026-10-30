package net.magister.liberanimalium.body;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.util.SpecReader;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;

public final class BodyPlan {
   public static final String MUZZLE = "muzzle";
   public static final BodyPlan EMPTY = new BodyPlan(List.of(""), Map.of(), List.of(), Map.of(), List.of());
   private final List<String> prefixes;
   private final Map<String, List<String>> chains;
   private final List<PartSpec> parts;
   private final Map<String, String> tracked;
   private final List<String> problems;
   private final List<String> slots;

   BodyPlan(List<String> prefixes, Map<String, List<String>> chains, List<PartSpec> parts, Map<String, String> tracked, List<String> problems) {
      this.prefixes = prefixes.isEmpty() ? List.of("") : List.copyOf(prefixes);
      this.chains = Map.copyOf(chains);
      this.parts = List.copyOf(parts);
      this.tracked = Map.copyOf(tracked);
      this.problems = List.copyOf(problems);
      this.slots = slotsOf(this.parts, this.tracked);
   }

   public List<String> prefixes() {
      return this.prefixes;
   }

   public List<String> chain(String name) {
      return this.chains.getOrDefault(name, List.of());
   }

   public Map<String, List<String>> chains() {
      return this.chains;
   }

   public List<PartSpec> parts() {
      return this.parts;
   }

   public List<PartSpec> heads() {
      List<PartSpec> heads = new ArrayList<>();

      for (PartSpec part : this.parts) {
         if (part.head()) {
            heads.add(part);
         }
      }

      return heads;
   }

   public Map<String, String> tracked() {
      return this.tracked;
   }

   public List<String> reportSlots() {
      return this.slots;
   }

   public int slotOf(int partIndex) {
      int slot = 0;

      for (int i = 0; i < partIndex; i++) {
         slot += this.parts.get(i).elongated() ? 2 : 1;
      }

      return slot;
   }

   public int trackedSlot(String name) {
      if (!this.tracked.containsKey(name)) {
         return -1;
      }

      int slot = 0;

      for (PartSpec part : this.parts) {
         slot += part.elongated() ? 2 : 1;
      }

      for (String key : this.tracked.keySet()) {
         if (key.equals(name)) {
            return slot;
         }

         slot++;
      }

      return -1;
   }

   public List<String> problems() {
      return this.problems;
   }

   public boolean isEmpty() {
      return this.parts.isEmpty() && this.chains.isEmpty();
   }

   private static List<String> slotsOf(List<PartSpec> parts, Map<String, String> tracked) {
      List<String> out = new ArrayList<>();

      for (PartSpec part : parts) {
         out.add(part.bone());
         if (part.elongated()) {
            out.add(part.toward());
         }
      }

      out.addAll(tracked.values());
      return List.copyOf(out);
   }

   public static BodyPlan parse(@Nullable JsonObject json) {
      if (json == null) {
         return EMPTY;
      }

      List<String> problems = new ArrayList<>();
      List<String> prefixes = SpecReader.strings(json, "prefixes");
      Map<String, List<String>> chains = new LinkedHashMap<>();
      if (json.has("chains") && json.get("chains").isJsonObject()) {
         for (Entry<String, JsonElement> entry : json.getAsJsonObject("chains").entrySet()) {
            if (!entry.getValue().isJsonArray()) {
               problems.add("chain " + entry.getKey() + " is not a list of bones");
            } else {
               List<String> bones = new ArrayList<>();

               for (JsonElement bone : entry.getValue().getAsJsonArray()) {
                  if (bone.isJsonPrimitive()) {
                     bones.add(bone.getAsString());
                  }
               }

               chains.put(entry.getKey(), List.copyOf(bones));
            }
         }
      }

      List<PartSpec> parts = new ArrayList<>();
      if (json.has("parts") && json.get("parts").isJsonArray()) {
         for (JsonElement element : json.getAsJsonArray("parts")) {
            if (!element.isJsonObject()) {
               problems.add("a parts entry is not an object");
            } else {
               PartSpec part = part(element.getAsJsonObject(), problems);
               if (part != null) {
                  parts.add(part);
               }
            }
         }
      }

      Map<String, String> tracked = new LinkedHashMap<>();
      if (json.has("tracked") && json.get("tracked").isJsonObject()) {
         for (Entry<String, JsonElement> entry : json.getAsJsonObject("tracked").entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
               tracked.put(entry.getKey(), entry.getValue().getAsString());
            } else {
               problems.add("tracked " + entry.getKey() + " is not a bone name");
            }
         }
      }

      return new BodyPlan(prefixes, chains, parts, tracked, problems);
   }

   @Nullable
   private static PartSpec part(JsonObject json, List<String> problems) {
      String name = GsonHelper.getAsString(json, "name", "");
      String bone = GsonHelper.getAsString(json, "bone", "");
      if (!name.isEmpty() && !bone.isEmpty()) {
         String toward = GsonHelper.getAsString(json, "toward", "");
         float width = GsonHelper.getAsFloat(json, "width", 0.0F);
         float height = GsonHelper.getAsFloat(json, "height", width);
         float damage = GsonHelper.getAsFloat(json, "damage", 1.0F);
         boolean head = GsonHelper.getAsBoolean(json, "head", false);
         float stretch = GsonHelper.getAsFloat(json, "stretch", 0.75F);
         Vec3 rest = Vec3.ZERO;
         if (json.has("rest") && json.get("rest").isJsonArray()) {
            JsonArray array = json.getAsJsonArray("rest");
            if (array.size() == 3) {
               rest = new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
            } else {
               problems.add("part " + name + " rest must be [lateral, up, forward]");
            }
         }

         boolean walkable = GsonHelper.getAsBoolean(json, "walkable", false);
         return new PartSpec(name, bone, toward.isEmpty() ? null : toward, width, height, damage, head, stretch, rest, walkable);
      } else {
         problems.add("a part needs a name and a bone: " + json);
         return null;
      }
   }
}
