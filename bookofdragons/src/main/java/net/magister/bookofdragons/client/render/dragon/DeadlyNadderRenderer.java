package net.magister.bookofdragons.client.render.dragon;

import net.magister.bookofdragons.entity.custom.dragon.DeadlynadderEntity;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.render.FaunaRendererAz;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;

public class DeadlyNadderRenderer extends FaunaRendererAz<DeadlynadderEntity> {
   public DeadlyNadderRenderer(Context context) {
      super(context, DragonType.DEADLY_NADDER.getSerializedName());
   }
}
