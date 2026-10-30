package net.magister.bookofdragons.client.render.dragon;

import net.magister.bookofdragons.entity.custom.dragon.NightfuryEntity;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.render.FaunaRendererAz;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;

public class NightfuryRenderer extends FaunaRendererAz<NightfuryEntity> {
   public NightfuryRenderer(Context context) {
      super(context, DragonType.NIGHTFURY.getSerializedName());
   }
}
