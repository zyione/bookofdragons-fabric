package net.magister.bookofdragons.advancement;

import org.jetbrains.annotations.Nullable;
import net.magister.bookofdragons.datagen.data.DragonDiscoveryData;
import net.magister.bookofdragons.discovery.DiscoveryStore;
import net.magister.bookofdragons.discovery.Lore;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayRegistry;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.magister.liberanimalium.progression.FaunaInteractionEvents;
import net.magister.liberanimalium.progression.FaunaInteractionEvents.Type;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy;
import net.minecraft.world.entity.player.Player;

public final class DragonMilestones {
   public static final String MET = "met";
   public static final String VARIANT = "variant";
   public static final String TAMED = "tamed";
   public static final String BONDED = "bonded";
   public static final String FACT = "fact";
   public static final String CHAPTER = "chapter";
   public static final String ENTRY = "entry";
   public static final String STRUGGLE = "struggle";
   public static final String ASK = "ask";
   public static final String TAKEOFF = "takeoff";
   public static final String DODGE = "dodge";
   public static final String BREATH = "breath";
   public static final String ROOST = "roost";
   public static final String BOUND = "bound";
   private static final ResourceKey<PoiType> ROOST_POI = ResourceKey.create(
      Registries.POINT_OF_INTEREST_TYPE, new ResourceLocation("bookofdragons", "roost")
   );
   private static final int ROOST_RANGE = 12;
   private static final int ROOST_PERIOD = 40;

   private DragonMilestones() {
   }

   static void init() {
      FaunaInteractionEvents.subscribe(e -> {
         if (e.body() instanceof DragonBase dragon && e.player() != null) {
            if (e.type() == Type.STRUGGLE_WON) {
               reach(e.player(), "struggle", dragon);
            } else if (e.type() == Type.ASK_ANSWERED) {
               reach(e.player(), "ask", dragon);
            }
         }
      });
      CommonEvents.PLAYER_LOGIN.register(DragonMilestones::replay);
      CommonEvents.SERVER_TICK_END.register(DragonMilestones::lookForRoosts);
   }

   public static void reach(Player player, String event, DragonBase dragon) {
      reach(player, event, dragon.getDragonType());
   }

   public static void reach(Player player, String event, @Nullable DragonType type) {
      if (player instanceof ServerPlayer server) {
         String species = type == null ? null : type.getSerializedName();
         ModAdvancementTriggers.DRAGON.trigger(server, event, species, species == null ? null : SpeciesGameplayRegistry.speciesClassOf(species));
      }
   }

   public static void wrote(Player player, DragonDiscoveryData data, DragonType type) {
      reach(player, "fact", type);
      if (Lore.tier(data, type) >= 2) {
         reach(player, "chapter", type);
      }

      if (Lore.progress(data, type) >= 1.0F) {
         reach(player, "entry", type);
      }
   }

   private static void replay(ServerPlayer player) {
      DiscoveryStore.get(player).ifPresent(data -> {
         for (DragonType type : DragonType.values()) {
            if (data.hasEncountered(type)) {
               reach(player, "met", type);
            }

            if (data.hasTamed(type)) {
               reach(player, "tamed", type);
            }

            if (data.isBonded(type)) {
               reach(player, "bonded", type);
            }

            if (!data.facts(type).isEmpty()) {
               wrote(player, data, type);
            }
         }

         if (!data.seenVariants().isEmpty()) {
            reach(player, "variant", (DragonType)null);
         }
      });
   }

   private static void lookForRoosts(MinecraftServer server) {
      if (server.getTickCount() % 40 == 0) {
         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isSpectator()) {
               BlockPos at = player.blockPosition();
               boolean near = player.serverLevel().getPoiManager().findClosest(poi -> poi.is(ROOST_POI), at, 12, Occupancy.ANY).isPresent();
               if (near) {
                  reach(player, "roost", (DragonType)null);
               }
            }
         }
      }
   }
}
