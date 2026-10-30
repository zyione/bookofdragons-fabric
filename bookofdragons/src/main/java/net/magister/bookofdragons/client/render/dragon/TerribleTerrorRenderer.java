package net.magister.bookofdragons.client.render.dragon;

import net.magister.bookofdragons.entity.custom.dragon.TerribleTerrorEntity;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.render.FaunaRendererAz;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;

public class TerribleTerrorRenderer extends FaunaRendererAz<TerribleTerrorEntity> {
   public TerribleTerrorRenderer(Context context) {
      super(context, DragonType.TERRIBLE_TERROR.getSerializedName());
   }
}
