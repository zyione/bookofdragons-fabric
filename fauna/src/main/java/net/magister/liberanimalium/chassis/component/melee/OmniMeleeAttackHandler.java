package net.magister.liberanimalium.chassis.component.melee;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayProfile;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.CombatComponent;
import net.magister.liberanimalium.chassis.component.melee.data.MeleeAttackDefinition;
import net.magister.liberanimalium.chassis.component.ranged.data.FaunaAttackManager;
import net.magister.liberanimalium.compat.Effects;
import net.magister.liberanimalium.fauna.identity.TemperTuning;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.platform.part.Parts;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class OmniMeleeAttackHandler implements IMeleeAttackHandler {
   public static EntityDataAccessor<Integer> EDA_MELEE_PHASE;
   public static EntityDataAccessor<Integer> EDA_MELEE_SLOT;
   public static final int PHASE_IDLE = 0;
   public static final int PHASE_WINDUP = 1;
   public static final int PHASE_ACTIVE = 2;
   public static final int PHASE_RECOVERY = 3;
   private FaunaBase body;
   private List<ResourceLocation> attackIds;
   private int phaseTicks = 0;
   private int[] cooldowns;
   private boolean hitDealtThisCycle = false;
   @Nullable
   private LivingEntity victim;
   private boolean inPlay;
   private static final float PLAY_KNOCKBACK = 2.5F;
   private static final float PLAY_KNOCKBACK_MIN = 0.8F;
   private static final float PLAY_KNOCKBACK_MAX = 1.6F;
   private final Set<Integer> reserved = new HashSet<>();
   static final ResourceLocation NO_ATTACK = new ResourceLocation("liberanimalium", "no_attack");
   private boolean fromGameplay;
   private static final int DEFAULT_COOLDOWN_TICKS = 40;

   public OmniMeleeAttackHandler(ResourceLocation attackId, ResourceLocation... whileCooling) {
      List<ResourceLocation> ids = new ArrayList<>();
      ids.add(attackId);
      ids.addAll(List.of(whileCooling));
      this.attackIds = List.copyOf(ids);
      this.cooldowns = new int[this.attackIds.size()];
   }

   public static OmniMeleeAttackHandler fromGameplay() {
      OmniMeleeAttackHandler handler = new OmniMeleeAttackHandler(NO_ATTACK);
      handler.fromGameplay = true;
      return handler;
   }

   private void followGameplay() {
      if (this.fromGameplay && this.body != null) {
         SpeciesGameplayProfile profile = this.body.gameplay();
         if (profile != null && !profile.melee().isEmpty()) {
            this.retune(profile.melee(), profile.meleeReserved());
         }
      }
   }

   void retune(List<ResourceLocation> ids, Set<ResourceLocation> heldBack) {
      Set<Integer> heldSlots = new HashSet<>();

      for (ResourceLocation id : heldBack) {
         int slot = ids.indexOf(id);
         if (slot >= 0) {
            heldSlots.add(slot);
         }
      }

      if (!ids.equals(this.attackIds) || !heldSlots.equals(this.reserved)) {
         int[] next = new int[ids.size()];

         for (int i = 0; i < next.length; i++) {
            int before = this.attackIds.indexOf(ids.get(i));
            next[i] = before >= 0 ? this.cooldowns[before] : 0;
         }

         ResourceLocation running = this.body != null && EDA_MELEE_SLOT != null ? this.attackIds.get(this.slotIndex()) : null;
         this.attackIds = List.copyOf(ids);
         this.cooldowns = next;
         this.reserved.clear();
         this.reserved.addAll(heldSlots);
         if (running != null) {
            this.setSlot(Math.max(0, this.attackIds.indexOf(running)));
         }
      }
   }

   private int slotIndex() {
      int slot = EDA_MELEE_SLOT != null ? this.body.<Integer>getData(EDA_MELEE_SLOT) : 0;
      return slot >= 0 && slot < this.attackIds.size() ? slot : 0;
   }

   public OmniMeleeAttackHandler reserve(ResourceLocation attackId) {
      int slot = this.attackIds.indexOf(attackId);
      if (slot < 0) {
         throw new IllegalArgumentException("not one of this handler's attacks: " + attackId);
      }

      this.reserved.add(slot);
      return this;
   }

   public boolean attackWith(ResourceLocation attackId, @Nullable LivingEntity intended) {
      this.followGameplay();
      int slot = this.attackIds.indexOf(attackId);
      if (slot >= 0 && !this.isAttacking() && !this.body.getSyncedIsIncapacitated() && this.cooldowns[slot] <= 0) {
         MeleeAttackDefinition config = this.configOf(slot);
         if (config != null && this.body.getStaminaSystem().tryConsume(config.staminaCost())) {
            this.victim = intended;
            this.setSlot(slot);
            this.enterWindup(config);
            this.body.swing(InteractionHand.MAIN_HAND);
            config.swingSound().ifPresent(this::playSound);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Nullable
   public MeleeAttackDefinition definitionOf(ResourceLocation attackId) {
      this.followGameplay();
      int slot = this.attackIds.indexOf(attackId);
      return slot < 0 ? null : this.configOf(slot);
   }

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
      this.followGameplay();
   }

   @Override
   public void serverTick() {
      this.followGameplay();

      for (int i = 0; i < this.cooldowns.length; i++) {
         if (this.cooldowns[i] > 0) {
            this.cooldowns[i]--;
         }
      }

      int phase = this.getPhase();
      if (phase != 0) {
         this.phaseTicks--;
         if (phase == 1) {
            if (this.phaseTicks <= 0) {
               this.enterActive();
            }
         } else if (phase == 2) {
            if (!this.hitDealtThisCycle && this.dealHit()) {
               this.hitDealtThisCycle = true;
            }

            if (this.phaseTicks <= 0) {
               this.enterRecovery();
            }
         } else if (phase == 3 && this.phaseTicks <= 0) {
            this.enterIdle();
         }
      }
   }

   @Override
   public boolean isAttacking() {
      return this.getPhase() != 0;
   }

   @Override
   public void handlePlayerAttack(boolean pressed) {
      if (pressed) {
         this.attack(null);
      }
   }

   @Override
   public boolean attack(@Nullable LivingEntity intended) {
      this.followGameplay();
      return this.begin(intended, false);
   }

   @Override
   public boolean shove(LivingEntity intended) {
      this.followGameplay();
      return intended != null && this.begin(intended, true);
   }

   private boolean begin(@Nullable LivingEntity intended, boolean play) {
      if (!this.isAttacking() && !this.body.getSyncedIsIncapacitated()) {
         int slot = this.readySlot();
         if (slot < 0) {
            return false;
         }

         MeleeAttackDefinition config = this.configOf(slot);
         if (!this.body.getStaminaSystem().tryConsume(config.staminaCost())) {
            return false;
         }

         this.victim = intended;
         this.inPlay = play;
         this.setSlot(slot);
         this.enterWindup(config);
         this.body.swing(InteractionHand.MAIN_HAND);
         config.swingSound().ifPresent(this::playSound);
         return true;
      } else {
         return false;
      }
   }

   @Nullable
   @Override
   public LivingEntity committedVictim() {
      return this.victim != null && this.victim.isAlive() && this.getPhase() != 0 ? this.victim : null;
   }

   @Override
   public boolean canAttack() {
      this.followGameplay();
      if (this.isAttacking()) {
         return false;
      } else {
         return this.body.getSyncedIsIncapacitated() ? false : this.readySlot() >= 0;
      }
   }

   private int readySlot() {
      boolean[] usable = new boolean[this.attackIds.size()];

      for (int i = 0; i < usable.length; i++) {
         MeleeAttackDefinition config = this.configOf(i);
         usable[i] = !this.reserved.contains(i) && config != null && this.body.getStaminaSystem().hasStamina(config.staminaCost());
      }

      return firstReady(this.cooldowns, usable);
   }

   static int firstReady(int[] cooldowns, boolean[] usable) {
      for (int i = 0; i < cooldowns.length; i++) {
         if (cooldowns[i] <= 0 && usable[i]) {
            return i;
         }
      }

      return -1;
   }

   private int upcomingSlot() {
      int ready = this.readySlot();
      if (ready >= 0) {
         return ready;
      }

      int soonest = -1;

      for (int i = 0; i < this.cooldowns.length; i++) {
         if (!this.reserved.contains(i) && (soonest < 0 || this.cooldowns[i] < this.cooldowns[soonest])) {
            soonest = i;
         }
      }

      return soonest < 0 ? 0 : soonest;
   }

   private void enterWindup(MeleeAttackDefinition config) {
      this.hitDealtThisCycle = false;
      this.phaseTicks = config.windupTicks();
      this.setPhase(1);
   }

   private void enterActive() {
      MeleeAttackDefinition config = this.getConfig();
      this.phaseTicks = config != null ? config.activeTicks() : 4;
      this.setPhase(2);
   }

   private void enterRecovery() {
      MeleeAttackDefinition config = this.getConfig();
      this.phaseTicks = config != null ? config.recoveryTicks() : 10;
      this.setPhase(3);
   }

   private void enterIdle() {
      MeleeAttackDefinition config = this.getConfig();
      this.cooldowns[this.slot()] = this.pacedByTemper(config != null ? config.cooldownTicks() : 40);
      this.phaseTicks = 0;
      this.victim = null;
      this.inPlay = false;
      this.setPhase(0);
   }

   private int pacedByTemper(int authored) {
      if (this.body != null && this.body.faunaMind() != null) {
         float pace = TemperTuning.paceMultiplier(this.body.faunaMind().temper().get(Trait.ENERGY));
         return Math.max(1, Math.round(authored / pace));
      } else {
         return authored;
      }
   }

   private boolean dealHit() {
      if (this.body.level().isClientSide()) {
         return false;
      }

      MeleeAttackDefinition config = this.getConfig();
      if (config == null) {
         return false;
      }

      Vec3 forward = this.body.getHeadTrackingSystem().headLookDirection();
      double reach = config.reach();
      Vec3 primaryHead = CombatReach.biteOrigin(this.body, this.body.position());
      double neckExtent = primaryHead.distanceTo(this.body.position());
      AABB searchBox = this.body.getBoundingBox().inflate(reach + neckExtent);
      List<Entity> candidates = this.body.level().getEntities(this.body, searchBox);
      float halfAngleRad = (float)Math.toRadians(config.hitboxAngleDegrees() * 0.5F);
      CombatComponent combat = this.body.componentRegistry.get(CombatComponent.class);
      boolean connected = false;
      Set<Integer> seen = new HashSet<>();

      for (Entity candidate : candidates) {
         if (Parts.isPart(candidate)) {
            candidate = Parts.parentOf(candidate);
         }

         if (candidate != null
            && candidate != this.body
            && candidate instanceof LivingEntity living
            && living.isAlive()
            && seen.add(living.getId())
            && (!this.inPlay || living == this.victim)
            && (this.inPlay || !this.body.isTame() || !this.body.isFriendlyTarget(living))
            && (living == this.victim || !this.body.spares(living))) {
            Vec3 origin = CombatReach.biteOrigin(this.body, living.position());
            Vec3 nearest = CombatReach.closestPoint(living, origin);
            Vec3 toTarget = nearest.subtract(origin);
            double dist = toTarget.length();
            if (!(dist > reach) && CombatReach.unblocked(this.body, origin, living)) {
               Vec3 aim = CombatReach.aimOrigin(this.body);
               Vec3 toAimed = CombatReach.closestPoint(living, aim).subtract(aim);
               if (toAimed.lengthSqr() > 1.0E-4) {
                  float angle = (float)Math.acos(Mth.clamp(forward.dot(toAimed.normalize()), -1.0, 1.0));
                  if (angle > halfAngleRad) {
                     continue;
                  }
               }

               if (this.inPlay) {
                  float yaw = this.body.getYRot() * (float) (Math.PI / 180.0);
                  living.knockback(Mth.clamp(config.knockback() * 2.5F, 0.8F, 1.6F), Mth.sin(yaw), -Mth.cos(yaw));
                  living.hurtMarked = true;
                  config.hitSound().ifPresent(this::playSound);
                  connected = true;
               } else if (combat != null) {
                  float baseDamage = (float)this.body.getAttributeValue(Attributes.ATTACK_DAMAGE);
                  float scaledDamage = config.damage().orElse(baseDamage * config.damageMultiplier());
                  boolean hit = living.hurt(this.body.damageSources().mobAttack(this.body), scaledDamage);
                  if (hit) {
                     float kbYaw = this.body.getYRot() * (float) (Math.PI / 180.0);
                     living.knockback(config.knockback(), Mth.sin(kbYaw), -Mth.cos(kbYaw));
                     config.hitSound().ifPresent(this::playSound);
                     config.onHit().ifPresent(onHit -> this.applyOnHit(onHit, living));
                     if (this.body.getNeedsSystem() != null) {
                        this.body.getNeedsSystem().onAttack();
                     }

                     connected = true;
                  }
               }
            }
         }
      }

      return connected;
   }

   private void applyOnHit(MeleeAttackDefinition.OnHit onHit, LivingEntity living) {
      if (!(onHit.chance() < 1.0F) || !(this.body.getRandom().nextFloat() >= onHit.chance())) {
         Effects.instance(onHit.effect(), onHit.durationTicks(), onHit.amplifier()).ifPresent(effect -> living.addEffect(effect, this.body));
      }
   }

   private void playSound(String soundId) {
      try {
         ResourceLocation loc = ResourceLocation.tryParse(soundId);
         if (loc != null) {
            this.body.playSound(SoundEvent.createVariableRangeEvent(loc), 1.0F, 1.0F);
         }
      } catch (Exception var3) {
      }
   }

   @Override
   public double meleeReach() {
      this.followGameplay();
      MeleeAttackDefinition config = this.configOf(this.upcomingSlot());
      return config == null ? 0.0 : config.reach() * 0.8;
   }

   public MeleeAttackDefinition getConfig() {
      this.followGameplay();
      return this.configOf(this.slot());
   }

   @Nullable
   private MeleeAttackDefinition configOf(int slot) {
      return FaunaAttackManager.getInstance().getMeleeAttack(this.attackIds.get(slot));
   }

   public int getPhase() {
      return EDA_MELEE_PHASE != null ? this.body.<Integer>getData(EDA_MELEE_PHASE) : 0;
   }

   private void setPhase(int phase) {
      if (EDA_MELEE_PHASE != null) {
         this.body.setData(EDA_MELEE_PHASE, phase);
      }
   }

   public int slot() {
      this.followGameplay();
      int slot = EDA_MELEE_SLOT != null ? this.body.<Integer>getData(EDA_MELEE_SLOT) : 0;
      return slot >= 0 && slot < this.attackIds.size() ? slot : 0;
   }

   private void setSlot(int slot) {
      if (EDA_MELEE_SLOT != null) {
         this.body.setData(EDA_MELEE_SLOT, slot);
      }
   }

   public ResourceLocation getAttackId() {
      this.followGameplay();
      return this.attackIds.get(0);
   }

   @Override
   public void save(CompoundTag tag) {
      this.followGameplay();
      tag.putInt("MeleePhase", this.getPhase());
      tag.putInt("MeleeCooldown", this.cooldowns[0]);

      for (int i = 1; i < this.cooldowns.length; i++) {
         tag.putInt("MeleeCooldown" + i, this.cooldowns[i]);
      }

      tag.putInt("MeleeSlot", this.slot());
   }

   @Override
   public void load(CompoundTag tag) {
      this.followGameplay();
      if (tag.contains("MeleePhase")) {
         this.setPhase(tag.getInt("MeleePhase"));
         this.cooldowns[0] = tag.getInt("MeleeCooldown");

         for (int i = 1; i < this.cooldowns.length; i++) {
            this.cooldowns[i] = tag.getInt("MeleeCooldown" + i);
         }

         int slot = tag.getInt("MeleeSlot");
         this.setSlot(slot >= 0 && slot < this.attackIds.size() ? slot : 0);
      }
   }
}
