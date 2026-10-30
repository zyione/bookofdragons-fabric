package net.magister.bookofdragons.network.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.magister.bookofdragons.datagen.data.DragonRegistryData;
import net.magister.bookofdragons.datagen.data.DragonRegistryEntry;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonNames;
import net.magister.bookofdragons.network.ControlNetwork;
import net.magister.liberanimalium.body.data.CrippledFeature;
import net.magister.liberanimalium.body.stats.FaunaStatSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.IncapacitationComponent;
import net.magister.liberanimalium.fauna.api.Relation;
import net.magister.liberanimalium.fauna.core.FaunaMind;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.know.SocialBook.Opinion;
import net.magister.liberanimalium.platform.net.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class RequestDragonPageMessage {
   private final UUID dragon;
   private static final int MET_SHOWN = 12;

   public RequestDragonPageMessage(UUID dragon) {
      this.dragon = dragon;
   }

   public static void encode(RequestDragonPageMessage message, FriendlyByteBuf buffer) {
      buffer.writeUUID(message.dragon);
   }

   public static RequestDragonPageMessage decode(FriendlyByteBuf buffer) {
      return new RequestDragonPageMessage(buffer.readUUID());
   }

   public static void handle(RequestDragonPageMessage message, PacketContext context) {
      context.enqueueWork(() -> {
         ServerPlayer player = context.sender();
         if (player != null) {
            DragonPageMessage.Page page = page(player, message.dragon);
            if (page != null) {
               ControlNetwork.sendToPlayer(new DragonPageMessage(page), player);
            }
         }
      });
   }

   private static DragonPageMessage.Page page(ServerPlayer player, UUID uuid) {
      MinecraftServer server = player.getServer();
      if (server == null) {
         return null;
      } else {
         DragonRegistryData registry = DragonRegistryData.get(server);
         DragonRegistryEntry entry = registry.getDragon(uuid);
         DragonBase dragon = find(server, uuid);
         if (dragon != null && player.getUUID().equals(dragon.getOwnerUUID())) {
            return live(dragon, registry, player.getUUID());
         } else {
            return entry != null && player.getUUID().equals(entry.getOwnerUUID())
               ? new DragonPageMessage.Page(
                  uuid,
                  entry.getCustomName(),
                  entry.getType().name(),
                  entry.getStage().getStageId(),
                  entry.getGenetics(),
                  false,
                  entry.getHealth(),
                  entry.getMaxHealth(),
                  entry.getAffection(),
                  -1.0F,
                  -1.0F,
                  -1.0F,
                  new float[0],
                  new float[0],
                  List.of(),
                  List.of(),
                  DragonPageMessage.Injury.NONE,
                  lastSeen(entry, registry)
               )
               : null;
         }
      }
   }

   private static DragonBase find(MinecraftServer server, UUID uuid) {
      for (ServerLevel level : server.getAllLevels()) {
         if (level.getEntity(uuid) instanceof DragonBase dragon) {
            return dragon;
         }
      }

      return null;
   }

   private static DragonPageMessage.Page live(DragonBase d, DragonRegistryData registry, UUID owner) {
      FaunaStatSheet sheet = d.getStatSheet();
      float[] stats = new float[]{sheet.con(), sheet.ath(), sheet.agi(), sheet.prw(), sheet.pot(), sheet.vig()};
      FaunaMind mind = d.faunaMind();
      float[] temper = mind == null ? new float[0] : mind.temper().readings();
      float sleepDebt = mind == null ? -1.0F : mind.reservoirs().level("sleep");
      String name = DragonNames.of(d);
      CompoundTag genetics = (CompoundTag)d.getEntityData().get(FaunaBase.getGenotypeData());
      return new DragonPageMessage.Page(
         d.getUUID(),
         name,
         d.getDragonType().name(),
         d.getGrowthStage(),
         genetics == null ? new CompoundTag() : genetics,
         true,
         d.getHealth(),
         d.getMaxHealth(),
         d.getBond(owner),
         d.getNeedsSystem().getFoodLevel(),
         d.getStaminaSystem().getStaminaPercentage(),
         sleepDebt,
         stats,
         temper,
         met(d, mind),
         kin(d, mind, registry, owner),
         injury(d),
         new DragonPageMessage.Where(true, d.getX(), d.getY(), d.getZ(), d.level().dimension().location().toString(), false)
      );
   }

   private static DragonPageMessage.Where lastSeen(DragonRegistryEntry entry, DragonRegistryData registry) {
      boolean kept = registry.isStored(entry.getDragonUUID());
      if (entry.getLastKnownPos() != null && entry.getDimension() != null) {
         BlockPos at = entry.getLastKnownPos();
         return new DragonPageMessage.Where(
            !kept, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, entry.getDimension().location().toString(), kept
         );
      } else {
         return new DragonPageMessage.Where(false, 0.0, 0.0, 0.0, "", kept);
      }
   }

   private static DragonPageMessage.Injury injury(DragonBase d) {
      IncapacitationComponent incap = (IncapacitationComponent)d.componentRegistry.get(IncapacitationComponent.class);
      if (incap == null) {
         return DragonPageMessage.Injury.NONE;
      }

      List<String> broken = new ArrayList<>();

      for (CrippledFeature feature : incap.crippledFeatures()) {
         broken.add(CrippledFeature.key(feature));
      }

      return new DragonPageMessage.Injury(incap.isIncapacitated(), incap.getDebuffFraction(), broken, incap.ticksToMend());
   }

   private static List<DragonPageMessage.Acquaintance> met(DragonBase d, FaunaMind mind) {
      List<DragonPageMessage.Acquaintance> out = new ArrayList<>();
      if (mind == null) {
         return out;
      }

      List<Representation> reps = new ArrayList<>(mind.knowledge().all());
      reps.sort((a, b) -> Long.compare(b.lastSeenTick(), a.lastSeenTick()));

      for (Representation rep : reps) {
         if (out.size() >= 12) {
            break;
         }

         if (rep.id() != null) {
            out.add(acquaintance(mind, rep.id(), nameOf(d, rep.id(), rep.typeKey())));
         }
      }

      return out;
   }

   private static DragonPageMessage.Acquaintance acquaintance(FaunaMind mind, UUID id, String name) {
      Representation rep = mind == null ? null : mind.knowledge().of(id);
      Relation relation = rep != null ? rep.relation() : (mind == null ? null : mind.knowledge().standings().of(id));
      Opinion opinion = mind == null ? Opinion.NONE : mind.knowledge().social().of(id);
      int seconds = rep == null ? -1 : (int)Math.max(0L, (mind.age() - rep.lastSeenTick()) / 20L);
      return new DragonPageMessage.Acquaintance(
         id,
         name,
         relation == null ? "" : relation.type().name().toLowerCase(Locale.ROOT),
         relation == null ? 0.0F : relation.intensity(),
         opinion.like(),
         opinion.fear(),
         opinion.familiarity(),
         seconds
      );
   }

   private static List<DragonPageMessage.Acquaintance> kin(DragonBase d, FaunaMind mind, DragonRegistryData registry, UUID owner) {
      List<DragonPageMessage.Acquaintance> out = new ArrayList<>();

      for (DragonRegistryEntry other : registry.getDragonsByOwner(owner)) {
         if (!other.getDragonUUID().equals(d.getUUID())) {
            out.add(acquaintance(mind, other.getDragonUUID(), other.getCustomName()));
         }
      }

      return out;
   }

   private static String nameOf(DragonBase d, UUID id, String typeKey) {
      if (d.level() instanceof ServerLevel level) {
         Entity entity = level.getEntity(id);
         if (entity instanceof ServerPlayer seen) {
            return seen.getName().getString();
         }

         if (entity != null) {
            return DragonNames.of(entity);
         }

         ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
         if (player != null) {
            return player.getName().getString();
         }
      }

      ResourceLocation key = typeKey == null ? null : ResourceLocation.tryParse(typeKey);
      if (key != null && BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
         return DragonNames.of((EntityType<?>)BuiltInRegistries.ENTITY_TYPE.get(key));
      } else {
         return typeKey == null ? "?" : typeKey;
      }
   }
}
