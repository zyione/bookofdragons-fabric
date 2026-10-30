package net.magister.bookofdragons.entity.base.dragon;

import java.util.EnumMap;
import java.util.Map;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.fauna.api.Gullet;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.behavior.Perch;
import net.magister.liberanimalium.fauna.behavior.Pilfer;
import net.magister.liberanimalium.fauna.drive.PerchDrive;
import net.magister.liberanimalium.fauna.drive.PilferDrive;
import net.magister.liberanimalium.fauna.know.Beckons;
import net.magister.liberanimalium.fauna.nature.Nature;
import net.magister.liberanimalium.fauna.nature.Natures;
import net.magister.liberanimalium.fauna.nature.PerchHandoff;
import net.magister.liberanimalium.fauna.nature.PilferHandoff;

public final class DragonNatures {
   private static final Map<DragonType, Nature> NATURES = new EnumMap<>(DragonType.class);
   private static final Nature TERROR = Natures.unrideable()
      .extend("terror")
      .pursue(PilferDrive::new, Pilfer::new, PilferHandoff::new, new Class[]{Gullet.class})
      .pursue(PerchDrive::new, Perch::new, PerchHandoff::new, new Class[]{Halter.class})
      .faculty(Beckons::new)
      .build();

   private DragonNatures() {
   }

   public static void compose(DragonType type, Nature nature) {
      NATURES.put(type, nature);
   }

   public static Nature of(DragonType type) {
      Nature nature = NATURES.get(type);
      if (nature == null) {
         throw new IllegalStateException("no nature composed for " + type + " - add its line to DragonNatures");
      } else {
         return nature;
      }
   }

   static {
      NATURES.put(DragonType.NIGHTFURY, Natures.standard());
      NATURES.put(DragonType.DEADLY_NADDER, Natures.standard());
      NATURES.put(DragonType.MONSTROUS_NIGHTMARE, Natures.standard());
      NATURES.put(DragonType.GRONCKLE, Natures.standard());
      NATURES.put(DragonType.HIDEOUS_ZIPPLEBACK, Natures.standard());
      NATURES.put(DragonType.SKRILL, Natures.standard());
      NATURES.put(DragonType.WHISPERING_DEATH, Natures.standard());
      NATURES.put(DragonType.TERRIBLE_TERROR, TERROR);
      NATURES.put(DragonType.SPEED_STINGER, Natures.unrideable());
   }
}
