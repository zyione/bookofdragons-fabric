package net.magister.bookofdragons.entity.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public final class PartWounds extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static volatile Map<String, PartWounds.Wound> wounds = Map.of();

   public PartWounds() {
      super(new Gson(), "fauna/wounds");
   }

   @Nullable
   public static PartWounds.Wound of(String species) {
      return wounds.get(species);
   }

   protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
      Map<String, PartWounds.Wound> loaded = new HashMap<>();

      for (Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
         try {
            JsonObject json = GsonHelper.convertToJsonObject(file.getValue(), "wound");
            PartWounds.Wound wound = new PartWounds.Wound(
               GsonHelper.getAsString(json, "part"),
               GsonHelper.getAsFloat(json, "limit"),
               GsonHelper.getAsFloat(json, "mend_per_tick"),
               GsonHelper.getAsInt(json, "grounded_ticks"),
               GsonHelper.getAsString(json, "line")
            );
            if (wound.limit() <= 0.0F || wound.groundedTicks() <= 0 || wound.mendPerTick() < 0.0F) {
               throw new IllegalArgumentException("limit and grounded_ticks must be over zero, mend_per_tick not under it");
            }

            loaded.put(file.getKey().getPath(), wound);
         } catch (RuntimeException malformed) {
            LOGGER.error("fauna/wounds/{}.json is not read: {}", file.getKey().getPath(), malformed.getMessage());
         }
      }

      wounds = Map.copyOf(loaded);
   }

   public record Wound(String part, float limit, float mendPerTick, int groundedTicks, String line) {
   }
}
