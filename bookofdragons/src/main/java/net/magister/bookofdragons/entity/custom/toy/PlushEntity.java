package net.magister.bookofdragons.entity.custom.toy;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.magister.bookofdragons.config.ModClientConfig;
import net.magister.liberanimalium.anim.rig.DampedSpring;
import net.magister.liberanimalium.anim.rig.RigOnlyBody;
import net.magister.liberanimalium.anim.rig.RigSpec;
import net.magister.liberanimalium.body.grip.Grip;
import net.magister.liberanimalium.compat.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public class PlushEntity extends LivingEntity implements RigOnlyBody {
   private static final EntityDataAccessor<Quaternionf> ORIENTATION = SynchedEntityData.defineId(PlushEntity.class, EntityDataSerializers.QUATERNION);
   private static final EntityDataAccessor<Byte> COLOR = SynchedEntityData.defineId(PlushEntity.class, EntityDataSerializers.BYTE);
   private static final double GRAVITY = 0.08;
   private static final double AIR_DRAG = 0.98;
   private static final double WATER_DRAG = 0.85;
   private static final double BUOYANCY = 0.03;
   private static final double MIN_BOUNCE = 0.12;
   private static final double HIT_BASE = 0.35;
   private static final double HIT_PER_DAMAGE = 0.06;
   private static final double HIT_MAX = 1.4;
   private static final double HIT_LIFT = 0.45;
   private static final double THROW_MAX = 2.0;
   private static final float HIT_SPIN = 0.9F;
   private static final float SPIN_AIR_KEEP = 0.99F;
   private static final float HELD_SETTLE = 0.3F;
   private static final double REST_SPEED = 0.03;
   private static final float SETTLE_RATE = 0.25F;
   private static final float DRAW_FOLLOW = 0.6F;
   private static final float SQUEAK_VOLUME = 0.7F;
   private static final float SQUEAK_PITCH_SPREAD = 0.3F;
   private static final double SQUEAK_BOUNCE = 0.4;
   private static final double SQUISH_HZ = 4.0;
   private static final double SQUISH_DAMPING = 0.3;
   private static final double SQUISH_LIMIT = 0.45;
   private static final double SQUEEZE_KICK = 9.0;
   private static final double BUMP_KICK = 6.0;
   private static final byte SQUEEZE_EVENT = 70;
   private static final byte BUMP_EVENT = 71;
   private static final byte SQUEAK_EVENT = 72;
   private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);
   private static final String ORIENTATION_KEY = "Orientation";
   private static final String COLOR_KEY = "Color";
   private static final byte UNDYED = -1;
   private final PlushKind kind;
   private final Quaternionf orientation = new Quaternionf();
   private final Vector3f spin = new Vector3f();
   private final Quaternionf drawn = new Quaternionf();
   private final Quaternionf drawnO = new Quaternionf();
   private boolean drawnSet;
   private final DampedSpring squish = new DampedSpring();
   private float squishO;

   public PlushEntity(EntityType<? extends PlushEntity> type, Level level) {
      super(type, level);
      this.kind = PlushKind.of(type);
      this.setMaxUpStep(1.0F);
   }

   public PlushKind kind() {
      return this.kind;
   }

   public void place(double x, double y, double z, float yaw) {
      this.setPos(x, y, z);
      this.orientation.rotationY(-yaw * (float) (Math.PI / 180.0));
      this.entityData.set(ORIENTATION, new Quaternionf(this.orientation));
   }

   @Nullable
   public DyeColor color() {
      byte id = (Byte)this.entityData.get(COLOR);
      return id < 0 ? null : DyeColor.byId(id);
   }

   public void color(@Nullable DyeColor color) {
      this.entityData.set(COLOR, color == null ? -1 : (byte)color.getId());
   }

   @Nullable
   public static DyeColor colorOf(ItemStack stack) {
      return StackData.has(stack, "Color") ? DyeColor.byId(StackData.read(stack).getByte("Color")) : null;
   }

   private ItemStack asItem() {
      ItemStack stack = new ItemStack(this.kind.item());
      DyeColor color = this.color();
      if (color != null) {
         StackData.edit(stack, tag -> tag.putByte("Color", (byte)color.getId()));
      }

      return stack;
   }

   public float squish(float partialTick) {
      return 1.0F + Mth.lerp(partialTick, this.squishO, (float)this.squish.value());
   }

   public void handleEntityEvent(byte id) {
      if (id == 70) {
         this.squish.kick(-9.0);
      } else if (id == 71) {
         this.squish.kick(-6.0);
      } else if (id == 72) {
         this.playSqueak();
      } else {
         super.handleEntityEvent(id);
      }
   }

   public Quaternionf orientation(float partialTick) {
      return this.drawnO.slerp(this.drawn, partialTick, new Quaternionf());
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(ORIENTATION, new Quaternionf());
      this.entityData.define(COLOR, (byte)-1);
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide) {
         Quaternionf told = (Quaternionf)this.entityData.get(ORIENTATION);
         if (!this.drawnSet) {
            this.drawn.set(told);
            this.drawnSet = true;
         }

         this.drawnO.set(this.drawn);
         this.drawn.slerp(told, 0.6F);
         this.squishO = (float)this.squish.value();
         this.squish.step(0.0, 0.05, 4.0, 0.3, 0.45);
      }
   }

   public void travel(Vec3 input) {
      if (this.isControlledByLocalInstance()) {
         if (Grip.held(this)) {
            this.holdStill();
         } else {
            boolean wet = this.isInWater();
            Vec3 before = this.getDeltaMovement().add(0.0, wet ? 0.03 : -0.08, 0.0);
            this.setDeltaMovement(before);
            this.move(MoverType.SELF, before);
            Vec3 after = this.getDeltaMovement();
            double x = after.x;
            double y = after.y;
            double z = after.z;
            boolean struck = false;
            if (this.horizontalCollision) {
               if (x == 0.0 && Math.abs(before.x) > 0.12) {
                  x = -before.x * this.kind.restitution();
                  struck = true;
               }

               if (z == 0.0 && Math.abs(before.z) > 0.12) {
                  z = -before.z * this.kind.restitution();
                  struck = true;
               }
            }

            if (this.verticalCollision && y == 0.0 && Math.abs(before.y) > 0.12) {
               y = -before.y * this.kind.restitution();
               struck = true;
            }

            if (struck) {
               float loud = (float)Math.min(1.0, before.length());
               this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WOOL_FALL, SoundSource.NEUTRAL, loud, 1.0F);
               if (before.length() > 0.4) {
                  this.squeak();
               }

               this.level().broadcastEntityEvent(this, (byte)71);
            }

            if (wet) {
               x *= 0.85;
               y *= 0.85;
               z *= 0.85;
            } else if (this.onGround()) {
               float slip = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getFriction();
               double grip = this.kind.grip();
               double keep = Mth.clamp(grip + (slip - 0.6) * (1.0 - grip) / 0.4, 0.0, 0.995);
               x *= keep;
               z *= keep;
            } else {
               double keep = 1.0 - 0.020000000000000018 / this.kind.mass();
               x *= keep;
               y *= keep;
               z *= keep;
            }

            this.setDeltaMovement(x, y, z);
            this.tumble(x, z, struck);
         }
      }
   }

   private void tumble(double x, double z, boolean struck) {
      double ground = Math.sqrt(x * x + z * z);
      if (!this.onGround() && !struck) {
         this.spin.mul(0.99F);
      } else if (ground > 0.03 && this.kind.rest() != PlushKind.Rest.FLAT) {
         float rate = (float)ground / (this.getBbWidth() * 0.5F);
         this.spin.lerp(new Vector3f((float)(z / ground) * rate, 0.0F, (float)(-x / ground) * rate), 0.5F);
      } else {
         this.spin.zero();
         this.settle();
      }

      float rate = this.spin.length();
      if (rate > 1.0E-4F) {
         this.orientation.premul(new Quaternionf().rotationAxis(rate, this.spin.x / rate, this.spin.y / rate, this.spin.z / rate)).normalize();
      }

      if (!this.orientation.equals((Quaternionfc)this.entityData.get(ORIENTATION), 1.0E-4F)) {
         this.entityData.set(ORIENTATION, new Quaternionf(this.orientation));
      }
   }

   private void holdStill() {
      this.spin.zero();
      this.orientation.slerp(new Quaternionf(), 0.3F).normalize();
      if (!this.orientation.equals((Quaternionfc)this.entityData.get(ORIENTATION), 1.0E-4F)) {
         this.entityData.set(ORIENTATION, new Quaternionf(this.orientation));
      }
   }

   private void settle() {
      Vector3f up = this.orientation.transformInverse(new Vector3f(UP));
      PlushKind.Rest rest = this.kind.rest();
      float ax = rest == PlushKind.Rest.FLAT ? -1.0F : Math.abs(up.x);
      float ay = Math.abs(up.y);
      float az = rest == PlushKind.Rest.ANY ? Math.abs(up.z) : -1.0F;
      Vector3f side = ax >= ay && ax >= az
         ? new Vector3f(up.x < 0.0F ? -1.0F : 1.0F, 0.0F, 0.0F)
         : (ay >= az ? new Vector3f(0.0F, up.y < 0.0F ? -1.0F : 1.0F, 0.0F) : new Vector3f(0.0F, 0.0F, up.z < 0.0F ? -1.0F : 1.0F));
      Quaternionf level = new Quaternionf().rotationTo(this.orientation.transform(side), UP);
      this.orientation.premul(new Quaternionf().slerp(level, 0.25F)).normalize();
   }

   public boolean hurt(DamageSource source, float amount) {
      if (this.level().isClientSide || this.isRemoved()) {
         return false;
      }

      if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         this.discard();
         return true;
      }

      Vec3 from = source.getSourcePosition();
      if (from == null) {
         return false;
      }

      Vec3 away = new Vec3(this.getX() - from.x, 0.0, this.getZ() - from.z);
      if (away.lengthSqr() < 1.0E-6) {
         float turn = this.random.nextFloat() * (float) (Math.PI * 2);
         away = new Vec3(Mth.cos(turn), 0.0, Mth.sin(turn));
      }

      away = away.normalize();
      double power = Math.min(2.0, Math.min(1.4, 0.35 + 0.06 * amount) / this.kind.mass());
      this.setDeltaMovement(this.getDeltaMovement().scale(0.5).add(away.x * power, power * 0.45, away.z * power));
      this.hasImpulse = true;
      this.tumbleAlong(away.x, away.z, power);
      this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WOOL_HIT, SoundSource.NEUTRAL, 1.0F, 1.0F);
      this.squeak();
      this.level().broadcastEntityEvent(this, (byte)71);
      return true;
   }

   public void knockback(double strength, double x, double z) {
      double power = Math.min(2.0, strength / this.kind.mass());
      super.knockback(power, x, z);
      double length = Math.sqrt(x * x + z * z);
      if (!this.level().isClientSide && !(length < 1.0E-6)) {
         this.tumbleAlong(-x / length, -z / length, power);
         this.squeak();
         this.level().broadcastEntityEvent(this, (byte)71);
      }
   }

   private void tumbleAlong(double x, double z, double power) {
      float rate = (float)power * 0.9F;
      this.spin.add((float)z * rate, (this.random.nextFloat() - 0.5F) * rate, (float)(-x) * rate);
   }

   private void squeak() {
      if (this.kind.squeakId() != null) {
         this.level().broadcastEntityEvent(this, (byte)72);
      }
   }

   private void playSqueak() {
      SoundEvent sound = this.kind.squeak();
      if (sound != null && ModClientConfig.isPlushSoundsEnabled()) {
         this.level()
            .playLocalSound(
               this.getX(),
               this.getY(),
               this.getZ(),
               sound,
               SoundSource.NEUTRAL,
               0.7F,
               this.kind.squeakPitch() + (this.random.nextFloat() - 0.5F) * 0.3F,
               false
            );
      }
   }

   public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
      return false;
   }

   public boolean canBeAffected(MobEffectInstance effect) {
      return false;
   }

   protected float tickHeadTurn(float yRot, float animStep) {
      return animStep;
   }

   public InteractionResult interact(Player player, InteractionHand hand) {
      ItemStack held = player.getItemInHand(hand);
      if (this.kind.dyeable()
         && !player.isShiftKeyDown()
         && held.getItem() instanceof DyeItem dye
         && dye.getDyeColor() != this.color()
         && PlushSkins.takes(this.kind, dye.getDyeColor())) {
         if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
         }

         this.color(dye.getDyeColor());
         if (!player.isCreative()) {
            held.shrink(1);
         }

         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.DYE_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
         this.level().broadcastEntityEvent(this, (byte)70);
         return InteractionResult.SUCCESS;
      } else if (!player.isShiftKeyDown()) {
         if (!this.level().isClientSide) {
            this.squeak();
            this.level().broadcastEntityEvent(this, (byte)70);
         }

         return InteractionResult.SUCCESS;
      } else {
         if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
         }

         ItemStack stack = this.asItem();
         if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
         }

         this.level().playSound(null, this.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.8F, 1.2F);
         this.discard();
         return InteractionResult.SUCCESS;
      }
   }

   public ItemStack getPickResult() {
      return this.asItem();
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putFloat("OrientationX", this.orientation.x);
      tag.putFloat("OrientationY", this.orientation.y);
      tag.putFloat("OrientationZ", this.orientation.z);
      tag.putFloat("OrientationW", this.orientation.w);
      if ((Byte)this.entityData.get(COLOR) >= 0) {
         tag.putByte("Color", (Byte)this.entityData.get(COLOR));
      }
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.contains("Color")) {
         this.color(DyeColor.byId(tag.getByte("Color")));
      }

      if (tag.contains("OrientationW")) {
         this.orientation
            .set(tag.getFloat("OrientationX"), tag.getFloat("OrientationY"), tag.getFloat("OrientationZ"), tag.getFloat("OrientationW"))
            .normalize();
         this.entityData.set(ORIENTATION, new Quaternionf(this.orientation));
      }
   }

   public String animSpeciesId() {
      return this.kind.model();
   }

   public RigSpec animRig() {
      return this.kind.rig();
   }

   public Iterable<ItemStack> getArmorSlots() {
      return List.of();
   }

   public ItemStack getItemBySlot(EquipmentSlot slot) {
      return ItemStack.EMPTY;
   }

   public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
   }

   public HumanoidArm getMainArm() {
      return HumanoidArm.RIGHT;
   }
}
