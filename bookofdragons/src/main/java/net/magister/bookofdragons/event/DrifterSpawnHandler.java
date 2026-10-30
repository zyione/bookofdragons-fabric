package net.magister.bookofdragons.event;

import java.util.Random;
import net.magister.bookofdragons.entity.ModEntities;
import net.magister.bookofdragons.entity.custom.HooliganDrifterEntity;
import net.magister.liberanimalium.compat.Entities;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder.Reference;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class DrifterSpawnHandler {
   private static final String ADVANCEMENT_ID = "minecraft:story/smelt_iron";
   private static final String NBT_KEY_DRIFTER_UNLOCKED = "bookofdragons_drifter_unlocked";
   private static final String NBT_KEY_LAST_SPAWN = "bookofdragons_drifter_last_spawn";
   private static final String NBT_KEY_SPAWN_CHANCE = "bookofdragons_drifter_chance";
   private static final int DEFAULT_SPAWN_DELAY = 24000;

   public static void init() {
      CommonEvents.ADVANCEMENT_EARNED.register(DrifterSpawnHandler::onAdvancementEarned);
      CommonEvents.LEVEL_TICK_END.register(DrifterSpawnHandler::onWorldTick);
      CommonEvents.LEGACY_DATA.register(DrifterSpawnHandler::onLegacyData);
   }

   private static void onLegacyData(Entity entity, CompoundTag forgeCaps, CompoundTag forgeData) {
      if (entity instanceof Player player) {
         CompoundTag data = net.magister.liberanimalium.platform.fabric.EntityData.get(player);

         for (String key : new String[]{"bookofdragons_drifter_unlocked", "bookofdragons_drifter_last_spawn", "bookofdragons_drifter_chance"}) {
            if (forgeData.contains(key) && !data.contains(key)) {
               data.put(key, forgeData.get(key).copy());
            }
         }
      }
   }

   public static void onAdvancementEarned(Player player, ResourceLocation advancementId) {
      if (player instanceof ServerPlayer serverPlayer) {
         if (player.level() instanceof ServerLevel serverLevel) {
            if (advancementId.toString().equals("minecraft:story/smelt_iron")) {
               CompoundTag playerData = net.magister.liberanimalium.platform.fabric.EntityData.get(serverPlayer);
               if (!playerData.getBoolean("bookofdragons_drifter_unlocked")) {
                  playerData.putBoolean("bookofdragons_drifter_unlocked", true);
                  playerData.putInt("bookofdragons_drifter_chance", 25);
                  boolean spawned = spawnDrifterNearPlayer(serverLevel, serverPlayer);
                  if (spawned) {
                     serverPlayer.sendSystemMessage(Component.translatable("entity.bookofdragons.hooligan_drifter.arrived").withStyle(ChatFormatting.YELLOW));
                     playerData.putLong("bookofdragons_drifter_last_spawn", serverLevel.getGameTime());
                     playerData.putInt("bookofdragons_drifter_chance", 25);
                  }
               }
            }
         }
      }
   }

   public static void onWorldTick(Level level) {
      if (level instanceof ServerLevel serverLevel) {
         if (serverLevel.dimension() == Level.OVERWORLD) {
            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
               if (player.level() == serverLevel && !player.isSpectator()) {
                  CompoundTag playerData = net.magister.liberanimalium.platform.fabric.EntityData.get(player);
                  if (playerData.getBoolean("bookofdragons_drifter_unlocked")) {
                     long gameTime = serverLevel.getGameTime();
                     long lastCheck = playerData.getLong("bookofdragons_drifter_last_spawn");
                     if (gameTime - lastCheck >= 24000L) {
                        int chance = playerData.getInt("bookofdragons_drifter_chance");
                        if (chance == 0) {
                           chance = 25;
                        }

                        Random random = new Random();
                        boolean success = false;
                        if (random.nextInt(100) < chance && serverLevel.getEntitiesOfClass(HooliganDrifterEntity.class, player.getBoundingBox().inflate(100.0)).size() == 0) {
                           success = spawnDrifterNearPlayer(serverLevel, player);
                        }

                        if (success) {
                           playerData.putLong("bookofdragons_drifter_last_spawn", gameTime);
                           playerData.putInt("bookofdragons_drifter_chance", 25);
                        } else {
                           if (random.nextInt(100) >= chance) {
                              int newChance = chance == 25 ? 50 : 75;
                              playerData.putInt("bookofdragons_drifter_chance", newChance);
                           }

                           playerData.putLong("bookofdragons_drifter_last_spawn", gameTime);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean spawnDrifterNearPlayer(ServerLevel level, ServerPlayer player) {
      BlockPos playerPos = player.blockPosition();
      int radius = 48;
      BlockPos spawnPos = null;

      for (int i = 0; i < 10; i++) {
         int x = playerPos.getX() + (level.random.nextInt(radius * 2) - radius);
         int z = playerPos.getZ() + (level.random.nextInt(radius * 2) - radius);
         int y = level.getHeight(Types.WORLD_SURFACE, x, z);
         BlockPos pos = new BlockPos(x, y, z);
         boolean isValid = Entities.spawnsOnGround(level, pos, (EntityType)ModEntities.HOOLIGAN_DRIFTER.get());
         if (isValid && level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
            spawnPos = pos;
            break;
         }
      }

      if (spawnPos == null) {
         return false;
      } else {
         HooliganDrifterEntity drifter = (HooliganDrifterEntity)((EntityType)ModEntities.HOOLIGAN_DRIFTER.get()).create(level);
         if (drifter != null) {
            drifter.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0F, 0.0F);
            Entities.finalizeSpawn(drifter, level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT);
            drifter.setDespawnDelay(48000);
            drifter.setWanderTarget(playerPos);
            drifter.restrictTo(playerPos, 16);
            drifter.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            drifter.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            level.addFreshEntity(drifter);
            level.playSound(null, spawnPos, (SoundEvent)((Reference)SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0)).value(), SoundSource.NEUTRAL, 5.0F, 1.0F);
            return true;
         } else {
            return false;
         }
      }
   }
}
