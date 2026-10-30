package net.magister.liberanimalium.chassis.component;

import com.mojang.logging.LogUtils;
import net.magister.liberanimalium.body.data.FaunaWeightClass;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayProfile;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.body.stats.FaunaStatResolver;
import net.magister.liberanimalium.body.stats.FaunaStatSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.ranged.OmniAttackHandler;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfigRegistry;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;

public class StatsComponent implements IFaunaComponent {
   private static final Logger LOGGER = LogUtils.getLogger();
   protected FaunaBase body;
   private FaunaStatSheet statSheet = FaunaStatSheet.EMPTY;
   private boolean initialSpawnLogged = false;
   private boolean logPending = false;

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   @Override
   public void serverTick() {
      if (!this.body.level().isClientSide() && this.logPending && this.body.tickCount >= 10) {
         this.logStats("Spawn Summary");
         this.logPending = false;
         this.initialSpawnLogged = true;
      }
   }

   @Override
   public void save(CompoundTag tag) {
   }

   @Override
   public void load(CompoundTag tag) {
   }

   public FaunaStatSheet getStatSheet() {
      return this.statSheet;
   }

   public void recalculateStats() {
      if (this.body.hasGenotype() && PhysicsSheet.loaded()) {
         String speciesId = this.body.speciesId();
         SpeciesGeneticsConfig geneticsConfig = SpeciesGeneticsConfigRegistry.getConfig(speciesId);
         SpeciesGameplayProfile gameplayProfile = this.body.gameplay();
         if (geneticsConfig != null) {
            PhysicsSheet sheet = PhysicsSheet.of(this.body);
            FaunaWeightClass weightClass = gameplayProfile != null && gameplayProfile.weightClass() != null
               ? gameplayProfile.weightClass()
               : this.body.getWeightClass();
            PhysicsSheet.Plan plan = gameplayProfile != null && gameplayProfile.plan() != null ? sheet.plan(gameplayProfile.plan()) : PhysicsSheet.Plan.GROUND;
            GripComponent grip = this.body.grip();
            this.statSheet = FaunaStatSheet.compute(
                  FaunaStatSheet.effective(
                     FaunaStatResolver.resolveAll(this.body.getGenotype(), geneticsConfig), geneticsConfig, this.body.getSyncedDebuffFraction()
                  ),
                  sheet.stats(),
                  sheet.weight(weightClass),
                  plan
               )
               .laden(grip == null ? 0.0 : grip.load(), sheet.weight(weightClass).loadDrag());
            this.applyStats();
            if (this.body.getStaminaSystem() != null) {
               this.body.getStaminaSystem().setMaxStamina(this.statSheet.stamina());
               this.body.getStaminaSystem().setRegenRate(this.statSheet.staminaRegen());
            }

            if (gameplayProfile != null && gameplayProfile.attackId() != null) {
               OmniAttackHandler breathHandler = this.body.componentRegistry.get(OmniAttackHandler.class);
               if (breathHandler != null) {
                  breathHandler.setAttackId(gameplayProfile.attackId());
               }
            }

            if (!this.body.level().isClientSide() && this.body.tickCount < 20 && !this.initialSpawnLogged) {
               this.logPending = true;
            }
         }
      }
   }

   public void applyStats() {
      this.statSheet.applyToEntity(this.body);
   }

   public String[] describe() {
      SpeciesGameplayProfile profile = this.body.gameplay();
      String bodyLine = profile == null
         ? "no gameplay profile"
         : (profile.weightClass() == null ? "host weight" : profile.weightClass().getDisplayName())
            + " "
            + (profile.plan() == null ? "ground" : profile.plan().name().toLowerCase());
      String[] sheet = this.statSheet.describe();
      String[] lines = new String[sheet.length + 1];
      lines[0] = "§6[Inspector]§r " + this.body.getDisplayName().getString() + " - " + bodyLine;
      System.arraycopy(sheet, 0, lines, 1, sheet.length);
      return lines;
   }

   public void logStats(String reason) {
      if (!this.body.level().isClientSide()) {
         LOGGER.debug("[StatsComponent] [{}] {} ({})", new Object[]{reason, this.body.getDisplayName().getString(), this.body.getUUID()});
         if (this.body.hasGenotype()) {
            LOGGER.debug("[StatsComponent] Genetic: {}", this.body.getGenotype().toCompactString());
         }

         for (String line : this.describe()) {
            LOGGER.debug("{}", line);
         }
      }
   }
}
