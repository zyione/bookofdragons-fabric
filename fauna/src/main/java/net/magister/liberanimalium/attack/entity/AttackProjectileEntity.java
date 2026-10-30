package net.magister.liberanimalium.attack.entity;

import java.util.List;
import net.magister.liberanimalium.attack.AttackManager;
import net.magister.liberanimalium.attack.data.AttackDefinition;
import net.magister.liberanimalium.attack.data.EmitterConfig;
import net.magister.liberanimalium.attack.data.ImpactConfig;
import net.magister.liberanimalium.attack.data.LightConfig;
import net.magister.liberanimalium.attack.data.ProjectileEmitter;
import net.magister.liberanimalium.attack.data.VisualEmitter;
import net.magister.liberanimalium.attack.data.impact.ExplosionImpact;
import net.magister.liberanimalium.attack.data.impact.ExtinguishImpact;
import net.magister.liberanimalium.attack.data.impact.SlideImpact;
import net.magister.liberanimalium.attack.impact.ImpactEffect;
import net.magister.liberanimalium.attack.light.LightScribe;
import net.magister.liberanimalium.attack.movement.MovementModifier;
import net.magister.liberanimalium.attack.runtime.AttackInvoker;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.melee.CombatReach;
import net.magister.liberanimalium.compat.Entities;
import net.magister.liberanimalium.interop.SubLevels;
import net.magister.liberanimalium.platform.part.Parts;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector3f;

