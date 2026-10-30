package net.magister.bookofdragons.client.render.dragon;

import net.magister.bookofdragons.entity.custom.dragon.WhisperingDeathEntity;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.render.FaunaRendererAz;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;

public class WhisperingDeathRenderer extends FaunaRendererAz<WhisperingDeathEntity> {
   public WhisperingDeathRenderer(Context context) {
      super(context, DragonType.WHISPERING_DEATH.getSerializedName());
   }
}
