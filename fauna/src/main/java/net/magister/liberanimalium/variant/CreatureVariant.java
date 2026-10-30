package net.magister.liberanimalium.variant;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.genetics.GeneticsPreset;
import net.magister.liberanimalium.platform.registry.Regs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public record CreatureVariant(
   ResourceLocation id,
   EntityType<?> type,
   String name,
   String skin,
   GeneticsPreset genetics,
   @Nullable SpeciesProfile profile,
   List<String> missing,
   @Nullable JsonObject identity,
   @Nullable JsonObject body
) {
   public static final List<String> PROFILE_BLOCKS = List.of("habit", "senses", "movement", "social", "traits", "drives", "relations", "taming");
   private static final Set<String> KEYS = Set.of("entity", "name", "skin", "genetics", "profile", "identity", "body");

   public boolean overridesGameplay() {
      return this.identity != null || this.body != null;
   }

   public static CreatureVariant parse(ResourceLocation id, JsonObject json) {
      for (String key : json.keySet()) {
         if (!key.startsWith("__") && !KEYS.contains(key)) {
            throw new IllegalArgumentException("unknown key '" + key + "' (known: " + KEYS + ")");
         }
      }

      if (!json.has("entity")) {
         throw new IllegalArgumentException("no \"entity\"");
      }

      String word = json.get("entity").getAsString();
      ResourceLocation typeId = ResourceLocation.tryParse(word.contains(":") ? word : id.getNamespace() + ":" + word);
      if (typeId != null && Regs.hasEntityType(typeId)) {
         GeneticsPreset genetics = json.has("genetics") ? GeneticsPreset.parse(json.getAsJsonObject("genetics")) : GeneticsPreset.NONE;
         SpeciesProfile profile = null;
         List<String> missing = new ArrayList<>();
         if (json.has("profile")) {
            JsonObject root = json.getAsJsonObject("profile");

            for (String block : PROFILE_BLOCKS) {
               if (!root.has(block)) {
                  missing.add(block);
               }
            }

            profile = SpeciesProfile.parse(root, id.toString(), id.getNamespace());
         }

         return new CreatureVariant(
            id,
            Regs.entityType(typeId),
            json.has("name") ? json.get("name").getAsString() : "",
            json.has("skin") ? json.get("skin").getAsString() : "",
            genetics,
            profile,
            List.copyOf(missing),
            json.has("identity") ? json.getAsJsonObject("identity").deepCopy() : null,
            json.has("body") ? json.getAsJsonObject("body").deepCopy() : null
         );
      } else {
         throw new IllegalArgumentException("no entity type " + word);
      }
   }
}
