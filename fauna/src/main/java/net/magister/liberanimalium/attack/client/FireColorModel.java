package net.magister.liberanimalium.attack.client;

import net.magister.liberanimalium.attack.data.color.PaletteLUT;
import net.minecraft.util.Mth;

public class FireColorModel {
   public static int bandIndex(float temperature, int bandCount) {
      int idx = Mth.floor((1.0F - temperature) * bandCount);
      return Mth.clamp(idx, 0, bandCount - 1);
   }

   public static PaletteLUT.ColorBand sample(PaletteLUT palette, float temperature) {
      return palette.bands().get(bandIndex(temperature, palette.bands().size()));
   }
}