public class AttackProjectileEntity extends ThrowableProjectile {
   private static final EntityDataAccessor<String> DATA_ATTACK_ID = SynchedEntityData.defineId(AttackProjectileEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Float> DATA_CHARGE_SCALE = SynchedEntityData.defineId(AttackProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_EMITTER_INDEX = SynchedEntityData.defineId(
      AttackProjectileEntity.class, EntityDataSerializers.INT
   );
   private static final EntityDataAccessor<Float> DATA_SLIDE_ALPHA = SynchedEntityData.defineId(AttackProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Vector3f> DATA_LAUNCH_OFFSET = SynchedEntityData.defineId(
      AttackProjectileEntity.class, EntityDataSerializers.VECTOR3
   );
   private int flightTime = 0;
   private int delayTicks = 0;
   private boolean hitSurface = false;
   private int slideBouncesRemaining = -1;
   private int slideTick = 0;
   private ResourceLocation cachedAttackId;
   private Vec3 drawShift = Vec3.ZERO;
   private float launchSpeed;
   private float flown;

   public AttackProjectileEntity(EntityType<? extends ThrowableProjectile> type, Level level) {
      super(type, level);
   }

   public AttackProjectileEntity(
      EntityType<? extends ThrowableProjectile> type, Level level, LivingEntity owner, ResourceLocation attackId, float chargeScale, int emitterIndex
   ) {
      super(type, owner, level);
      this.entityData.set(DATA_ATTACK_ID, attackId.toString());
      this.entityData.set(DATA_CHARGE_SCALE, chargeScale);
      this.entityData.set(DATA_EMITTER_INDEX, emitterIndex);
   }

   protected void defineSynchedData() {
      this.entityData.define(DATA_ATTACK_ID, "");
      this.entityData.define(DATA_CHARGE_SCALE, 1.0F);
      this.entityData.define(DATA_EMITTER_INDEX, 0);
      this.entityData.define(DATA_SLIDE_ALPHA, 1.0F);
      this.entityData.define(DATA_LAUNCH_OFFSET, new Vector3f());
   }

   public void markLaunch() {
      Entity owner = this.getOwner();
      if (owner != null) {
         Vec3 offset = this.position().subtract(owner.position());
         this.entityData.set(DATA_LAUNCH_OFFSET, new Vector3f((float)offset.x, (float)offset.y, (float)offset.z));
      }
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (this.level().isClientSide() && DATA_LAUNCH_OFFSET.equals(key) && this.getOwner() != null) {
         Vector3f offset = (Vector3f)this.entityData.get(DATA_LAUNCH_OFFSET);
         Vec3 drawn = this.getOwner().position().add(offset.x(), offset.y(), offset.z());
         this.drawShift = drawn.subtract(this.position());
         this.setPos(drawn.x, drawn.y, drawn.z);
         this.xo = this.xOld = drawn.x;
         this.yo = this.yOld = drawn.y;
         this.zo = this.zOld = drawn.z;
      }
   }

   public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
      super.lerpTo(x + this.drawShift.x, y + this.drawShift.y, z + this.drawShift.z, yRot, xRot, steps, teleport);
   }

   public int getSlideBouncesRemaining() {
      return this.slideBouncesRemaining;
   }

   public void setSlideBouncesRemaining(int bounces) {
      if (this.slideBouncesRemaining < 0) {
         this.slideTick = 0;
         this.entityData.set(DATA_SLIDE_ALPHA, 1.0F);
      }

      this.slideBouncesRemaining = bounces;
   }

   public float getSlideAlpha() {
      return (Float)this.entityData.get(DATA_SLIDE_ALPHA);
   }

   private ResourceLocation attackIdLocation() {
      if (this.cachedAttackId == null) {
         String id = (String)this.entityData.get(DATA_ATTACK_ID);
         if (id.isEmpty()) {
            return null;
         }

         this.cachedAttackId = new ResourceLocation(id);
      }

      return this.cachedAttackId;
   }

   public AttackDefinition getAttackDef() {
      ResourceLocation id = this.attackIdLocation();
      return id == null ? null : AttackManager.getInstance().getAttack(id);
   }

   public ProjectileEmitter getProjectileEmitter() {
      AttackDefinition def = this.getAttackDef();
      if (def == null) {
         return null;
      } else {
         int idx = (Integer)this.entityData.get(DATA_EMITTER_INDEX);
         if (idx >= 0 && idx < def.emitters().size()) {
            return def.emitters().get(idx) instanceof ProjectileEmitter pe ? pe : null;
         } else {
            return null;
         }
      }
   }

   public int getFlightTime() {
      return this.flightTime;
   }

   public void setDelayTicks(int delayTicks) {
      this.delayTicks = delayTicks;
   }

   public float getChargeScale() {
      return (Float)this.entityData.get(DATA_CHARGE_SCALE);
   }

   public String getAttackId() {
      return (String)this.entityData.get(DATA_ATTACK_ID);
   }

   public void setupTargetDirection(Vec3 direction, float speed, Vec3 carried) {
      this.launchSpeed = speed;
      Vec3 vel = direction.normalize().scale(speed).add(carried);
      this.setDeltaMovement(vel);
      float yaw = (float)(Mth.atan2(vel.x, vel.z) * (180.0 / Math.PI));
      float pitch = (float)(Mth.atan2(vel.y, vel.horizontalDistance()) * (180.0 / Math.PI));
      this.setYRot(yaw);
      this.setXRot(pitch);
      this.yRotO = yaw;
      this.xRotO = pitch;
   }

   public void tick() {
      if (this.delayTicks > 0) {
         this.delayTicks--;
      } else {
         super.tick();
         if (!this.isRemoved()) {
            this.flightTime++;
            AttackDefinition def = this.getAttackDef();
            if (def == null) {
               if (!this.level().isClientSide) {
                  this.discard();
               }
            } else {
               if (!this.level().isClientSide) {
                  this.setNoGravity(true);
                  if (this.isInWater() && hasExtinguish(def)) {
                     this.handleImpact(null);
                     return;
                  }

                  this.flown = this.flown + (float)this.position().distanceTo(new Vec3(this.xo, this.yo, this.zo));
                  float burst = this.airburstDistance(def);
                  if (burst > 0.0F && this.flown >= burst) {
                     this.handleImpact(null);
                     return;
                  }

                  ProjectileEmitter pe = this.getProjectileEmitter();
                  if (pe == null) {
                     if (this.flightTime > 60) {
                        this.discard();
                        return;
                     }
                  } else {
                     Vec3 vel = this.getDeltaMovement();

                     for (MovementModifier mod : pe.sim().movement()) {
                        vel = mod.apply(vel, this, this.flightTime);
                     }

                     this.setDeltaMovement(vel);
                     if (this.flightTime > pe.sim().lifetimeTicks()) {
                        this.discard();
                        return;
                     }
                  }

                  this.tickSlide(def);
                  this.lightFlight(def);
               } else {
                  this.spawnTrailParticles(def);
               }
            }
         }
      }
   }

   private void spawnTrailParticles(AttackDefinition def) {
      if (!this.hitSurface && !this.onGround() && !(this.getDeltaMovement().lengthSqr() < 0.05)) {
         List<EmitterConfig> emitters = def.emitters();
         Vec3 vel = this.getDeltaMovement();
         ResourceLocation id = this.attackIdLocation();

         for (int i = 0; i < emitters.size(); i++) {
            if (emitters.get(i) instanceof VisualEmitter visual
               && visual.isProjectileAnchored()
               && this.flightTime % Math.max(1, visual.emission().rateTicks()) == 0) {
               AttackInvoker.spawnVisualParticles(
                  this.level(), this.position(), vel, id, visual, i, 1.0F, this.getId() * -7046029254386353131L, this.flightTime, Vec3.ZERO
               );
            }
         }
      }
   }

   private void lightFlight(AttackDefinition def) {
      if (!def.light().isEmpty() && def.light().get().projectile()) {
         if (this.level() instanceof ServerLevel serverLevel) {
            LightConfig var6 = def.light().get();
            if ((this.tickCount + this.getId()) % var6.everyTicks() == 0) {
               Entity owner = this.getOwner();
               int ownerId = owner != null ? owner.getId() : this.getId();
               LightScribe.of(serverLevel).illuminate(serverLevel, this.position(), ownerId, this.getId(), var6, 1.0F);
            }
         }
      }
   }

   public void remove(RemovalReason reason) {
      if (reason.shouldDestroy() && this.level() instanceof ServerLevel serverLevel) {
         LightScribe.of(serverLevel).release(serverLevel, this.getId());
      }

      super.remove(reason);
   }

   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide) {
         AttackDefinition def = this.getAttackDef();
         if (def == null) {
            this.discard();
         } else {
            Entity target = result.getEntity();
            Vec3 from = new Vec3(this.xo, this.yo, this.zo);
            Vec3 nearest = CombatReach.closestPoint(target, from);
            if (this.level().clip(new ClipContext(from, nearest, Block.COLLIDER, Fluid.NONE, this)).getType() != Type.MISS) {
               this.handleImpact(null);
            } else {
               Entity owner = this.getOwner();
               DamageSource source = this.damageSources().thrown(this, (Entity)(owner != null ? owner : this));
               float power = owner instanceof FaunaBase body ? body.getStatSheet().breathPower() : 1.0F;
               target.hurt(source, def.collision().damage() * power);
               if (def.collision().fireSeconds() > 0) {
                  Entities.ignite(target, def.collision().fireSeconds());
               }

               this.handleImpact(null);
            }
         }
      }
   }

