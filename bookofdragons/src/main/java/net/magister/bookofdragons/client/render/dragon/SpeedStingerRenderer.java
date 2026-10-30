package net.magister.bookofdragons.client.render.dragon;

import net.magister.bookofdragons.entity.custom.dragon.SpeedStingerEntity;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.render.FaunaRendererAz;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;

public class SpeedStingerRenderer extends FaunaRendererAz<SpeedStingerEntity> {
   public SpeedStingerRenderer(Context context) {
      super(context, DragonType.SPEED_STINGER.getSerializedName());
   }
}
