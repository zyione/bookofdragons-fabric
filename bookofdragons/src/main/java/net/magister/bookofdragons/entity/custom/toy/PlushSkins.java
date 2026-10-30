package net.magister.bookofdragons.entity.custom.toy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.DyeColor;
import org.slf4j.Logger;

public final class PlushSkins extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static volatile Map<String, PlushSkins.Skins> tables = Map.of();
   private static volatile Map<String, String> raw = Map.of();

   public PlushSkins() {
      super(new Gson(), "plush");
   }

   protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
      Map<String, String> read = new LinkedHashMap<>();

      for (Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
         read.put(file.getKey().getPath(), file.getValue().toString());
      }

      install(read);
   }

   public static Map<String, String> raw() {
      return raw;
   }

   public static void install(Map<String, String> files) {
      Map<String, PlushSkins.Skins> read = new HashMap<>();

      for (Entry<String, String> file : files.entrySet()) {
         try {
            read.put(file.getKey(), parse(file.getKey(), JsonParser.parseString(file.getValue()).getAsJsonObject()));
         } catch (RuntimeException malformed) {
            LOGGER.warn("plush/{}.json is not a skins table: {}", file.getKey(), malformed.toString());
         }
      }

      tables = Map.copyOf(read);
      raw = Map.copyOf(files);
   }

   private static PlushSkins.Skins parse(String toy, JsonObject json) {
      ResourceLocation fallback = json.has("default") ? ResourceLocation.tryParse(json.get("default").getAsString()) : null;
      Map<DyeColor, ResourceLocation> byDye = new EnumMap<>(DyeColor.class);
      if (json.has("skins")) {
         for (Entry<String, JsonElement> skin : json.getAsJsonObject("skins").entrySet()) {
            DyeColor dye = DyeColor.byName(skin.getKey(), null);
            ResourceLocation texture = ResourceLocation.tryParse(skin.getValue().getAsString());
            if (dye != null && texture != null) {
               byDye.put(dye, texture);
            } else {
               LOGGER.warn("plush/{}.json: '{}' is not a dye with a texture", toy, skin.getKey());
            }
         }
      }

      return new PlushSkins.Skins(fallback, byDye);
   }

   @Nullable
   public static PlushSkins.Skins of(PlushKind kind) {
      return tables.get(kind.id());
   }

   public static boolean takes(PlushKind kind, DyeColor dye) {
      PlushSkins.Skins skins = of(kind);
      return skins == null || skins.byDye().containsKey(dye);
   }

   public record Skins(@Nullable ResourceLocation fallback, Map<DyeColor, ResourceLocation> byDye) {
   }
}
