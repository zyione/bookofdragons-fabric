package net.magister.bookofdragons.client.render.misc;

import net.magister.bookofdragons.entity.custom.HooliganDrifterEntity;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.WanderingTrader;

public class HooliganDrifterRenderer extends WanderingTraderRenderer {
   private static final ResourceLocation TEXTURE = new ResourceLocation("bookofdragons", "textures/entity/hooligan_drifter.png");

   public HooliganDrifterRenderer(Context pContext) {
      super(pContext);
   }

   public ResourceLocation getTextureLocation(WanderingTrader pEntity) {
      return pEntity instanceof HooliganDrifterEntity ? TEXTURE : super.getTextureLocation(pEntity);
   }
}
