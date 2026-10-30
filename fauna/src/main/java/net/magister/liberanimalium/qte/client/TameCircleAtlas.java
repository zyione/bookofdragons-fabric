package net.magister.liberanimalium.qte.client;

import net.minecraft.resources.ResourceLocation;

public final class TameCircleAtlas {
   public static final ResourceLocation SHEET = new ResourceLocation("liberanimalium", "textures/gui/qte/tame_circle.png");
   public static final int SHEET_W = 256;
   public static final int SHEET_H = 128;
   public static final QteAtlas.Region ARENA = new QteAtlas.Region(0, 0, 84, 84);
   public static final QteAtlas.Region ICON = new QteAtlas.Region(88, 0, 12, 12);
   public static final QteAtlas.Region BAR_FRAME = new QteAtlas.Region(88, 24, 16, 84);
   public static final QteAtlas.Region FILL_SOUL = new QteAtlas.Region(108, 24, 12, 80);
   public static final QteAtlas.Region FILL_TAME = new QteAtlas.Region(124, 24, 12, 80);

   private TameCircleAtlas() {
   }
}
