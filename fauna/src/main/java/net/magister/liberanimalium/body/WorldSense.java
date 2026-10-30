package net.magister.liberanimalium.body;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.nav.pathfinding.VerticalScan;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Kind;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.magister.liberanimalium.fauna.api.SensedBody;
import net.magister.liberanimalium.fauna.api.SensedItem;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.api.Terrain;
import net.magister.liberanimalium.fauna.sense.SightSense;
import net.magister.liberanimalium.interop.Decks;
import net.magister.liberanimalium.platform.registry.Regs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class WorldSense {
   private static final int GROUND_SCAN_DOWN = 8;
   private static final int STANDABLE_SCAN_UP = 8;
   public static final String PROVIDED_BY_KEY = "liberanimalium:provided_by";
   private final Mob self;
   private final AnimalHandle handle;
   private final WorldSense.Appraisal appraisal;
   private static final int SIGHT_PASSES = 8;
   private int headClearTick = Integer.MIN_VALUE;
   private boolean headClear = true;
   private static final double CHEST_LID = 0.875;
   private static final double LID_CLEARANCE = 0.001;
   private static final double DECK_ROUNDING = 0.01;
   private static final int WATER_SCAN_DOWN = 4;
   private static final double LEAD_REACH = 10.0;
   private static final Map<EntityType<?>, Kind> KINDS = new ConcurrentHashMap<>();
   private List<LivingEntity> livingInSight;
   private int livingInSightTick = Integer.MIN_VALUE;
   private double livingInSightRadius = Double.NaN;

   public WorldSense(Mob self, AnimalHandle handle, WorldSense.Appraisal appraisal) {
      this.self = self;
      this.handle = handle;
      this.appraisal = appraisal;
   }

   public boolean lineOfSight(Vec3 from, Vec3 to) {
      HitResult hit = this.self.level().clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, this.self));
      return hit.getType() == Type.MISS;
   }

   public boolean sightLine(Vec3 from, Vec3 to) {
      Vec3 start = from;

      for (int i = 0; i < 8; i++) {
         BlockHitResult hit = this.self.level().clip(new ClipContext(start, to, Block.VISUAL, Fluid.NONE, this.self));
         if (hit.getType() == Type.MISS) {
            return true;
         }

         BlockPos at = hit.getBlockPos();
         BlockState state = this.self.level().getBlockState(at);
         if (!state.is(BlockTags.FENCES) && !state.is(BlockTags.FENCE_GATES)) {
            return false;
         }

         Optional<Vec3> exit = new AABB(at).clip(to, start);
         if (exit.isEmpty()) {
            return false;
         }

         start = exit.get().add(to.subtract(start).normalize().scale(0.001));
      }

      return false;
   }

   public boolean headClear() {
      if (this.headClearTick != this.self.tickCount) {
         this.headClearTick = this.self.tickCount;
         this.headClear = this.lineOfSight(this.self.getEyePosition(), this.handle.eyePosition());
      }

      return this.headClear;
   }

   public Terrain terrain() {
      return new Terrain() {
         @Override
         public boolean solid(BlockPos pos) {
            return !WorldSense.this.self.level().getBlockState(pos).getCollisionShape(WorldSense.this.self.level(), pos).isEmpty();
         }

         @Override
         public boolean fluid(BlockPos pos) {
            return !WorldSense.this.self.level().getFluidState(pos).isEmpty();
         }

         @Override
         public int skyLight(BlockPos pos) {
            return WorldSense.this.self.level().getBrightness(LightLayer.SKY, pos);
         }

         @Override
         public boolean standable(BlockPos pos) {
            return pos.equals(WorldSense.this.findStandableNear(pos));
         }

         @Override
         public float danger(Vec3 at) {
            return 0.0F;
         }

         @Override
         public boolean chestTop(BlockPos pos) {
            if (!(WorldSense.this.self.level().getBlockState(pos).getBlock() instanceof ChestBlock)) {
               return false;
            }

            Vec3 seat = new Vec3(pos.getX() + 0.5, pos.getY() + 0.875 + 0.001, pos.getZ() + 0.5);
            return WorldSense.this.self
               .level()
               .noCollision(WorldSense.this.self, WorldSense.this.self.getBoundingBox().move(seat.subtract(WorldSense.this.self.position())));
         }
      };
   }

   public boolean isBlockedAt(Vec3 point) {
      if (this.blockedAt(point.subtract(this.handle.bodyPosition()))) {
         return true;
      }

      Object deck = Decks.of(this.self);
      Vec3 place = deck == null ? null : Decks.place(deck, point);
      return place != null && this.blockedAt(place.subtract(this.handle.bodyPosition()));
   }

   private boolean blockedAt(Vec3 shift) {
      AABB box = this.self.getBoundingBox().move(shift);
      if (this.self.level().noCollision(this.self, box)) {
         return false;
      }

      AABB stepped = box.move(0.0, this.self.maxUpStep(), 0.0);
      return !this.self.level().noCollision(this.self, stepped);
   }

   public double groundHeightNear(Vec3 point, double fromY, double maxRise) {
      Object deck = Decks.of(this.self);
      Vec3 place = deck == null ? null : Decks.place(deck, new Vec3(point.x, fromY, point.z));
      if (place == null) {
         return this.column(point.x, point.z, fromY, maxRise);
      }

      double height = this.column(place.x, place.z, place.y, maxRise);
      if (Double.isNaN(height)) {
         return Double.NaN;
      }

      Vec3 ground = Decks.world(deck, new Vec3(place.x, height, place.z));
      return ground == null ? Double.NaN : ground.y;
   }

   private double column(double px, double pz, double fromY, double maxRise) {
      Level level = this.self.level();
      int x = Mth.floor(px);
      int z = Mth.floor(pz);
      int top = Mth.floor(fromY + Math.max(this.self.maxUpStep(), maxRise));
      int bottom = Mth.floor(fromY) - 8;
      int headroom = Math.max(1, (int)Math.ceil(this.self.getBbHeight()));
      MutableBlockPos cursor = new MutableBlockPos();

      for (int y = top; y >= bottom; y--) {
         cursor.set(x, y - 1, z);
         if (blocks(level, cursor) && level.getFluidState(cursor).isEmpty()) {
            boolean clear = true;

            for (int h = 0; h < headroom; h++) {
               cursor.set(x, y + h, z);
               if (blocks(level, cursor)) {
                  clear = false;
                  break;
               }
            }

            if (clear) {
               return y;
            }
         }
      }

      return Double.NaN;
   }

   static boolean blocks(BlockGetter level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.OPEN) ? false : !state.getCollisionShape(level, pos).isEmpty();
   }

   @Nullable
   public BlockPos findSwimmableNear(BlockPos approx, boolean dive) {
      Level level = this.self.level();
      int reach = Math.max(8, (int)this.handle.navRange());
      MutableBlockPos at = new MutableBlockPos(approx.getX(), approx.getY(), approx.getZ());
      int top = Integer.MIN_VALUE;
      int bottom = Integer.MIN_VALUE;

      for (int y = approx.getY() + 8; y >= approx.getY() - reach; y--) {
         at.setY(y);
         if (level.getFluidState(at).is(FluidTags.WATER)) {
            if (top == Integer.MIN_VALUE) {
               top = y;
            }

            bottom = y;
         } else if (top != Integer.MIN_VALUE) {
            break;
         }
      }

      if (top == Integer.MIN_VALUE) {
         return null;
      }

      at.setY(top + 1);
      return level.getBlockState(at).isCollisionShapeFullBlock(level, at) ? null : new BlockPos(approx.getX(), dive ? (top + bottom) / 2 : top, approx.getZ());
   }

   @Nullable
   public BlockPos findStandableNear(BlockPos approx) {
      Object deck = Decks.of(this.self);
      if (deck == null) {
         return this.standableNear(approx);
      }

      BlockPos onDeck = this.deckBlock(deck, approx);
      if (onDeck == null) {
         return null;
      }

      Vec3 world = Decks.world(deck, Vec3.atBottomCenterOf(onDeck));
      return world == null ? null : BlockPos.containing(world.add(0.0, 0.01, 0.0));
   }

   public boolean onDeck() {
      return Decks.of(this.self) != null;
   }

   @Nullable
   public BlockPos deckGoal(BlockPos world) {
      Object deck = Decks.of(this.self);
      return deck == null ? null : this.deckBlock(deck, world);
   }

   @Nullable
   private BlockPos deckBlock(Object deck, BlockPos world) {
      Vec3 place = Decks.place(deck, Vec3.atBottomCenterOf(world));
      return place == null ? null : this.standableNear(BlockPos.containing(place.add(0.0, 0.01, 0.0)));
   }

   @Nullable
   private BlockPos standableNear(BlockPos approx) {
      Level level = this.self.level();
      int scanDown = Math.max(8, (int)this.handle.navRange());
      Integer y = VerticalScan.nearestStandableY(
         approx.getX(),
         approx.getY(),
         approx.getZ(),
         scanDown,
         8,
         (x, cy, z) -> this.standableAt(level, x, cy, z),
         (x, cy, z) -> this.openAt(level, x, cy, z)
      );
      if (y == null && this.handle.currentMode() == MoveMode.AIR) {
         int top = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, approx.getX(), approx.getZ());
         if (top < approx.getY() - scanDown) {
            y = VerticalScan.nearestStandableY(
               approx.getX(), top, approx.getZ(), 8, 8, (x, cy, z) -> this.standableAt(level, x, cy, z), (x, cy, z) -> this.openAt(level, x, cy, z)
            );
         }
      }

      return y == null ? null : new BlockPos(approx.getX(), y, approx.getZ());
   }

   public double airGapBelow(Vec3 point) {
      double support = this.groundHeightNear(point, point.y, 0.0);
      double gap = Double.isNaN(support) ? 8.0 : Math.max(0.0, point.y - support);
      Level level = this.self.level();
      MutableBlockPos cursor = new MutableBlockPos();
      int x = Mth.floor(point.x);
      int z = Mth.floor(point.z);

      for (int y = Mth.floor(point.y); y >= Mth.floor(point.y - gap); y--) {
         cursor.set(x, y, z);
         if (!level.getFluidState(cursor).isEmpty()) {
            return Math.max(0.0, Math.min(gap, point.y - (y + 1)));
         }
      }

      return gap;
   }

   public boolean overWater(Vec3 point) {
      Level level = this.self.level();
      MutableBlockPos cursor = new MutableBlockPos();
      int top = Mth.floor(point.y);

      for (int y = top; y >= top - 4; y--) {
         cursor.set(Mth.floor(point.x), y, Mth.floor(point.z));
         if (level.getFluidState(cursor).is(FluidTags.WATER)) {
            return true;
         }

         if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) {
            return false;
         }
      }

      return false;
   }

   public List<SensedBody> scanNearby(double radius) {
      if (!(this.self.level() instanceof ServerLevel serverLevel)) {
         return List.of();
      } else {
         SpeciesProfile.Senses var14 = this.handle.senses();
         Vec3 eye = this.handle.eyePosition();
         Vec3 facing = this.handle.sightDirection();
         ArrayList out = new ArrayList();

         for (LivingEntity other : this.livingInSight(radius)) {
            BlockPos at = other.blockPosition();
            int light = Math.max(serverLevel.getBrightness(LightLayer.BLOCK, at), serverLevel.getBrightness(LightLayer.SKY, at));
            SensedBody body = new SensedBody(
               other.getUUID(),
               this.kindOf(other),
               other.position(),
               other.getEyePosition(),
               other.getBbWidth(),
               other.getBbHeight(),
               other.isShiftKeyDown(),
               other instanceof AnimalHandle peer && peer.hasPose(PoseFlag.STARTLED),
               other.isInWater(),
               light,
               true,
               heldMenace(other),
               Mth.clamp(other.getArmorValue() / 20.0F, 0.0F, 1.0F),
               other instanceof TamableAnimal tame && tame.getOwnerUUID() != null,
               leashHolder(other),
               tetheredTo(other),
               providedBy(other),
               other instanceof AnimalHandle mate && mate.hasPose(PoseFlag.PLAYFUL),
               this.leads(other),
               other instanceof Player || other instanceof AnimalHandle,
               other instanceof Mob mob && mob.getTarget() == this.self
            );
            boolean worthTheRay = SightSense.sees(var14, eye, facing, body);
            out.add(worthTheRay && this.headClear() && this.sightLine(eye, other.getEyePosition()) ? body : body.withLineOfSight(false));
         }

         return out;
      }
   }

   private List<LivingEntity> livingInSight(double radius) {
      if (this.livingInSightTick == this.self.tickCount && this.livingInSightRadius == radius && this.livingInSight != null) {
         return this.livingInSight;
      }

      this.livingInSight = this.self.level().getEntitiesOfClass(LivingEntity.class, this.self.getBoundingBox().inflate(radius), this::isSightCandidate);
      this.livingInSightTick = this.self.tickCount;
      this.livingInSightRadius = radius;
      return this.livingInSight;
   }

   private boolean isSightCandidate(LivingEntity other) {
      if (other != this.self && other.isAlive()) {
         return !(other instanceof Player player && (player.isCreative() || player.isSpectator()))
            ? !(other instanceof AnimalHandle down && down.incapacitated())
            : false;
      } else {
         return false;
      }
   }

   public List<SensedItem> scanItems(double radius) {
      if (!(this.self.level() instanceof ServerLevel)) {
         return List.of();
      }

      List<SensedItem> out = new ArrayList<>();

      for (ItemEntity entity : this.self.level().getEntitiesOfClass(ItemEntity.class, this.self.getBoundingBox().inflate(radius), Entity::isAlive)) {
         if (!entity.isInWater()) {
            CompoundTag pile = net.magister.liberanimalium.platform.fabric.EntityData.get(entity);
            if (!pile.hasUUID("LiberAnimaliumHoard") || !this.self.getUUID().equals(pile.getUUID("LiberAnimaliumHoard"))) {
               ItemStack stack = entity.getItem();
               float craving = this.appraisal.craving(stack);
               if (!(craving <= 0.0F) && this.sightLine(this.handle.eyePosition(), entity.position().add(0.0, 0.25, 0.0))) {
                  out.add(
                     new SensedItem(
                        entity.getUUID(),
                        entity.position(),
                        this.appraisal.isFood(stack),
                        craving,
                        null,
                        entity.getOwner() == null ? null : entity.getOwner().getUUID()
                     )
                  );
               }
            }
         }
      }

      if (out.size() > 16) {
         Vec3 here = this.handle.bodyPosition();
         out.sort(Comparator.comparingDouble(item -> item.position().distanceToSqr(here)));
         out = new ArrayList<>(out.subList(0, 16));
      }

      for (LivingEntity other : this.livingInSight(radius)) {
         ItemStack offered = this.appraisal.heldFood(other);
         if (!offered.isEmpty()) {
            out.add(new SensedItem(other.getUUID(), other.position(), true, this.appraisal.craving(offered), other.getUUID(), null));
         }

         if (this.appraisal.wantsTouchFrom(other)) {
            out.add(new SensedItem(other.getUUID(), other.position(), false, 1.0F, other.getUUID(), null));
         }
      }

      return out;
   }

   public Kind kindOf(Entity other) {
      Kind base = KINDS.computeIfAbsent(
         other.getType(),
         type -> {
            ResourceLocation key = Regs.entityTypeKey((EntityType<?>)type);
            Set<String> tags = Regs.entityTypeHolder((EntityType<?>)type)
               .map(holder -> holder.tags().map(tag -> "#" + tag.location()).collect(Collectors.toUnmodifiableSet()))
               .orElse(Set.of());
            return new Kind(key == null ? "" : key.toString(), tags, false);
         }
      );
      return other.getType() == this.self.getType() ? new Kind(base.typeKey(), base.tags(), true) : base;
   }

   private static float heldMenace(LivingEntity other) {
      return Math.max(itemMenace(other.getMainHandItem().getItem()), itemMenace(other.getOffhandItem().getItem()));
   }

   private List<Kind> leads(LivingEntity other) {
      if (!(other instanceof Player player)) {
         return List.of();
      } else {
         List<Mob> led = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(10.0), mobx -> mobx.isLeashed() && mobx.getLeashHolder() == player);
         if (led.isEmpty()) {
            return List.of();
         }

         List<Kind> kinds = new ArrayList<>(led.size());

         for (Mob mob : led) {
            kinds.add(this.kindOf(mob));
         }

         return kinds;
      }
   }

   @Nullable
   private static UUID leashHolder(LivingEntity other) {
      if (other instanceof Mob mob && mob.isLeashed()) {
         return mob.getLeashHolder() instanceof Player player ? player.getUUID() : null;
      } else {
         return null;
      }
   }

   private static boolean tetheredTo(LivingEntity other) {
      return other instanceof Mob mob && mob.isLeashed() && mob.getLeashHolder() instanceof LeashFenceKnotEntity;
   }

   @Nullable
   private static UUID providedBy(LivingEntity other) {
      CompoundTag data = net.magister.liberanimalium.platform.fabric.EntityData.get(other);
      return data.hasUUID("liberanimalium:provided_by") ? data.getUUID("liberanimalium:provided_by") : null;
   }

   private static float itemMenace(Item item) {
      if (item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem) {
         return 0.5F;
      } else if (item instanceof ShieldItem) {
         return 0.25F;
      } else {
         return item == Items.AIR ? 0.0F : 0.15F;
      }
   }

   public static void forgetKinds() {
      KINDS.clear();
   }

   private boolean standableAt(Level level, int x, int y, int z) {
      BlockPos pos = new BlockPos(x, y, z);
      if (!level.getFluidState(pos).isEmpty()) {
         return false;
      }

      BlockPos below = pos.below();
      return level.getBlockState(below).getCollisionShape(level, below).isEmpty() ? false : this.openAt(level, x, y, z);
   }

   private boolean openAt(Level level, int x, int y, int z) {
      AABB box = this.self.getBoundingBox().move(x + 0.5 - this.self.getX(), y - this.self.getY(), z + 0.5 - this.self.getZ());
      return level.noCollision(this.self, box);
   }

   public interface Appraisal {
      WorldSense.Appraisal NOTHING = new WorldSense.Appraisal() {
         @Override
         public float craving(ItemStack stack) {
            return 0.0F;
         }

         @Override
         public boolean isFood(ItemStack stack) {
            return false;
         }

         @Override
         public ItemStack heldFood(LivingEntity other) {
            return ItemStack.EMPTY;
         }

         @Override
         public boolean wantsTouchFrom(LivingEntity other) {
            return false;
         }
      };

      float craving(ItemStack var1);

      boolean isFood(ItemStack var1);

      ItemStack heldFood(LivingEntity var1);

      boolean wantsTouchFrom(LivingEntity var1);
   }
}
