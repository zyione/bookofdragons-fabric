package net.magister.bookofdragons.entity.base.dragon;

import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum GrowthStage {
   TINY_TOOTH(0),
   SHORT_WING(1),
   BROAD_WING(2),
   TITAN_WING(3);

   private final int stageId;

   GrowthStage(int stageId) {
      this.stageId = stageId;
   }

   public int getStageId() {
      return this.stageId;
   }

   public Component getDisplayName() {
      return Component.translatable("book.bookofdragons.stage." + this.name().toLowerCase(Locale.ROOT));
   }

   public static GrowthStage fromStageId(int stageId) {
      for (GrowthStage stage : values()) {
         if (stage.stageId == stageId) {
            return stage;
         }
      }

      return TINY_TOOTH;
   }
}
