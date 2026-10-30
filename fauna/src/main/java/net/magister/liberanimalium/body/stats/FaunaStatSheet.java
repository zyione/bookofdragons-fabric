package net.magister.liberanimalium.body.stats;

import java.util.EnumMap;
import java.util.Map;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.body.stats.derivation.DerivedStat;
import net.magister.liberanimalium.body.stats.derivation.StatDerivationTable;
import net.magister.liberanimalium.genetics.Locus;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public record FaunaStatSheet(
   float con,
   float ath,
   float agi,
   float prw,
   float pot,
   float vig,
   float health,
   float armor,
   float regen,
   float metabolism,
   float airSpeed,
   float groundSpeed,
   float waterSpeed,
   float acceleration,
   float agility,
   float groundTurn,
   float meleeDamage,
   float knockbackResist,
   float breathPower,
   float breathCost,
   float stamina,
   float staminaRegen,
   float inertia,
   float gravity,
   float stallSpeed,
   float climbBleed,
   float diveGain,
   float autoLevel,
   float bankYaw,
   float diveCap
) {
   public static final FaunaStatSheet EMPTY = new FaunaStatSheet(
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      0.0F,
      1.0F,
      0.08F,
      0.35F,
      0.08F,
      0.08F,
      1.0F,
      1.0F,
      0.0F
   );

   public static Map<PrimaryAttribute, Float> effective(Map<PrimaryAttribute, Integer> scores, SpeciesGeneticsConfig genConfig, float debuffFraction) {
      Map<PrimaryAttribute, Float> eff = new EnumMap<>(PrimaryAttribute.class);

      for (PrimaryAttribute attr : PrimaryAttribute.values()) {
         SpeciesGeneticsConfig.AttributeRange range = genConfig.getAttributeRange(Locus.valueOf(attr.name()));
         float floor = range != null ? range.floor() : 0.0F;
         float ceiling = range != null ? range.ceiling() : 100.0F;
         float value = floor + scores.get(attr).intValue() / 100.0F * (ceiling - floor);
         if (attr != PrimaryAttribute.CON && debuffFraction > 0.0F) {
            value *= 1.0F - debuffFraction;
         }

         eff.put(attr, Math.max(0.0F, Math.min(100.0F, value)));
      }

      return eff;
   }

   public static FaunaStatSheet compute(Map<PrimaryAttribute, Float> eff, StatDerivationTable table, PhysicsSheet.Weight weight, PhysicsSheet.Plan plan) {
      float health = Math.round(table.evaluate(DerivedStat.HEALTH, eff) * weight.health() / 10.0F) * 10.0F;
      return new FaunaStatSheet(
         eff.get(PrimaryAttribute.CON),
         eff.get(PrimaryAttribute.ATH),
         eff.get(PrimaryAttribute.AGI),
         eff.get(PrimaryAttribute.PRW),
         eff.get(PrimaryAttribute.POT),
         eff.get(PrimaryAttribute.VIG),
         health,
         table.evaluate(DerivedStat.ARMOR, eff) * weight.armor(),
         table.evaluate(DerivedStat.REGEN, eff) * health,
         table.evaluate(DerivedStat.METABOLISM, eff),
         table.evaluate(DerivedStat.AIR_SPEED, eff) * weight.speed() * plan.topSpeed(),
         table.evaluate(DerivedStat.GROUND_SPEED, eff) * weight.speed(),
         table.evaluate(DerivedStat.WATER_SPEED, eff) * weight.speed(),
         table.evaluate(DerivedStat.ACCELERATION, eff),
         table.evaluate(DerivedStat.AGILITY, eff) * plan.agility() / weight.inertia(),
         table.evaluate(DerivedStat.GROUND_TURN, eff),
         table.evaluate(DerivedStat.MELEE_DAMAGE, eff),
         table.evaluate(DerivedStat.KNOCKBACK_RESIST, eff),
         table.evaluate(DerivedStat.BREATH_POWER, eff),
         table.evaluate(DerivedStat.BREATH_COST, eff),
         table.evaluate(DerivedStat.STAMINA, eff),
         table.evaluate(DerivedStat.STAMINA_REGEN, eff),
         weight.inertia(),
         weight.gravity(),
         plan.stallSpeed(),
         plan.climbBleed() * weight.gravity(),
         plan.diveGain() * weight.gravity(),
         plan.autoLevel(),
         plan.bankYaw(),
         plan.diveCap()
      );
   }

   public FaunaStatSheet laden(double load, double drag) {
      if (!(load > 0.0)) {
         return this;
      }

      float heavier = (float)(1.0 + load);
      float slower = (float)Math.pow(1.0 + load, -Math.max(0.0, drag));
      return new FaunaStatSheet(
         this.con,
         this.ath,
         this.agi,
         this.prw,
         this.pot,
         this.vig,
         this.health,
         this.armor,
         this.regen,
         this.metabolism,
         this.airSpeed * slower,
         this.groundSpeed,
         this.waterSpeed * slower,
         this.acceleration,
         this.agility / heavier,
         this.groundTurn,
         this.meleeDamage,
         this.knockbackResist,
         this.breathPower,
         this.breathCost,
         this.stamina,
         this.staminaRegen,
         this.inertia * heavier,
         this.gravity * heavier,
         this.stallSpeed * (float)Math.sqrt(heavier),
         this.climbBleed * heavier,
         this.diveGain * heavier,
         this.autoLevel,
         this.bankYaw,
         this.diveCap
      );
   }

   public void applyToEntity(LivingEntity entity) {
      float oldMax = entity.getMaxHealth();
      float share = oldMax > 0.0F ? entity.getHealth() / oldMax : 1.0F;
      set(entity.getAttribute(Attributes.MAX_HEALTH), this.health);
      set(entity.getAttribute(Attributes.ATTACK_DAMAGE), this.meleeDamage);
      set(entity.getAttribute(Attributes.ARMOR), this.armor);
      set(entity.getAttribute(Attributes.MOVEMENT_SPEED), this.groundSpeed);
      set(entity.getAttribute(Attributes.FLYING_SPEED), this.airSpeed);
      set(entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE), this.knockbackResist);
      entity.setHealth(entity.getMaxHealth() * share);
   }

   private static void set(AttributeInstance instance, float value) {
      if (instance != null) {
         instance.setBaseValue(value);
      }
   }

   public String[] describe() {
      return new String[]{
         String.format(
            "§6CON§r %.0f  §6ATH§r %.0f  §6AGI§r %.0f  §6PRW§r %.0f  §6POT§r %.0f  §6VIG§r %.0f", this.con, this.ath, this.agi, this.prw, this.pot, this.vig
         ),
         String.format("§bhealth§r %.0f  §barmor§r %.1f  §bregen§r %.2f  §bmetabolism§r %.2f", this.health, this.armor, this.regen, this.metabolism),
         String.format("§espeed§r air %.3f  ground %.3f  water %.3f  §eaccel§r %.3f", this.airSpeed, this.groundSpeed, this.waterSpeed, this.acceleration),
         String.format("§dagility§r %.3f  §dground turn§r %.2f  §dinertia§r %.2f", this.agility, this.groundTurn, this.inertia),
         String.format(
            "§cmelee§r %.1f  §cknockback resist§r %.2f  §cbreath§r power %.2f cost %.2f",
            this.meleeDamage,
            this.knockbackResist,
            this.breathPower,
            this.breathCost
         ),
         String.format("§astamina§r %.0f  §aregen§r %.2f", this.stamina, this.staminaRegen)
      };
   }
}
