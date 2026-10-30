package net.magister.bookofdragons.client.render.dragon;

import java.util.List;
import net.magister.bookofdragons.entity.custom.dragon.MonstrousnightmareEntity;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.render.FaunaRendererAz;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;

public class MonstrousNightmareRenderer extends FaunaRendererAz<MonstrousnightmareEntity> {
   public MonstrousNightmareRenderer(Context context) {
      super(context, DragonType.MONSTROUS_NIGHTMARE.getSerializedName(), List.of(new NightmareImmolationLayer()));
   }
}
