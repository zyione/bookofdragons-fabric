package net.magister.bookofdragons.client.render.dragon;

import net.magister.bookofdragons.entity.custom.dragon.HiddeousZipplebackEntity;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.render.FaunaRendererAz;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;

public class HiddeousZipplebackRenderer extends FaunaRendererAz<HiddeousZipplebackEntity> {
   public HiddeousZipplebackRenderer(Context context) {
      super(context, DragonType.HIDEOUS_ZIPPLEBACK.getSerializedName());
   }
}
