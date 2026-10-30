package net.magister.liberanimalium.chassis.component.ranged.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.magister.liberanimalium.chassis.component.melee.data.MeleeAttackDefinition;
import net.magister.liberanimalium.compat.Effects;
import net.magister.liberanimalium.fauna.core.PairSession;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class FaunaAttackManager extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static FaunaAttackManager instance;
   private Map<ResourceLocation, MeleeAttackDefinition> meleeAttacks = new HashMap<>();
   public static final Map<String, String> RAW_JSON = new HashMap<>();

   public FaunaAttackManager() {
      super(GSON, "fauna/melee");
      instance = this;
   }

   public static FaunaAttackManager getInstance() {
      if (instance == null) {
         instance = new FaunaAttackManager();
      }

      return instance;
   }

   protected void apply(Map<ResourceLocation, JsonElement> objectIn, @NotNull ResourceManager resourceManagerIn, @NotNull ProfilerFiller profilerIn) {
      Map<ResourceLocation, MeleeAttackDefinition> parsedMelee = new HashMap<>();
      RAW_JSON.clear();

      for (Entry<ResourceLocation, JsonElement> entry : objectIn.entrySet()) {
         ResourceLocation fileId = entry.getKey();

         try {
            String type = entry.getValue().isJsonObject() ? GsonHelper.getAsString(entry.getValue().getAsJsonObject(), "type", "ranged") : "ranged";
            if ("melee".equalsIgnoreCase(type)) {
               MeleeAttackDefinition.CODEC
                  .parse(JsonOps.INSTANCE, entry.getValue())
                  .resultOrPartial(s -> LOGGER.error("Failed to parse melee attack definition {}: {}", fileId, s))
                  .ifPresent(def -> parsedMelee.put(fileId, def));
            }
         } catch (Exception e) {
            LOGGER.error("Parsing error loading melee attack definition {}", fileId, e);
         }
      }

      LOGGER.debug("Loaded {} melee body attack definitions", parsedMelee.size());
      parsedMelee.forEach((id, def) -> def.onHit().ifPresent(onHit -> {
         if (Effects.instance(onHit.effect(), 1, 0).isEmpty()) {
            LOGGER.error("Melee attack {} names on_hit effect {}, and no effect has that id", id, onHit.effect());
         }
      }));
      this.meleeAttacks = Collections.unmodifiableMap(parsedMelee);
      PairSession.noteLongestStrike(parsedMelee.values().stream().mapToInt(def -> def.windupTicks() + def.activeTicks()).max().orElse(0));

      for (Entry<ResourceLocation, MeleeAttackDefinition> kept : parsedMelee.entrySet()) {
         JsonElement source = objectIn.get(kept.getKey());
         if (source != null) {
            RAW_JSON.put(kept.getKey().toString(), source.toString());
         }
      }
   }

   public void acceptSynced(Map<ResourceLocation, MeleeAttackDefinition> synced) {
      this.meleeAttacks = Collections.unmodifiableMap(new HashMap<>(synced));
   }

   public MeleeAttackDefinition getMeleeAttack(ResourceLocation id) {
      return id != null ? this.meleeAttacks.get(id) : null;
   }
}