   protected void onHit(HitResult result) {
      super.onHit(result);
      this.hitSurface = true;
      if (!this.level().isClientSide) {
         this.handleImpact(SubLevels.inWorld(this.level(), result));
      }
   }

   private void tickSlide(AttackDefinition def) {
      if (this.slideBouncesRemaining >= 0) {
         SlideImpact slide = firstSlide(def);
         if (slide != null) {
            this.slideTick++;
            this.setDeltaMovement(this.getDeltaMovement().scale(slide.friction()));
            float alpha = Math.max(0.0F, this.getSlideAlpha() - slide.fadeRate());
            this.entityData.set(DATA_SLIDE_ALPHA, alpha);
            if (alpha <= 0.0F || this.slideTick >= slide.lifetimeTicks()) {
               this.discard();
            }
         }
      }
   }

   private static SlideImpact firstSlide(AttackDefinition def) {
      for (ImpactConfig ic : def.impact()) {
         if (ic instanceof SlideImpact s) {
            return s;
         }
      }

      return null;
   }

   private static boolean hasExtinguish(AttackDefinition def) {
      for (ImpactConfig ic : def.impact()) {
         if (ic instanceof ExtinguishImpact) {
            return true;
         }
      }

      return false;
   }

   public float speedRatio() {
      return this.launchSpeed > 0.0F ? (float)(this.getDeltaMovement().length() / this.launchSpeed) : 1.0F;
   }

   private float airburstDistance(AttackDefinition def) {
      for (ImpactConfig impact : def.impact()) {
         if (impact instanceof ExplosionImpact ex && ex.airburst().isPresent()) {
            return ex.airburst().get().distance(this.getChargeScale());
         }
      }

      return 0.0F;
   }

   private float impactShare(AttackDefinition def) {
      for (ImpactConfig impact : def.impact()) {
         if (impact instanceof ExplosionImpact ex) {
            return ex.share(ex.power(this.getChargeScale(), this.speedRatio()));
         }
      }

      return 0.0F;
   }

