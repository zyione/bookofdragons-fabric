package net.magister.bookofdragons.advancement;

import net.minecraft.advancements.CriteriaTriggers;

public class ModAdvancementTriggers {
   public static DragonTrigger DRAGON;

   public static void register() {
      DRAGON = (DragonTrigger)CriteriaTriggers.register(new DragonTrigger());
      DragonMilestones.init();
   }
}
