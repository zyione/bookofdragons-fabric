package net.magister.bookofdragons.client.render.dragon;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.model.cache.AzBakedModelCache;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.layer.AzRenderLayer;
import net.magister.bookofdragons.entity.custom.dragon.MonstrousnightmareEntity;
import net.magister.bookofdragons.particle.ModParticles;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.compat.AzColor;
import net.magister.liberanimalium.render.WornModels;
import net.magister.liberanimalium.render.gpu.GpuSkin;
import net.magister.liberanimalium.render.layer.BoneAudience;
import net.magister.liberanimalium.render.layer.WornMembranes;
import net.magister.liberanimalium.render.membrane.MembranePanel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class NightmareImmolationLayer<T extends FaunaBase> implements AzRenderLayer<UUID, T>, BoneAudience {
   private static final ResourceLocation COAT_MODEL = new ResourceLocation("bookofdragons", "geo/monstrous_nightmare_coat.geo.json");
   private static final ResourceLocation COAT_TEXTURE = new ResourceLocation("bookofdragons", "textures/entity/dragon/vfx/nightmare_coat.png");
   private static final int FIRE_LIGHT = 15728880;
   private static final float UPWARD = 0.45F;
   private static final float MIN_FACE_PIXELS = 6.0F;
   private static final float FLAME_ODDS = 0.12F;
   private static final float MEMBRANE_LIFT = 0.0625F;
   private static final float FLAMES_PER_PANEL = 0.6F;
   private static final Predicate<MembranePanel> WINGS = panel -> panel.corner()[0].toLowerCase(Locale.ROOT).contains("wing");
   private static final Map<Integer, Integer> LAST_TICK = new HashMap<>();

   public void preRender(AzRendererPipelineContext<UUID, T> context) {
   }

   public void render(AzRendererPipelineContext<UUID, T> context) {
      T entity = (T)context.animatable();
      if (!(entity instanceof MonstrousnightmareEntity nightmare && nightmare.isSelfImmolating())) {
         LAST_TICK.remove(entity.getId());
      } else if (!entity.isBaby()) {
         AzBakedModel coat = AzBakedModelCache.getInstance().getNullable(COAT_MODEL);
         if (coat != null) {
            RenderType type = RenderType.entityTranslucentEmissive(COAT_TEXTURE);
            float r = AzColor.r(context);
            float g = AzColor.g(context);
            float b = AzColor.b(context);
            float a = AzColor.a(context);
            if (!GpuSkin.drawWorn(entity.getId(), coat, type, 15728880, context.packedOverlay(), r, g, b, a)) {
               WornModels.drawCpu(
                  entity.getId(),
                  coat,
                  context.poseStack().last(),
                  context.multiBufferSource().getBuffer(type),
                  15728880,
                  context.packedOverlay(),
                  r,
                  g,
                  b,
                  a
               );
            }

            WornMembranes.draw(context, WINGS, 0.0625F, context.multiBufferSource().getBuffer(type), 15728880, context.packedOverlay());
            letUpFlames(entity, context.bakedModel(), coat, context.partialTick());
         }
      }
   }

   private static void letUpFlames(FaunaBase entity, AzBakedModel body, AzBakedModel coat, float partialTick) {
      Integer last = LAST_TICK.put(entity.getId(), entity.tickCount);
      int ticks = last == null ? 1 : Math.min(3, entity.tickCount - last);
      Vec3 drawn = entity.getPosition(partialTick);
      RandomSource random = entity.level().getRandom();
      Consumer<Vec3> flame = at -> entity.level()
         .addParticle((ParticleOptions)ModParticles.NIGHTMARE_JACKET_FLAME.get(), at.x, at.y, at.z, 0.0, 0.0, 0.0);

      for (int t = 0; t < ticks; t++) {
         WornModels.upwardPoints(entity.getId(), body, coat, 0.45F, 6.0F, 0.12F, drawn, random, flame);
         WornMembranes.upwardPoints(entity, partialTick, WINGS, 0.0625F, 0.45F, 0.6F, drawn, random, flame);
      }
   }

   public void renderForBone(AzRendererPipelineContext<UUID, T> context, AzBone bone) {
   }

   public void wantedBones(AzRendererPipelineContext<UUID, ?> context, Set<String> into) {
   }
}
