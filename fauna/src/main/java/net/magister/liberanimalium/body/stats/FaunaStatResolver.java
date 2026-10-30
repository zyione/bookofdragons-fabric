package net.magister.liberanimalium.body.stats;

import java.util.EnumMap;
import java.util.Map;
import net.magister.liberanimalium.genetics.Allele;
import net.magister.liberanimalium.genetics.Genotype;
import net.magister.liberanimalium.genetics.Locus;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig;

public class FaunaStatResolver {
   public static Map<PrimaryAttribute, Integer> resolveAll(Genotype genotype, SpeciesGeneticsConfig config) {
      Map<PrimaryAttribute, Integer> scores = new EnumMap<>(PrimaryAttribute.class);

      for (PrimaryAttribute attr : PrimaryAttribute.values()) {
         Locus locus = getLocusForAttribute(attr);
         int pairIndex = config.getPairIndexOffset(locus);
         Allele defaultAttr = Allele.of(locus.name() + "5", 5);
         Allele a1 = genotype.getAllele1Safe(pairIndex, defaultAttr);
         Allele a2 = genotype.getAllele2Safe(pairIndex, defaultAttr);
         int score = resolveScore(a1, a2);
         scores.put(attr, score);
      }

      return scores;
   }

   public static int resolveScore(Allele a1, Allele a2) {
      int dom1 = a1.getDominance();
      int dom2 = a2.getDominance();
      float avgDom = (dom1 + dom2) / 2.0F;
      float qualityPercent = (10.0F - avgDom) / 9.0F * 100.0F;
      return Math.round(qualityPercent);
   }

   private static Locus getLocusForAttribute(PrimaryAttribute attr) {
      return switch (attr) {
         case CON -> Locus.CON;
         case ATH -> Locus.ATH;
         case AGI -> Locus.AGI;
         case PRW -> Locus.PRW;
         case POT -> Locus.POT;
         case VIG -> Locus.VIG;
      };
   }
}
