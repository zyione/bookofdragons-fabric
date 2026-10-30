package net.magister.liberanimalium.attack.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;

public final class AttackRenderTypes extends RenderType {
   private static final Map<ResourceLocation, RenderType> ADDITIVE_EMISSIVE = new HashMap<>();

   private AttackRenderTypes(
      String name, VertexFormat format, Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState
   ) {
      super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
   }

   public static RenderType additiveEmissive(ResourceLocation texture) {
      return ADDITIVE_EMISSIVE.computeIfAbsent(texture, AttackRenderTypes::makeAdditiveEmissive);
   }

   private static RenderType makeAdditiveEmissive(ResourceLocation texture) {
      CompositeState state = CompositeState.builder()
         .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
         .setTextureState(new TextureStateShard(texture, false, false))
         .setTransparencyState(LIGHTNING_TRANSPARENCY)
         .setCullState(NO_CULL)
         .setLightmapState(LIGHTMAP)
         .setOverlayState(OVERLAY)
         .setWriteMaskState(COLOR_WRITE)
         .createCompositeState(false);
      return create("liberanimalium_additive_emissive", DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 256, false, true, state);
   }
}
