package net.magister.bookofdragons.client.gui.book;

import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.body.stats.PrimaryAttribute;
import net.magister.liberanimalium.genetics.Locus;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfigRegistry;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig.AttributeRange;

public class DragonStatGrader {
   private static final int THRESHOLD_S = 75;
   private static final int THRESHOLD_A = 60;
   private static final int THRESHOLD_B = 45;
   private static final int THRESHOLD_C = 30;

   public static int gradeColor(char grade) {
      return switch (grade) {
         case 'A' -> 2972187;
         case 'B' -> 139;
         case 'C' -> 7032576;
         case 'S' -> 9109504;
         default -> 4868682;
      };
   }

   public static char grade(DragonType type, PrimaryAttribute attr) {
      SpeciesGeneticsConfig config = SpeciesGeneticsConfigRegistry.getConfig(type.getSerializedName());
      AttributeRange range = config.getAttributeRange(Locus.valueOf(attr.name()));
      int floor = range != null ? range.floor() : 0;
      int ceiling = range != null ? range.ceiling() : 100;
      int score = (floor + ceiling) / 2;
      return gradeFromScore(score);
   }

   public static char gradeFromScore(int score) {
      if (score >= 75) {
         return 'S';
      } else if (score >= 60) {
         return 'A';
      } else if (score >= 45) {
         return 'B';
      } else {
         return (char)(score >= 30 ? 'C' : 'D');
      }
   }
}
