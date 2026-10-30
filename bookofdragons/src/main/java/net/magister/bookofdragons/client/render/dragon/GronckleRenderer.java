package net.magister.bookofdragons.client.render.dragon;

import net.magister.bookofdragons.entity.custom.dragon.GronckleEntity;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.render.FaunaRendererAz;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;

public class GronckleRenderer extends FaunaRendererAz<GronckleEntity> {
   public GronckleRenderer(Context context) {
      super(context, DragonType.GRONCKLE.getSerializedName());
   }
}
