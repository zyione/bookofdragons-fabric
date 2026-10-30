package net.magister.liberanimalium.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.magister.liberanimalium.render.gpu.FaunaShaders;
import net.magister.liberanimalium.render.gpu.LayeredEntityFormat;
import net.magister.liberanimalium.render.gpu.SpeciesTextureArrays;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.EmptyTextureStateShard;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;

public final class FaunaRenderTypes extends RenderType {
   private static volatile FaunaRenderTypes.Sort sort = FaunaRenderTypes.Sort.UNSORTED;
   private static final Function<ResourceLocation, RenderType> UNSORTED_TRANSLUCENT = Util.memoize(
      texture -> {
         CompositeState state = CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .createCompositeState(true);
         return create("liberanimalium_entity_translucent_unsorted", DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 256, true, false, state);
      }
   );
   private static final Map<RenderType, String> SPECIES_OF = Collections.synchronizedMap(new HashMap<>());
   private static final Map<RenderType, FaunaRenderTypes.Kind> SPECIES_KIND = Collections.synchronizedMap(new HashMap<>());
   private static final Function<String, RenderType> SPECIES_TRANSLUCENT = Util.memoize(
      species -> {
         CompositeState state = CompositeState.builder()
            .setShaderState(new ShaderStateShard(FaunaShaders::membrane))
            .setTextureState(arrayTexture(species))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .createCompositeState(true);
         RenderType type = create("liberanimalium_species_translucent_" + species, LayeredEntityFormat.FORMAT, Mode.QUADS, 256, true, false, state);
         SPECIES_OF.put(type, species);
         SPECIES_KIND.put(type, FaunaRenderTypes.Kind.BODY);
         return type;
      }
   );
   private static final Function<String, RenderType> SPECIES_CUTOUT = Util.memoize(
      species -> {
         CompositeState state = CompositeState.builder()
            .setShaderState(new ShaderStateShard(FaunaShaders::membraneCutout))
            .setTextureState(arrayTexture(species))
            .setTransparencyState(NO_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .createCompositeState(true);
         RenderType type = create("liberanimalium_species_cutout_" + species, LayeredEntityFormat.FORMAT, Mode.QUADS, 256, true, false, state);
         SPECIES_OF.put(type, species);
         SPECIES_KIND.put(type, FaunaRenderTypes.Kind.HURT);
         return type;
      }
   );
   private static final Set<RenderType> BODY_TYPES = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
   private static final Map<RenderType, FaunaRenderTypes.Kind> KINDS = Collections.synchronizedMap(new WeakHashMap<>());

   public static FaunaRenderTypes.Sort sort() {
      return sort;
   }

   public static void setSort(FaunaRenderTypes.Sort value) {
      sort = value == null ? FaunaRenderTypes.Sort.UNSORTED : value;
   }

   private FaunaRenderTypes(
      String name, VertexFormat format, Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState
   ) {
      super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
   }

   public static RenderType translucent(ResourceLocation texture) {
      RenderType type = sort == FaunaRenderTypes.Sort.UNSORTED ? UNSORTED_TRANSLUCENT.apply(texture) : RenderType.entityTranslucent(texture);
      BODY_TYPES.add(type);
      return type;
   }

   private static EmptyTextureStateShard arrayTexture(String species) {
      return new EmptyTextureStateShard(() -> {
         RenderSystem.setShaderTexture(0, 0);
         SpeciesTextureArrays.bind(species);
      }, SpeciesTextureArrays::unbind);
   }

   public static RenderType speciesTranslucent(String species) {
      return SPECIES_TRANSLUCENT.apply(species);
   }

   public static RenderType speciesCutout(String species) {
      return SPECIES_CUTOUT.apply(species);
   }

   public static String speciesOf(RenderType type) {
      return type == null ? null : SPECIES_OF.get(type);
   }

   public static boolean isSpecies(RenderType type) {
      return type != null && SPECIES_OF.containsKey(type);
   }

   public static boolean isBody(RenderType type) {
      return type != null && BODY_TYPES.contains(type);
   }

   public static void noteChosen(RenderType type, FaunaRenderTypes.Kind kind) {
      if (type != null) {
         KINDS.put(type, kind);
      }
   }

   public static FaunaRenderTypes.Kind kindOf(RenderType type) {
      if (type == null) {
         return FaunaRenderTypes.Kind.OTHER;
      }

      FaunaRenderTypes.Kind species = SPECIES_KIND.get(type);
      if (species != null) {
         return species;
      }

      FaunaRenderTypes.Kind kind = KINDS.get(type);
      return kind == null ? FaunaRenderTypes.Kind.OTHER : kind;
   }

   public enum Kind {
      BODY,
      HURT,
      OTHER;
   }

   public enum Sort {
      VANILLA,
      UNSORTED;
   }
}
