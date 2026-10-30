package net.magister.bookofdragons.client.render.misc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import mod.azure.azurelib.render.lod.AzLodConfig;
import net.magister.bookofdragons.entity.custom.toy.PlushEntity;
import net.magister.bookofdragons.entity.custom.toy.PlushKind;
import net.magister.liberanimalium.anim.rig.RigSpec;
import net.magister.liberanimalium.render.RiggedRendererAz;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

public class PlushRenderer extends RiggedRendererAz<PlushEntity> {
   private static final Logger LOGGER = LogUtils.getLogger();

   public PlushRenderer(Context context, PlushKind kind) {
      super(
         AzEntityRendererConfig.<PlushEntity>builder(plush -> kind.modelFile(), plush -> PlushTints.texture(kind, plush.color()))
            .setPrerenderEntry(PlushRenderer::turn)
            .setRenderType(plush -> RenderType.entityCutoutNoCull(PlushTints.texture(kind, plush.color())))
            .setShadowRadius(kind.width() * 0.45F)
            .withLodConfig(AzLodConfig.DISABLED)
            .build(),
         context
      );
      kind.rig(readRig(context.getResourceManager(), kind.rigFile()));
      PlushTints.clear();
   }

   public boolean shouldShowName(PlushEntity plush) {
      return plush.hasCustomName() && plush == this.entityRenderDispatcher.crosshairPickEntity;
   }

   private static AzRendererPipelineContext<UUID, PlushEntity> turn(AzRendererPipelineContext<UUID, PlushEntity> context) {
      PlushEntity plush = (PlushEntity)context.animatable();
      PoseStack stack = context.poseStack();
      float middle = plush.getBbHeight() / 2.0F;
      stack.translate(0.0F, middle, 0.0F);
      stack.mulPose(plush.orientation(context.partialTick()));
      stack.translate(0.0F, -middle, 0.0F);
      float tall = plush.squish(context.partialTick());
      float wide = 1.0F / (float)Math.sqrt(tall);
      stack.scale(wide, tall, wide);
      return context;
   }

   private static RigSpec readRig(ResourceManager resources, ResourceLocation file) {
      Optional<Resource> found = resources.getResource(file);
      if (found.isEmpty()) {
         return RigSpec.EMPTY;
      }

      try (Reader reader = found.get().openAsReader()) {
         JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
         Map<String, List<String>> chains = new LinkedHashMap<>();
         if (json.has("chains")) {
            for (Entry<String, JsonElement> chain : json.getAsJsonObject("chains").entrySet()) {
               List<String> bones = new ArrayList<>();

               for (JsonElement bone : chain.getValue().getAsJsonArray()) {
                  bones.add(bone.getAsString());
               }

               chains.put(chain.getKey(), bones);
            }
         }

         return RigSpec.parse(json).withBody(List.of(""), chains);
      } catch (Exception malformed) {
         LOGGER.warn("{} is not a rig block: {}", file, malformed.toString());
         return RigSpec.EMPTY;
      }
   }
}
