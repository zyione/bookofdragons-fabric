package net.magister.liberanimalium.fauna.know;

import net.magister.liberanimalium.fauna.api.Relation;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.minecraft.util.Mth;

public final class Bend {
   private Bend() {
   }

   public static boolean retaliates(float bravery, float aggression, boolean cornered) {
      float mettle = 0.5F * (Mth.clamp(bravery, 0.0F, 1.0F) + Mth.clamp(aggression, 0.0F, 1.0F));
      return mettle >= (cornered ? 0.4F : 0.5F);
   }

   public static Relation afterHurt(Relation current, float severity, float bravery, float aggression, boolean cornered, boolean sameSpecies) {
      float carried = isPressable(current.type()) ? current.intensity() : 0.0F;
      float intensity = Mth.clamp(carried + Mth.clamp(severity, 0.0F, 1.0F), 0.0F, 1.0F);
      return isFighting(current.type())
         ? new Relation(current.type(), intensity)
         : new Relation(answersWith(bravery, aggression, cornered, sameSpecies), intensity);
   }

   public static RelationType answersWith(float bravery, float aggression, boolean cornered, boolean ritual) {
      if (!retaliates(bravery, aggression, cornered)) {
         return RelationType.AFRAID;
      } else {
         return ritual ? RelationType.RIVAL : RelationType.ATTACKS;
      }
   }

   public static boolean isFighting(RelationType type) {
      return type == RelationType.RIVAL || type == RelationType.ATTACKS;
   }

   public static Relation pressed(
      Relation current, Relation reference, float pressure, float warmth, float bravery, float aggression, boolean cornered, boolean ritual, long elapsed
   ) {
      float target = Mth.clamp(pressure, 0.0F, 1.0F) * (1.0F - Mth.clamp(warmth, 0.0F, 1.0F));
      if (target <= current.intensity() || elapsed <= 0L) {
         return current;
      }

      if (!isPressable(current.type())) {
         return current;
      }

      double closed = 1.0 - Math.pow(1.0 - MannerTuning.PRESS_RISE_PER_TICK, elapsed);
      float intensity = Mth.clamp(current.intensity() + (float)(closed * (target - current.intensity())), 0.0F, 1.0F);
      if (intensity <= current.intensity()) {
         return current;
      }

      RelationType type = isFighting(current.type()) ? current.type() : answersWith(bravery, aggression, cornered, ritual);
      return new Relation(type, Math.max(floorOf(reference, type), intensity));
   }

   public static boolean tolerates(Relation held, float like) {
      return isNegative(held.type()) && like >= packLine(held.intensity());
   }

   public static float packLine(Relation held) {
      return packLine(held, 0.0F);
   }

   public static float packLine(Relation held, float aloofness) {
      float plain = packLine(held.intensity());
      return !isNegative(held.type()) ? plain : Math.min(plain + held.intensity() * 0.5F, 0.8F) + held.intensity() * Mth.clamp(aloofness, 0.0F, 1.0F);
   }

   private static float ignoreLine(Relation held, float aloofness) {
      return Math.min((packLine(held.intensity()) + packLine(held, aloofness)) / 2.0F, 0.8F);
   }

   public static Relation projection(Relation underlying, Relation live, float like) {
      return projection(underlying, live, like, 0.0F);
   }

   public static Relation projection(Relation underlying, Relation live, float like, float aloofness) {
      if (!isNegative(underlying.type())) {
         if (like >= packLine(underlying)) {
            return new Relation(RelationType.PACK, Mth.clamp(like, 0.0F, 1.0F));
         } else if ((live.type() == RelationType.PACK || live.type() == RelationType.IGNORES) && underlying.type() != live.type()) {
            return like >= packLine(underlying) - 0.1F ? live : underlying;
         } else {
            return live;
         }
      } else {
         int held = band(live.type());
         int reached = like >= packLine(underlying, aloofness)
            ? 3
            : (like >= ignoreLine(underlying, aloofness) ? 2 : (like >= packLine(underlying.intensity()) ? 1 : 0));
         int band = held > reached && like >= bandLine(underlying, held, aloofness) - 0.1F ? held : reached;

         return switch (band) {
            case 1 -> new Relation(RelationType.UNCOMFORTABLE, underlying.intensity());
            case 2 -> new Relation(RelationType.IGNORES, 0.0F);
            case 3 -> new Relation(RelationType.PACK, Mth.clamp(like, 0.0F, 1.0F));
            default -> held == 0 ? live : underlying;
         };
      }
   }

