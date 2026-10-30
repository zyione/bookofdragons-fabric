package net.magister.liberanimalium.attack.data.color;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record PaletteLUT(List<PaletteLUT.ColorBand> bands) {
   public static final Codec<PaletteLUT> CODEC = PaletteLUT.ColorBand.CODEC.listOf().xmap(PaletteLUT::new, PaletteLUT::bands);

   public record ColorBand(List<Integer> color, float alpha) {
      public static final Codec<PaletteLUT.ColorBand> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.INT.listOf().fieldOf("color").forGetter(PaletteLUT.ColorBand::color),
               Codec.FLOAT.optionalFieldOf("alpha", 1.0F).forGetter(PaletteLUT.ColorBand::alpha)
            )
            .apply(i, PaletteLUT.ColorBand::new)
      );
   }
}
