package net.magister.liberanimalium.attack.client.render.builtin;

import net.magister.liberanimalium.attack.client.render.QuadSink;
import net.magister.liberanimalium.attack.client.render.RenderInput;
import net.magister.liberanimalium.attack.client.render.RenderStrategy;
import net.magister.liberanimalium.render.vfx.LightningBoltRenderer;

public class BeamStrategy implements RenderStrategy {
   public static final BeamStrategy INSTANCE = new BeamStrategy();

   @Override
   public void emit(QuadSink sink, RenderInput in) {
      if (in.beamStart() != null && in.beamEnd() != null) {
         sink.poseStack().pushPose();
         LightningBoltRenderer.renderLightningTargeting(
            in.entityUUID(),
            sink.poseStack(),
            sink.multiBufferSource(),
            in.beamStart(),
            in.beamEnd(),
            in.beamEnd(),
            in.beamInnerColor(),
            in.beamOuterColor(),
            in.gameTime()
         );
         sink.poseStack().popPose();
      }
   }
}
