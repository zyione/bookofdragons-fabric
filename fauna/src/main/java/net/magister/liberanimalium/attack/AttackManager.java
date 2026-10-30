package net.magister.liberanimalium.attack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.magister.liberanimalium.attack.data.AttackDefinition;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.magister.liberanimalium.network.SyncOmniAttackDefinitionsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class AttackManager extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new GsonBuilder().create();
   private static AttackManager instance;
   private Map<ResourceLocation, AttackDefinition> attacks = new HashMap<>();

   public AttackManager() {
      super(GSON, "attacks");
      instance = this;
   }

   public static AttackManager getInstance() {
      if (instance == null) {
         instance = new AttackManager();
      }

      return instance;
   }

   protected void apply(Map<ResourceLocation, JsonElement> objectIn, @NotNull ResourceManager resourceManagerIn, @NotNull ProfilerFiller profilerIn) {
      Map<ResourceLocation, AttackDefinition> parsed = new HashMap<>();

      for (Entry<ResourceLocation, JsonElement> entry : objectIn.entrySet()) {
         ResourceLocation id = entry.getKey();
         AttackDefinition.CODEC
            .parse(JsonOps.INSTANCE, entry.getValue())
            .resultOrPartial(err -> LOGGER.error("Failed to parse attack definition {}: {}", id, err))
            .ifPresent(def -> parsed.put(id, def));
      }

      LOGGER.debug("Loaded {} attack definitions", parsed.size());
      this.attacks = parsed;
   }

   public AttackDefinition getAttack(ResourceLocation id) {
      return this.attacks.get(id);
   }

   public Map<ResourceLocation, AttackDefinition> getAllAttacks() {
      return Collections.unmodifiableMap(this.attacks);
   }

   public void put(ResourceLocation id, AttackDefinition definition) {
      Map<ResourceLocation, AttackDefinition> next = new HashMap<>(this.attacks);
      next.put(id, definition);
      this.attacks = next;
   }

   public void replaceDefinitions(Map<ResourceLocation, AttackDefinition> newAttacks) {
      this.attacks = newAttacks;
      LOGGER.debug("Synced {} attack definitions from server", this.attacks.size());
   }

   public void syncToClient(ServerPlayer player) {
      FaunaNetwork.sendToPlayer(new SyncOmniAttackDefinitionsPacket(this.attacks), player);
   }

   public void syncToAllClients() {
      FaunaNetwork.sendToAllClients(new SyncOmniAttackDefinitionsPacket(this.attacks));
   }
}
