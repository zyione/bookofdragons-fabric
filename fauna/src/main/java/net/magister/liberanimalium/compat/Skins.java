package net.magister.liberanimalium.compat;

import net.magister.liberanimalium.compat.v20.SkinsImpl;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

public final class Skins {
   private Skins() {
   }

   public static ResourceLocation texture(AbstractClientPlayer player) {
      return SkinsImpl.texture(player);
   }
}
