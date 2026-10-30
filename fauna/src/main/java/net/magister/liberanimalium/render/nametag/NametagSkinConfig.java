package net.magister.liberanimalium.render.nametag;

import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.genetics.GeneticsPreset;
import net.minecraft.resources.ResourceLocation;

public record NametagSkinConfig(@Nullable ResourceLocation texture, boolean applySaddle, GeneticsPreset genetics) {
   public boolean composed() {
      return this.texture == null;
   }
}
