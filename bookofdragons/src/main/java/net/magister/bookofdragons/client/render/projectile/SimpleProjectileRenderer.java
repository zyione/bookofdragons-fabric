package net.magister.bookofdragons.client.render.projectile;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;

public class SimpleProjectileRenderer extends ArrowRenderer<AbstractArrow> {
   public static final ResourceLocation TEXTURE = new ResourceLocation("bookofdragons", "textures/entity/nadder_spike.png");

   public SimpleProjectileRenderer(Context pContext) {
      super(pContext);
   }

   public ResourceLocation getTextureLocation(AbstractArrow pEntity) {
      return TEXTURE;
   }
}
