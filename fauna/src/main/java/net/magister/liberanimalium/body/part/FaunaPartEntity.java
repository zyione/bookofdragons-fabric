package net.magister.liberanimalium.body.part;

import net.magister.liberanimalium.body.PartSpec;
import net.magister.liberanimalium.body.StuckArrows;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaMultipartComponent;
import net.magister.liberanimalium.platform.part.PartBody;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FaunaPartEntity extends PartBody<FaunaBase> {
   public final String partName;
   private final PartSpec spec;
   private EntityDimensions size;
   private float boxWidth;
   private float boxHeight;
   private float scale = 1.0F;
   private FaunaPartEntity.Rung rung = FaunaPartEntity.Rung.NONE;
   @Nullable
   private FaunaMultipartComponent owner;

   public FaunaPartEntity(FaunaBase parent, PartSpec spec, float scale) {
      super(parent);
      this.partName = spec.name();
      this.spec = spec;
      this.resize(scale);
   }

   public PartSpec spec() {
      return this.spec;
   }

   public float getDamageMultiplier() {
      return this.spec.damage();
   }

   public boolean isHead() {
      return this.spec.head();
   }

   public FaunaPartEntity.Rung rung() {
      return this.rung;
   }

   public void resize(float scale) {
      this.scale = scale;
      float width = Math.max(0.5F, this.spec.width() * scale);
      float height = Math.max(0.5F, this.spec.height() * scale);
      this.size = EntityDimensions.scalable(width, height);
      this.boxWidth = width;
      this.boxHeight = height;
      this.refreshDimensions();
      if (this.owner != null) {
         this.owner.markUnionDirty();
      }
   }

   public void own(FaunaMultipartComponent component) {
      this.owner = component;
   }

   public float scale() {
      return this.scale;
   }

   public void place(Vec3 bone, Vec3 toward, FaunaPartEntity.Rung rung) {
      this.xo = this.getX();
      this.yo = this.getY();
      this.zo = this.getZ();
      this.xOld = this.xo;
      this.yOld = this.yo;
      this.zOld = this.zo;
      this.rung = rung;
      double x = bone.x;
      double y = bone.y - this.boxHeight / 2.0;
      double z = bone.z;
      this.setPos(x, y, z);
      AABB box = this.size.makeBoundingBox(x, y, z);
      if (toward != null) {
         double sx = bone.x + (toward.x - bone.x) * this.spec.stretch();
         double sy = bone.y + (toward.y - bone.y) * this.spec.stretch();
         double sz = bone.z + (toward.z - bone.z) * this.spec.stretch();
         double halfWidth = this.boxWidth / 2.0;
         double halfHeight = this.boxHeight / 2.0;
         box = new AABB(
            Math.min(box.minX, sx - halfWidth),
            Math.min(box.minY, sy - halfHeight),
            Math.min(box.minZ, sz - halfWidth),
            Math.max(box.maxX, sx + halfWidth),
            Math.max(box.maxY, sy + halfHeight),
            Math.max(box.maxZ, sz + halfWidth)
         );
      }

      this.setBoundingBox(box);
      if (this.owner != null) {
         this.owner.markUnionDirty();
      }
   }

   public Vec3 centre() {
      return this.getBoundingBox().getCenter();
   }

   public boolean isPickable() {
      return true;
   }

   public Entity getRootVehicle() {
      Entity parent = this.getParent();
      return (Entity)(parent == null ? this : parent.getRootVehicle());
   }

   public boolean canBeCollidedWith() {
      FaunaBase parent = (FaunaBase)this.getParent();
      return this.spec.walkable() && parent != null && parent.stoodOn();
   }

   public boolean startRiding(@NotNull Entity entity, boolean force) {
      Entity parent = this.getParent();
      return parent != null ? entity.startRiding(parent, force) : false;
   }

   public void addPassenger(@NotNull Entity passenger) {
      Entity parent = this.getParent();
      if (parent != null) {
         passenger.startRiding(parent, true);
      } else {
         super.addPassenger(passenger);
      }
   }

   protected boolean canAddPassenger(@NotNull Entity passenger) {
      return false;
   }

   public InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
      Entity parent = this.getParent();
      return parent == null ? InteractionResult.PASS : parent.interact(player, hand);
   }

   public InteractionResult interactAt(@NotNull Player player, @NotNull Vec3 vec, @NotNull InteractionHand hand) {
      Entity parent = this.getParent();
      return parent == null ? InteractionResult.PASS : parent.interactAt(player, vec, hand);
   }

   public boolean hurt(@NotNull DamageSource source, float amount) {
      if (this.level().isClientSide) {
         return !this.isInvulnerableTo(source);
      }

      if (this.isInvulnerableTo(source)) {
         return false;
      }

      FaunaBase parent = (FaunaBase)this.getParent();
      return parent == null ? false : StuckArrows.through(this.spec.bone(), () -> parent.hurtThrough(this, source, amount * this.spec.damage()));
   }

   public boolean is(@NotNull Entity entity) {
      return this == entity || this.getParent() == entity;
   }

   protected void defineSynchedData() {
   }

   protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
   }

   protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
   }

   @NotNull
   public EntityDimensions getDimensions(@NotNull Pose pose) {
      return this.size;
   }

   public boolean shouldBeSaved() {
      return false;
   }

   public enum Rung {
      NONE,
      REPORT,
      FALLBACK,
      MODEL;
   }
}
