package net.magister.bookofdragons.client.render.dragon;

import net.magister.bookofdragons.entity.custom.dragon.SkrillEntity;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.render.FaunaRendererAz;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;

public class SkrillRenderer extends FaunaRendererAz<SkrillEntity> {
   public SkrillRenderer(Context context) {
      super(context, DragonType.SKRILL.getSerializedName());
   }
}
