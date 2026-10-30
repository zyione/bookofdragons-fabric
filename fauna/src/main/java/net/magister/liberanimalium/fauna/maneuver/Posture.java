package net.magister.liberanimalium.fauna.maneuver;

import net.magister.liberanimalium.fauna.identity.Trait;
import net.minecraft.util.Mth;

public record Posture(float radial, float tangential, float covert, float grip) {
   public static final Posture NONE = new Posture(1.0F, 0.0F, 0.0F, 1.0F);

   public static Posture of(Situation situation, Posture.TraitReader traits, float health, float observed) {
      return of(situation, traits, health, observed, 0.0F);
   }

   public static Posture of(Situation situation, Posture.TraitReader traits, float health, float observed, float strain) {
      float aggression = traits.get(Trait.AGGRESSION);
      float bravery = traits.get(Trait.BRAVERY);
      float nervousness = traits.get(Trait.NERVOUSNESS);
      float timidity = 1.0F - bravery;

      return switch (situation) {
         case HUNT -> new Posture(
            1.0F, 0.0F, Mth.clamp(0.5F + 0.15F * timidity + 0.1F * observed, 0.0F, 1.0F), Mth.clamp(0.19F + 0.1F * aggression, 0.0F, 1.0F)
         );
         case FIGHT -> new Posture(
            Mth.clamp(0.45F + 0.5F * aggression - 0.95F * (1.0F - health) - 0.45F * Mth.clamp(strain, 0.0F, 1.0F), 0.0F, 1.0F),
            Mth.clamp(0.35F + 0.5F * nervousness * (1.0F - 0.8F * aggression) - 0.3F * bravery + 0.5F * Mth.clamp(strain, 0.0F, 1.0F), 0.0F, 1.0F),
            Mth.clamp(0.27F * timidity * (1.0F - health), 0.0F, 1.0F),
            Mth.clamp(0.3F + 0.4F * bravery + 0.3F * aggression - 1.0F * (1.0F - health), 0.0F, 1.0F)
         );
         case POSTURE -> new Posture(0.3F, Mth.clamp(0.6F + 0.3F * aggression, 0.0F, 1.0F), 0.0F, Mth.clamp(0.3F + 0.4F * bravery, 0.0F, 1.0F));
         case SPACE -> new Posture(0.25F, Mth.clamp(0.2F + 0.45F * aggression, 0.0F, 1.0F), 0.0F, Mth.clamp(0.3F + 0.4F * bravery, 0.0F, 1.0F));
         case FLEE -> new Posture(0.0F, 0.15F, Mth.clamp(0.4F + 0.5F * (1.0F - health), 0.0F, 1.0F), 0.0F);
         case COURT -> new Posture(0.85F, 0.35F, 0.0F, 0.7F);
         case SOCIAL -> new Posture(0.4F, 0.5F, 0.0F, 0.6F);
         case TRAVEL, IDLE -> NONE;
      };
   }

   @FunctionalInterface
   public interface TraitReader {
      float get(Trait var1);
   }
}
