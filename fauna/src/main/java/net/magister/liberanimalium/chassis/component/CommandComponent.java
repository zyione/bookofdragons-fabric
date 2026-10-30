package net.magister.liberanimalium.chassis.component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.Notices;
import net.magister.liberanimalium.chassis.component.command.Modifier;
import net.magister.liberanimalium.chassis.component.command.Stance;
import net.magister.liberanimalium.fauna.api.Order;
import net.magister.liberanimalium.fauna.core.FaunaMind;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class CommandComponent implements IFaunaComponent {
   private static final String TAG_STANCE = "CommandStance";
   private static final String TAG_MODIFIER = "CommandModifier";
   private static final String TAG_LAST_MODIFIER_PREFIX = "CommandLast_";
   private static final String TAG_LEGACY_COMMAND = "Command";
   private static final String TAG_HOME = "CommandHome";
   private static final int LEGACY_WANDER = 0;
   private static final int LEGACY_SIT = 1;
   private static final int LEGACY_FOLLOW = 2;
   private static final int BREAK_SUPPRESS_TICKS = 40;
   protected FaunaBase body;
   private Stance stance = Stance.FREE;
   private Modifier modifier = Modifier.GROUND;
   private final Map<Stance, Modifier> lastModifierPerStance = new EnumMap<>(Stance.class);
   private long freedAt = Long.MIN_VALUE;
   @Nullable
   private BlockPos home;
   private int disobeyLockoutTicks = 0;
   private Stance lockoutStance = null;

   public CommandComponent() {
      for (Stance s : Stance.values()) {
         this.lastModifierPerStance.put(s, s.defaultModifier());
      }
   }

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   @Override
   public void serverTick() {
      if (this.disobeyLockoutTicks > 0) {
         this.disobeyLockoutTicks--;
      }
   }

   @Override
   public void save(CompoundTag tag) {
      tag.putByte("CommandStance", (byte)this.stance.ordinal());
      tag.putByte("CommandModifier", (byte)this.modifier.ordinal());

      for (Entry<Stance, Modifier> e : this.lastModifierPerStance.entrySet()) {
         tag.putByte("CommandLast_" + e.getKey().name(), (byte)e.getValue().ordinal());
      }

      if (this.home != null) {
         tag.putLong("CommandHome", this.home.asLong());
      }
   }

   @Override
   public void load(CompoundTag tag) {
      if (tag.contains("CommandStance")) {
         Stance s = Stance.fromOrdinal(tag.getByte("CommandStance"));
         Modifier m = Modifier.fromOrdinal(tag.getByte("CommandModifier"));
         if (!s.accepts(m)) {
            m = s.defaultModifier();
         }

         this.applyCommand(s, m);

         for (Stance each : Stance.values()) {
            String key = "CommandLast_" + each.name();
            if (tag.contains(key)) {
               Modifier lm = Modifier.fromOrdinal(tag.getByte(key));
               if (each.accepts(lm)) {
                  this.lastModifierPerStance.put(each, lm);
               }
            }
         }

         this.unpoisonStay();
      } else if (tag.contains("Command")) {
         this.migrateLegacy(tag.getInt("Command"));
      }

      this.freedAt = Long.MIN_VALUE;
      this.home = tag.contains("CommandHome") ? BlockPos.of(tag.getLong("CommandHome")) : null;
   }

   public long freeForTicks() {
      return this.stance == Stance.FREE && this.freedAt != Long.MIN_VALUE && this.body != null && this.body.level() != null
         ? Math.max(0L, this.body.level().getGameTime() - this.freedAt)
         : Long.MAX_VALUE;
   }

   @Nullable
   public BlockPos home() {
      return this.stance == Stance.FREE ? this.home : null;
   }

   private void unpoisonStay() {
      if (this.lastModifierPerStance.get(Stance.STAY) == Modifier.GUARD) {
         this.lastModifierPerStance.put(Stance.STAY, Modifier.REST);
      }

      if (this.stance == Stance.STAY && this.modifier == Modifier.GUARD) {
         this.applyCommand(Stance.STAY, Modifier.REST);
      }
   }

   private void migrateLegacy(int legacy) {
      Stance s = switch (legacy) {
         case 0 -> Stance.FREE;
         case 1 -> Stance.STAY;
         case 2 -> Stance.FOLLOW;
         default -> Stance.FREE;
      };
      this.applyCommand(s, s.defaultModifier());
   }

   public Stance getStance() {
      return this.stance;
   }

   public Modifier getModifier() {
      return this.modifier;
   }

   public Modifier getLastModifierFor(Stance s) {
      return this.lastModifierPerStance.getOrDefault(s, s.defaultModifier());
   }

   public boolean isOrderedToSit() {
      return this.stance == Stance.STAY && this.modifier == Modifier.REST;
   }

   public void cycleStance(Player requestor) {
      Stance next = this.stance.next();
      this.attemptSet(next, next.defaultModifier(), null, requestor);
   }

   public void setCommandFromPlayer(Stance newStance, Modifier newModifier, Player requestor) {
      if (!newStance.accepts(newModifier)) {
         newModifier = newStance.defaultModifier();
      }

      if (this.stance != newStance) {
         newModifier = this.getLastModifierFor(newStance);
      }

      this.attemptSet(newStance, newModifier, null, requestor);
   }

   public boolean orderFromPlayer(Stance newStance, Modifier newModifier, @Nullable Vec3 post, @Nullable Player requestor) {
      if (!newStance.accepts(newModifier)) {
         newModifier = newStance.defaultModifier();
      }

      return this.attemptSet(newStance, newModifier, newStance == Stance.STAY ? post : null, requestor);
   }

   public boolean orderFromStaff(Stance newStance, Modifier newModifier, @Nullable Vec3 post, @Nullable Player requestor) {
      if (!newStance.accepts(newModifier)) {
         newModifier = newStance.defaultModifier();
      }

      FaunaMind mind = this.body.faunaMind();
      this.interrupt();
      if (mind != null && mind.threatFear() >= 0.5F) {
         this.feedbackDisobey(requestor);
         return false;
      }

      this.applyCommand(newStance, newModifier);
      if (mind != null) {
         mind.noteObeyed(Order.of(newStance), newStance == Stance.STAY ? post : null);
      }

      this.feedbackObey(requestor);
      return true;
   }

   private boolean attemptSet(Stance newStance, Modifier newModifier, @Nullable Vec3 post, Player requestor) {
      this.interrupt();
      if (this.lockoutStance == newStance && this.disobeyLockoutTicks > 0) {
         this.feedbackDisobey(requestor);
         return false;
      }

      if (!this.willfulnessObeys(newStance)) {
         this.disobeyLockoutTicks = 40;
         this.lockoutStance = newStance;
         this.feedbackDisobey(requestor);
         return false;
      }

      this.applyCommand(newStance, newModifier);
      if (this.body.faunaMind() != null) {
         this.body.faunaMind().noteObeyed(Order.of(newStance), post);
      }

      this.feedbackObey(requestor);
      return true;
   }

   private void interrupt() {
      FaunaMind mind = this.body.faunaMind();
      if (mind != null) {
         mind.knowledge().standDown(this.body);
      }
   }

   private boolean willfulnessObeys(Stance stance) {
      FaunaMind mind = this.body.faunaMind();
      return mind == null || mind.obeys(Order.of(stance));
   }

   public void setCommandForced(Stance newStance, Modifier newModifier) {
      if (!newStance.accepts(newModifier)) {
         newModifier = newStance.defaultModifier();
      }

      this.applyCommand(newStance, newModifier);
   }

   public void setCommandForced(Stance newStance, Modifier newModifier, Vec3 post) {
      this.setCommandForced(newStance, newModifier);
      if (this.body.faunaMind() != null) {
         this.body.faunaMind().knowledge().obedience().ordered(this.stance == Stance.STAY && post != null ? this.body.keepPlace(post) : null);
      }
   }

   private void applyCommand(Stance newStance, Modifier newModifier) {
      this.applyCommand(newStance, newModifier, true);
   }

   private void applyCommand(Stance newStance, Modifier newModifier, boolean remember) {
      boolean changed = this.stance != newStance || this.modifier != newModifier;
      if (newStance == Stance.FREE && this.stance != Stance.FREE && this.body != null && this.body.level() != null && !this.body.level().isClientSide()) {
         this.freedAt = this.body.level().getGameTime();
         BlockPos roost = this.body.getRoostPosition();
         this.home = roost != null ? roost : this.body.blockPosition();
      }

      this.stance = newStance;
      this.modifier = newModifier;
      if (remember) {
         this.lastModifierPerStance.put(newStance, newModifier);
      }

      this.syncCommand();
      if (changed) {
      }
   }

   private void syncCommand() {
      EntityDataAccessor<Integer> stanceData = FaunaBase.getCommandData();
      if ((Integer)this.body.getEntityData().get(stanceData) != this.stance.ordinal()) {
         this.body.getEntityData().set(stanceData, this.stance.ordinal());
      }

      int bits = this.modifier.ordinal() & 7;
      if (this.lastModifierPerStance.getOrDefault(Stance.STAY, Stance.STAY.defaultModifier()) == Stance.STAY.secondaryModifier()) {
         bits |= 8;
      }

      if (this.lastModifierPerStance.getOrDefault(Stance.FOLLOW, Stance.FOLLOW.defaultModifier()) == Stance.FOLLOW.secondaryModifier()) {
         bits |= 16;
      }

      if (this.lastModifierPerStance.getOrDefault(Stance.FREE, Stance.FREE.defaultModifier()) == Stance.FREE.secondaryModifier()) {
         bits |= 32;
      }

      EntityDataAccessor<Integer> modifierData = FaunaBase.getCommandModifierData();
      if ((Integer)this.body.getEntityData().get(modifierData) != bits) {
         this.body.getEntityData().set(modifierData, bits);
      }
   }

   private void feedbackObey(Player requestor) {
      if (requestor != null) {
         Notices.tell(
            requestor,
            Component.translatable(
                  "liberanimalium.command.set",
                  new Object[]{
                     this.body.getDisplayName(),
                     Component.translatable("liberanimalium.command.stance." + this.stance.name().toLowerCase()),
                     Component.translatable("liberanimalium.command.modifier." + this.modifier.name().toLowerCase())
                  }
               )
               .withStyle(ChatFormatting.GREEN)
         );
      }
   }

   private void feedbackDisobey(Player requestor) {
      if (!this.body.level().isClientSide && this.body.level() instanceof ServerLevel srv) {
         Vec3 at = this.body.moodPoint();
         srv.sendParticles(
            ParticleTypes.ANGRY_VILLAGER, at.x, at.y, at.z, 3, FaunaBase.moodSpread(), FaunaBase.moodSpread(), FaunaBase.moodSpread(), 0.02
         );
      }

      if (requestor != null) {
         Notices.tell(requestor, Component.translatable("liberanimalium.command.disobey", new Object[]{this.body.getDisplayName()}).withStyle(ChatFormatting.RED));
      }
   }

   public void wakeFromRest() {
      if (this.stance == Stance.STAY && this.modifier == Modifier.REST) {
         this.applyCommand(Stance.STAY, Modifier.GUARD, false);
      }
   }
}
