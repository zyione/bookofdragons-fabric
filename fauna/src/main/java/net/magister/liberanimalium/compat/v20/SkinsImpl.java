package net.magister.liberanimalium.compat.v20;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

public final class SkinsImpl {
   private SkinsImpl() {
   }

   public static ResourceLocation texture(AbstractClientPlayer player) {
      return player.getSkinTextureLocation();
   }
}