   private static int band(RelationType type) {
      return switch (type) {
         case UNCOMFORTABLE -> 1;
         case IGNORES -> 2;
         case PACK -> 3;
         default -> 0;
      };
   }

   private static float bandLine(Relation underlying, int band, float aloofness) {
      return switch (band) {
         case 2 -> ignoreLine(underlying, aloofness);
         case 3 -> packLine(underlying, aloofness);
         default -> packLine(underlying.intensity());
      };
   }

   public static Relation unanswered(Relation species, boolean answers, boolean pressing, float share) {
      if (!answers && !pressing && !(share >= 1.0F)) {
         return switch (species.type()) {
            case UNCOMFORTABLE, AFRAID, ATTACKS, RIVAL -> new Relation(species.type(), species.intensity() * Math.max(0.0F, share));
            default -> species;
         };
      } else {
         return species;
      }
   }

   public static Relation toyedWith(Relation underlying, Relation live, Wants wants, float hunger) {
      if (underlying.type() != RelationType.EATS) {
         return live;
      } else {
         boolean toy = live.type() == RelationType.PLAYS_WITH;
         if (live.type() != RelationType.EATS && !toy) {
            return live;
         } else if (wants.rough() && wants.playWant() > hunger) {
            return toy ? live : new Relation(RelationType.PLAYS_WITH, underlying.intensity());
         } else {
            return toy ? underlying : live;
         }
      }
   }

   public static float packLine(float heldIntensity) {
      return Mth.clamp(heldIntensity, 0.2F, 0.5F);
   }

   public static boolean isPressable(RelationType type) {
      return isNegative(type) || type == RelationType.EATS || type == RelationType.IGNORES || type == RelationType.UNCOMFORTABLE;
   }

   public static Relation habituate(Relation current, Relation reference, float nearness, float rungRelief) {
      if (!isNegative(current.type())) {
         return current;
      }

      float floor = floorOf(reference, current.type()) * (1.0F - Mth.clamp(rungRelief, 0.0F, 1.0F));
      float wear = 8.0E-4F * Mth.clamp(nearness, 0.0F, 1.0F);
      float intensity = Math.max(floor, current.intensity() - wear);
      return intensity == current.intensity() ? current : new Relation(current.type(), intensity);
   }

   public static Relation kindness(Relation current, float share) {
      if (!isPressable(current.type())) {
         return current;
      }

      float relief = 0.02F * Math.max(0.0F, share);
      float intensity = Math.max(0.0F, current.intensity() - relief);
      return intensity == current.intensity() ? current : new Relation(current.type(), intensity);
   }

   public static Relation afterLostFight(Relation current, float fear) {
      float intensity = Mth.clamp(current.intensity() + Mth.clamp(fear, 0.0F, 1.0F), 0.0F, 1.0F);
      return new Relation(RelationType.AFRAID, intensity);
   }

   public static Relation defendingFriend(Relation current, float severity, boolean sameSpecies) {
      RelationType type = sameSpecies ? RelationType.RIVAL : RelationType.ATTACKS;
      return new Relation(type, Mth.clamp(Math.max(current.type() == type ? current.intensity() : 0.0F, 0.4F + severity), 0.0F, 1.0F));
   }

   public static Relation urgedOn(Relation current, float word, boolean sameSpecies) {
      float worth = Mth.clamp(word, 0.0F, 1.0F);
      if (worth <= 0.0F) {
         return current;
      }

      RelationType type = sameSpecies ? RelationType.RIVAL : RelationType.ATTACKS;
      return new Relation(type, Math.max(current.type() == type ? current.intensity() : 0.0F, worth));
   }

   public static Relation calledOff(Relation current, float word) {
      if (current.type() != RelationType.ATTACKS && current.type() != RelationType.RIVAL) {
         return current;
      }

      float intensity = Math.max(0.0F, current.intensity() - Mth.clamp(word, 0.0F, 1.0F));
      return intensity == current.intensity() ? current : new Relation(current.type(), intensity);
   }

   public static float floorOf(Relation reference, RelationType currentType) {
      return reference.type() != currentType ? 0.0F : reference.intensity() * 0.5F;
   }

   public static Relation peaceful(Relation live) {
      return !isFighting(live.type()) && live.type() != RelationType.EATS ? live : new Relation(RelationType.AFRAID, live.intensity());
   }

   public static boolean isNegative(RelationType type) {
      return type == RelationType.AFRAID || isFighting(type);
   }
}
