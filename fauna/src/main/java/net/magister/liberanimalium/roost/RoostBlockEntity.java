package net.magister.liberanimalium.roost;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.fauna.core.FaunaMind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class RoostBlockEntity extends BlockEntity {
   private final RoostLedger ledger = new RoostLedger();
   private final List<RoostModule> modules = new ArrayList<>();
   @Nullable
   private BlockPos controllerPos;
   private final Set<BlockPos> members = new LinkedHashSet<>();
   private long loadedAt = Long.MIN_VALUE;

   protected RoostBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   protected final void addModule(RoostModule module) {
      for (RoostModule present : this.modules) {
         if (present.id().equals(module.id())) {
            throw new IllegalArgumentException("two roost modules named " + module.id());
         }
      }

      this.modules.add(module);
   }

   @Nullable
   public <T extends RoostModule> T module(Class<T> type) {
      for (RoostModule module : this.root().modules) {
         if (type.isInstance(module)) {
            return type.cast(module);
         }
      }

      return null;
   }

   public boolean isController() {
      return this.controllerPos == null;
   }

   public RoostBlockEntity root() {
      if (this.controllerPos != null && this.level != null && this.level.isLoaded(this.controllerPos)) {
         return this.level.getBlockEntity(this.controllerPos) instanceof RoostBlockEntity controller && controller.isController() ? controller : this;
      } else {
         return this;
      }
   }

   public List<BlockPos> blocks() {
      RoostBlockEntity root = this.root();
      List<BlockPos> out = new ArrayList<>();
      out.add(root.worldPosition);
      out.addAll(root.members);
      return out;
   }

   public int blockCount() {
      return this.root().members.size() + 1;
   }

   void becomeMemberOf(BlockPos controller) {
      this.controllerPos = controller;
      this.members.clear();
      this.setChanged();
   }

   void becomeController(Set<BlockPos> others) {
      this.controllerPos = null;
      this.members.clear();
      this.members.addAll(others);
      this.members.remove(this.worldPosition);
      this.setChanged();
   }

   public List<UUID> occupants() {
      return this.root().ledger.occupants();
   }

   public boolean holds(UUID id) {
      return this.root().ledger.holds(id);
   }

   public boolean placedByPlayer() {
      return this.root().ledger.placedBy() != null;
   }

   @Nullable
   public UUID placedBy() {
      return this.root().ledger.placedBy();
   }

   public void setPlacedBy(@Nullable UUID player) {
      if (this.blockCount() == 1) {
         this.ledger.setPlacedBy(player);
         this.setChanged();
      }
   }

   public BlockPos footprintMin() {
      RoostBlockEntity root = this.root();
      if (root.ledger.footprintMin() != null) {
         return root.ledger.footprintMin();
      }

      BlockPos min = root.worldPosition;

      for (BlockPos p : root.members) {
         min = new BlockPos(Math.min(min.getX(), p.getX()), min.getY(), Math.min(min.getZ(), p.getZ()));
      }

      return min;
   }

   public BlockPos footprintMax() {
      RoostBlockEntity root = this.root();
      if (root.ledger.footprintMax() != null) {
         return root.ledger.footprintMax();
      }

      BlockPos max = root.worldPosition;

      for (BlockPos p : root.members) {
         max = new BlockPos(Math.max(max.getX(), p.getX()), max.getY(), Math.max(max.getZ(), p.getZ()));
      }

      return max;
   }

   public boolean hasFootprint() {
      return this.root().ledger.footprintMin() != null;
   }

   public void setFootprint(BlockPos min, BlockPos max) {
      RoostBlockEntity root = this.root();
      root.ledger.setFootprint(min, max);
      root.setChanged();
   }

   public int slots() {
      RoostBlockEntity root = this.root();
      int slots = root.blockCount() + 1;

      for (RoostModule module : root.modules) {
         slots = module.slots(root, slots);
      }

      return Math.max(0, slots);
   }

   public int freeSlots() {
      return Math.max(0, this.slots() - this.root().ledger.count());
   }

   public boolean describedAs(String word) {
      for (RoostModule module : this.root().modules) {
         if (module.describes(word)) {
            return true;
         }
      }

      return false;
   }

   protected boolean acceptsHost(String host) {
      return host.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(this.getType()).getNamespace());
   }

   @Nullable
   public String refusal(FaunaBase claimant) {
      RoostBlockEntity root = this.root();
      if (root.level instanceof ServerLevel server) {
         if (!root.acceptsHost(SpeciesAssets.hostOf(claimant))) {
            return "another mod's roost";
         }

         for (RoostModule module : root.modules) {
            if (!module.accepts(root, claimant)) {
               return "refused by " + module.id();
            }
         }

         return RoostClaimRule.refusal(
            new RoostClaimRule.Facts(
               claimant.isBaby(),
               root.ledger.holds(claimant.faunaId()),
               this.freeSlots(),
               claimant.isTame(),
               this.placedByPlayer(),
               root.friendsWithAll(server, claimant),
               root.conqueror(server, claimant)
            )
         );
      } else {
         return "client side";
      }
   }

   public boolean claim(FaunaBase claimant) {
      RoostBlockEntity root = this.root();
      if (root.level instanceof ServerLevel server && this.refusal(claimant) == null) {
         root.ledger.add(claimant.faunaId(), server.getGameTime());
         claimant.setRoostPosition(root.worldPosition);

         for (RoostModule module : root.modules) {
            module.onClaim(root, server, claimant);
         }

         root.setChanged();
         return true;
      } else {
         return false;
      }
   }

   public boolean forfeit(FaunaBase loser, FaunaBase winner) {
      RoostBlockEntity root = this.root();
      if (root.level instanceof ServerLevel server && root.ledger.holds(loser.faunaId())) {
         root.release(loser.faunaId());
         if (root.worldPosition.equals(loser.getRoostPosition())) {
            loser.setRoostPosition(null);
         }

         root.ledger.setConquest(winner.faunaId(), server.getGameTime());

         for (RoostModule module : root.modules) {
            module.onForfeit(root, server, loser.faunaId(), winner);
         }

         if (winner.getRoostPosition() == null) {
            root.claim(winner);
         }

         root.setChanged();
         return true;
      } else {
         return false;
      }
   }

   private boolean conqueror(ServerLevel level, FaunaBase claimant) {
      return claimant.faunaId().equals(this.ledger.conqueror()) && level.getGameTime() - this.ledger.conqueredAt() <= 1200L;
   }

   public boolean release(UUID occupant) {
      RoostBlockEntity root = this.root();
      if (root.level instanceof ServerLevel server && root.ledger.remove(occupant)) {
         for (RoostModule module : root.modules) {
            module.onRelease(root, server, occupant);
         }

         root.setChanged();
         return true;
      } else {
         return false;
      }
   }

   public void seen(UUID occupant) {
      RoostBlockEntity root = this.root();
      if (root.level != null) {
         root.ledger.seen(occupant, root.level.getGameTime());
      }
   }

   void trimToSlots() {
      List<UUID> held = this.ledger.occupants();

      for (int i = held.size() - 1; i >= 0 && this.ledger.count() > this.slots(); i--) {
         this.release(held.get(i));
      }
   }

   private boolean friendsWithAll(ServerLevel level, FaunaBase claimant) {
      for (UUID id : this.ledger.occupants()) {
         if (!(level.getEntity(id) instanceof FaunaBase occupant)) {
            return false;
         }

         if (!likes(claimant, occupant) || !likes(occupant, claimant)) {
            return false;
         }
      }

      return true;
   }

   private static boolean likes(FaunaBase who, FaunaBase whom) {
      FaunaMind mind = who.faunaMind();
      return mind != null && mind.knowledge().social().of(whom.faunaId()).like() >= 0.5F;
   }

   public InteractionResult use(Player player, InteractionHand hand, ItemStack stack) {
      RoostBlockEntity root = this.root();

      for (RoostModule module : root.modules) {
         InteractionResult result = module.use(root, player, hand, stack);
         if (result != InteractionResult.PASS) {
            return result;
         }
      }

      return InteractionResult.PASS;
   }

   void blockChanged(ServerLevel level, BlockPos pos, boolean added) {
      for (RoostModule module : this.modules) {
         if (added) {
            module.onBlockAdded(this, level, pos);
         } else {
            module.onBlockRemoved(this, level, pos);
         }
      }
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, RoostBlockEntity roost) {
      if (level instanceof ServerLevel server) {
         roost.tick(server);
      }
   }

   private void tick(ServerLevel level) {
      long now = level.getGameTime();
      if (this.loadedAt == Long.MIN_VALUE) {
         this.loadedAt = now;
         this.ledger.stampUnseen(now);
      }

      if (Math.floorMod(now + this.worldPosition.hashCode(), 200) == 0) {
         this.registerPoi(level);
         if (this.isController()) {
            this.heartbeat(level, now);
         }
      }
   }

   private void heartbeat(ServerLevel level, long now) {
      for (UUID id : this.ledger.occupants()) {
         Entity entity = level.getEntity(id);
         if (entity != null) {
            boolean left = !entity.isAlive();
            if (entity instanceof FaunaBase creature) {
               BlockPos home = creature.getRoostPosition();
               boolean here = home != null && (home.equals(this.worldPosition) || this.members.contains(home));
               left |= !here;
               left |= creature.isTame() && !this.placedByPlayer();
               if (left && here) {
                  creature.setRoostPosition(null);
               }
            }

            if (left) {
               this.release(id);
            } else {
               this.ledger.seen(id, now);
            }
         }
      }

      if (now - this.loadedAt > 200L) {
         for (UUID id : this.ledger.unseenSince(now - 24000L)) {
            this.release(id);
         }
      }

      for (RoostModule module : this.modules) {
         module.onHeartbeat(this, level);
      }
   }

   private void registerPoi(ServerLevel level) {
      PoiTypes.forState(this.getBlockState()).ifPresent(type -> {
         PoiManager poi = level.getPoiManager();
         if (poi.getType(this.worldPosition).isEmpty()) {
            poi.add(this.worldPosition, type);
         }
      });
   }

   protected void upgrade(CompoundTag tag) {
   }

   CompoundTag saveState() {
      CompoundTag tag = new CompoundTag();
      this.ledger.save(tag);
      CompoundTag all = new CompoundTag();

      for (RoostModule module : this.modules) {
         CompoundTag own = new CompoundTag();
         module.save(own);
         all.put(module.id(), own);
      }

      tag.put("Modules", all);
      return tag;
   }

   void loadState(CompoundTag tag) {
      this.ledger.load(tag);
      CompoundTag all = tag.getCompound("Modules");

      for (RoostModule module : this.modules) {
         module.load(all.getCompound(module.id()));
      }

      this.setChanged();
   }

   void absorbOccupants(RoostBlockEntity other) {
      for (UUID id : other.ledger.occupants()) {
         if (!this.ledger.holds(id) && this.level != null) {
            this.ledger.add(id, this.level.getGameTime());
         }
      }

      this.setChanged();
   }

   private void saveRoost(CompoundTag tag) {
      tag.merge(this.saveState());
      if (this.controllerPos != null) {
         tag.putLong("Controller", this.controllerPos.asLong());
      }

      if (!this.members.isEmpty()) {
         ListTag list = new ListTag();

         for (BlockPos p : this.members) {
            list.add(LongTag.valueOf(p.asLong()));
         }

         tag.put("Members", list);
      }
   }

   private void loadRoost(CompoundTag tag) {
      this.upgrade(tag);
      this.loadState(tag);
      this.controllerPos = tag.contains("Controller") ? BlockPos.of(tag.getLong("Controller")) : null;
      this.members.clear();

      for (Tag t : tag.getList("Members", 4)) {
         this.members.add(BlockPos.of(((LongTag)t).getAsLong()));
      }
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      this.saveRoost(tag);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.loadRoost(tag);
   }
}