   private void handleImpact(HitResult hit) {
      if (!this.isRemoved()) {
         AttackDefinition def = this.getAttackDef();
         if (def == null) {
            this.discard();
         } else {
            float share = this.impactShare(def);
            Vec3 impactPos = hit != null ? hit.getLocation() : this.position();
            boolean consumed = false;

            for (ImpactConfig impact : def.impact()) {
               ImpactEffect effect = ImpactEffect.byType(impact.type());
               if (effect != null && effect.apply(this, hit, impact)) {
                  consumed = true;
                  break;
               }
            }

            if (!consumed) {
               this.discard();
            }

            if (this.isRemoved() && this.level() instanceof ServerLevel serverLevel) {
               spawnImpactParticles(serverLevel, def, this.attackIdLocation(), impactPos, share);
               this.flashImpact(serverLevel, def, impactPos);
            }
         }
      }
   }

   public void showImpact(ServerLevel level, AttackDefinition def, ResourceLocation attackId) {
      spawnImpactParticles(level, def, attackId, this.position(), this.impactShare(def));
      this.flashImpact(level, def, this.position());
   }

   private void flashImpact(ServerLevel level, AttackDefinition def, Vec3 pos) {
      if (!def.light().isEmpty() && def.light().get().impactLevel() > 0) {
         Vec3 back = this.getDeltaMovement().lengthSqr() < 1.0E-6 ? Vec3.ZERO : this.getDeltaMovement().normalize().scale(-0.5);
         Entity owner = this.getOwner();
         int ownerId = owner != null ? owner.getId() : this.getId();
         LightScribe.of(level).illuminate(level, pos.add(back), ownerId, this.getId(), def.light().get().impactFlash(), 1.0F);
      }
   }

   private static void spawnImpactParticles(ServerLevel level, AttackDefinition def, ResourceLocation id, Vec3 pos, float share) {
      List<EmitterConfig> emitters = def.emitters();

      for (int i = 0; i < emitters.size(); i++) {
         if (emitters.get(i) instanceof VisualEmitter visual && visual.isImpactAnchored()) {
            AttackInvoker.spawnImpactParticles(level, pos, id, visual, i, share);
         }
      }
   }

   protected boolean canHitEntity(Entity entity) {
      if (entity == this.getOwner()) {
         return false;
      } else if (Parts.isPart(entity) && Parts.parentOf(entity) == this.getOwner()) {
         return false;
      } else {
         Entity struck = Parts.isPart(entity) ? Parts.parentOf(entity) : entity;
         if (FaunaBase.isFriendlyFire(this.getOwner(), struck)) {
            return false;
         } else {
            return this.tickCount < 2 ? false : super.canHitEntity(entity);
         }
      }
   }

   public boolean displayFireAnimation() {
      return false;
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putString("AttackId", (String)this.entityData.get(DATA_ATTACK_ID));
      tag.putFloat("ChargeScale", (Float)this.entityData.get(DATA_CHARGE_SCALE));
      tag.putInt("EmitterIndex", (Integer)this.entityData.get(DATA_EMITTER_INDEX));
      tag.putInt("FlightTime", this.flightTime);
      tag.putFloat("Flown", this.flown);
      tag.putInt("SlideBouncesRemaining", this.slideBouncesRemaining);
      tag.putInt("SlideTick", this.slideTick);
      tag.putFloat("SlideAlpha", (Float)this.entityData.get(DATA_SLIDE_ALPHA));
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.entityData.set(DATA_ATTACK_ID, tag.getString("AttackId"));
      this.cachedAttackId = null;
      this.entityData.set(DATA_CHARGE_SCALE, tag.getFloat("ChargeScale"));
      this.entityData.set(DATA_EMITTER_INDEX, tag.getInt("EmitterIndex"));
      this.flightTime = tag.getInt("FlightTime");
      this.flown = tag.getFloat("Flown");
      this.slideBouncesRemaining = tag.contains("SlideBouncesRemaining") ? tag.getInt("SlideBouncesRemaining") : -1;
      this.slideTick = tag.getInt("SlideTick");
      this.entityData.set(DATA_SLIDE_ALPHA, tag.contains("SlideAlpha") ? tag.getFloat("SlideAlpha") : 1.0F);
   }
}
