package net.magister.liberanimalium.render.waves.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;

final class WaveRenderTypes extends RenderType {
   private WaveRenderTypes(
      String name, VertexFormat format, Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState
   ) {
      super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
   }

   static RenderType surface(ResourceLocation texture) {
      CompositeState state = CompositeState.builder()
         .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
         .setTextureState(new TextureStateShard(texture, false, false))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(CULL)
         .setLightmapState(LIGHTMAP)
         .setOverlayState(OVERLAY)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false);
      return create("liberanimalium_water_surface", DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 1536, false, false, state);
   }
}
