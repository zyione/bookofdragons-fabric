package net.magister.liberanimalium.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.magister.liberanimalium.attack.AttackManager;
import net.magister.liberanimalium.attack.entity.AttackProjectileEntity;
import net.magister.liberanimalium.body.WorldSense;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.ContainerOpened;
import net.magister.liberanimalium.fauna.api.Delivery;
import net.magister.liberanimalium.fauna.api.Hurt;
import net.magister.liberanimalium.fauna.api.Kind;
import net.magister.liberanimalium.fauna.api.Kindness;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.api.SpeciesProfiles;
import net.magister.liberanimalium.fauna.api.Witness;
import net.magister.liberanimalium.fauna.core.FaunaMind;
import net.magister.liberanimalium.fauna.core.PairSession;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.network.DataSync;
import net.magister.liberanimalium.network.LibraryDataSets;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class FaunaWorldEvents {
   private static final double ROUTE_INVALIDATE_SCAN = 64.0;
   private static final double LEAD_TIE_RANGE = 7.0;

   private FaunaWorldEvents() {
   }

   public static void init() {
      CommonEvents.TAGS_UPDATED.register(FaunaWorldEvents::onTagsUpdated);
      CommonEvents.EXPLOSION.register(FaunaWorldEvents::onExplosionDetonate);
      CommonEvents.BLOCK_PLACED.register(FaunaWorldEvents::onBlockPlaced);
      CommonEvents.BLOCK_BROKEN.register(FaunaWorldEvents::onBlockBroken);
      CommonEvents.USE_BLOCK.register(FaunaWorldEvents::onRightClickBlock);
      CommonEvents.LIVING_DIED.register(FaunaWorldEvents::onLivingDeath);
      CommonEvents.PLAYER_LOGOUT.register(FaunaWorldEvents::onPlayerLoggedOut);
      CommonEvents.LIVING_DAMAGED.register(FaunaWorldEvents::onLivingHurt);
      CommonEvents.PLAYER_LOGIN.register(FaunaWorldEvents::onPlayerLoggedIn);
      CommonEvents.DATA_RELOADED.register(FaunaWorldEvents::onDataReloaded);
      CommonEvents.LEGACY_DATA.register(FaunaWorldEvents::onLegacyData);
      LibraryDataSets.register();
   }

   public static void onTagsUpdated() {
      WorldSense.forgetKinds();
   }

   public static void onExplosionDetonate(Level level, Explosion explosion, List<BlockPos> affectedBlocks, List<Entity> affectedEntities) {
      if (explosion.getDirectSourceEntity() instanceof AttackProjectileEntity breath) {
         Entity shooter = breath.getOwner();
         if (shooter != null) {
            affectedEntities.removeIf(entity -> entity.is(shooter) || shooter.hasPassenger(entity));
         }
      }
   }

   public static void onBlockPlaced(LevelAccessor level, BlockPos pos) {
      notifyRoutesNear(level, pos);
   }

   public static void onBlockBroken(LevelAccessor level, BlockPos pos) {
      notifyRoutesNear(level, pos);
   }

   private static void notifyRoutesNear(LevelAccessor levelAccessor, BlockPos pos) {
      if (levelAccessor instanceof ServerLevel level) {
         AABB area = new AABB(pos).inflate(64.0);

         for (Entity entity : level.getEntities((Entity)null, area, e -> e instanceof AnimalHandle)) {
            AnimalHandle handle = (AnimalHandle)entity;
            if (handle.faunaMind() != null) {
               handle.faunaMind().route().onBlockChanged(pos, handle.faunaMind().age());
            }
         }
      }
   }

   public static void onRightClickBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
      if (level instanceof ServerLevel serverLevel) {
         if (hand == InteractionHand.MAIN_HAND && player.isShiftKeyDown() && player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()) {
            FaunaBase.setDownPet(player);
         }

         if (serverLevel.getBlockState(hit.getBlockPos()).getBlock() instanceof ChestBlock) {
            noticeLoafers(serverLevel, player, hit.getBlockPos());
         } else if (serverLevel.getBlockState(hit.getBlockPos()).is(BlockTags.FENCES)) {
            UUID id = player.getUUID();
            Vec3 at = Vec3.atCenterOf(hit.getBlockPos());

            for (Mob mob : serverLevel.getEntitiesOfClass(
               Mob.class, new AABB(at.x - 7.0, at.y - 7.0, at.z - 7.0, at.x + 7.0, at.y + 7.0, at.z + 7.0)
            )) {
               if (mob.getLeashHolder() == player) {
                  net.magister.liberanimalium.platform.fabric.EntityData.get(mob).putUUID("liberanimalium:provided_by", id);
               }
            }
         }
      }
   }

   private static void noticeLoafers(ServerLevel level, Player player, BlockPos clicked) {
      if (!player.isCreative() && !player.isSpectator()) {
         BlockState state = level.getBlockState(clicked);
         List<BlockPos> halves = new ArrayList<>(List.of(clicked.immutable()));
         if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            halves.add(clicked.relative(ChestBlock.getConnectedDirection(state)).immutable());
         }

         for (BlockPos chest : halves) {
            AABB above = new AABB(
               chest.getX(), chest.getY() + 1, chest.getZ(), chest.getX() + 1, chest.getY() + 2, chest.getZ() + 1
            );

            for (FaunaBase animal : level.getEntitiesOfClass(FaunaBase.class, above, a -> a.hasFaunaPose(PoseFlag.LOAFING))) {
               FaunaMind mind = animal.faunaMind();
               if (mind != null) {
                  mind.perception().post(animal, new ContainerOpened(player.getUUID(), chest));
               }
            }
         }
      }
   }

   public static void onLegacyData(Entity entity, CompoundTag forgeCaps, CompoundTag forgeData) {
      CompoundTag data = net.magister.liberanimalium.platform.fabric.EntityData.get(entity);

      for (String key : forgeData.getAllKeys()) {
         if (key.startsWith("liberanimalium:") && !data.contains(key)) {
            data.put(key, forgeData.get(key).copy());
         }
      }
   }

   private static void provided(ServerLevel level, FaunaBase killer, UUID providerId) {
      FaunaMind mind = killer.faunaMind();
      if (mind != null) {
         Entity provider = level.getEntity(providerId);
         Representation known = mind.knowledge().about(providerId);
         Kind kind = provider != null
            ? killer.kindOfAttacker(provider)
            : (known != null ? new Kind(known.typeKey(), Set.of(), known.sameSpecies()) : Kind.UNKNOWN);
         Vec3 at = provider != null ? provider.position() : killer.position();
         mind.perception().deliver(killer, new Kindness(providerId, kind, at, mind.age(), Delivery.PREY));
         killer.awardGift(providerId, Delivery.PREY);
      }
   }

   public static void onLivingDeath(LivingEntity dead, DamageSource source) {
      if (dead.level() instanceof ServerLevel serverLevel) {
         if (source.getEntity() instanceof FaunaBase killer && killer.faunaMind() != null) {
            Representation rep = killer.faunaMind().knowledge().of(dead.getUUID());
            if (rep != null && rep.relation().type() == RelationType.EATS) {
               killer.feedOnKill(dead.getMaxHealth());
               UUID provider = rep.tethered() ? rep.providedBy() : rep.leashHolder();
               if (provider != null) {
                  provided(serverLevel, killer, provider);
               }
            }
         }

         if (source.getEntity() instanceof LivingEntity culprit && !culprit.getUUID().equals(dead.getUUID())) {
            forEachNearbyMind(serverLevel, dead, mind -> {
               if (!mind.handle().faunaId().equals(culprit.getUUID())) {
                  if (mind.handle() instanceof FaunaBase observer) {
                     if (observer.lineOfSight(observer.eyePosition(), dead.position())) {
                        mind.knowledge().noteKilled(dead.getUUID(), culprit.getUUID(), observer.kindOfAttacker(culprit), dead.position(), mind.age());
                     }
                  }
               }
            });
         }

         boolean mayReturn = dead instanceof Player;
         forEachNearbyMind(serverLevel, dead, mind -> mind.forget(dead.getUUID(), mayReturn));
      }
   }

   public static void onPlayerLoggedOut(Player player) {
      PairSession.closeAll(player.getUUID());
   }

   public static void onLivingHurt(LivingEntity entity, DamageSource source, float amount) {
      if (!entity.level().isClientSide()) {
         if (entity.level() instanceof ServerLevel serverLevel && source.getEntity() != entity) {
            Entity hitBy = source.getEntity();
            UUID victim = entity.getUUID();
            float severity = amount / Math.max(1.0F, entity.getMaxHealth());
            forEachNearbyMind(
               serverLevel,
               entity,
               mind -> {
                  if (!mind.handle().faunaId().equals(victim)) {
                     if (mind.handle() instanceof FaunaBase observer) {
                        mind.perception()
                           .deliver(
                              mind.handle(),
                              new Witness(
                                 victim, hitBy == null ? null : hitBy.getUUID(), observer.kindOfAttacker(hitBy), entity.position(), severity, mind.age()
                              )
                           );
                     }
                  }
               }
            );
         }

         if (entity instanceof FaunaBase body) {
            FaunaMind mind = body.faunaMind();
            if (mind != null) {
               Entity attacker = source.getEntity();
               mind.perception()
                  .deliver(
                     body,
                     new Hurt(
                        attacker == null ? null : attacker.getUUID(),
                        body.kindOfAttacker(attacker),
                        attacker == null ? body.position() : attacker.position(),
                        amount,
                        mind.age()
                     )
                  );
            }
         }
      }
   }

   private static void forEachNearbyMind(ServerLevel level, Entity about, Consumer<FaunaMind> action) {
      forEachMindWithin(level, about, SpeciesProfiles.maxSightRadius(), action);
   }

   public static void forEachMindWithin(ServerLevel level, Entity about, double radius, Consumer<FaunaMind> action) {
      double reach = radius * radius;

      for (Entity nearby : level.getEntities(about, about.getBoundingBox().inflate(radius))) {
         if (nearby instanceof FaunaBase body && !(nearby.distanceToSqr(about) > reach)) {
            FaunaMind mind = body.faunaMind();
            if (mind != null) {
               action.accept(mind);
            }
         }
      }
   }

   public static void onPlayerLoggedIn(Player player) {
      if (player instanceof ServerPlayer serverPlayer) {
         sendData(serverPlayer);
      }
   }

   public static void onDataReloaded(List<ServerPlayer> players) {
      for (ServerPlayer player : players) {
         sendData(player);
      }
   }

   private static void sendData(ServerPlayer serverPlayer) {
      AttackManager.getInstance().syncToClient(serverPlayer);
      DataSync.send(serverPlayer);
   }
}
