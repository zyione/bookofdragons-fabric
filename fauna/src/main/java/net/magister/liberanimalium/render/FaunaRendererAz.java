package net.magister.liberanimalium.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import mod.azure.azurelib.animation.AzAnimator;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.model.cache.AzBakedModelCache;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import mod.azure.azurelib.render.entity.AzEntityRendererPipeline;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig.Builder;
import mod.azure.azurelib.render.layer.AzRenderLayer;
import mod.azure.azurelib.render.lod.AzLodConfig;
import net.magister.liberanimalium.anim.ExpressionEventClient;
import net.magister.liberanimalium.anim.rig.BoneFk;
import net.magister.liberanimalium.anim.rig.BoneMatrices;
import net.magister.liberanimalium.anim.sample.ClipIndex;
import net.magister.liberanimalium.attack.data.AttackDefinition;
import net.magister.liberanimalium.attack.data.MuzzleConfig;
import net.magister.liberanimalium.attack.runtime.MuzzleAnchor;
import net.magister.liberanimalium.body.BabyArt;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.magister.liberanimalium.body.anim.data.PerEntityHeadMatrixExtractor;
import net.magister.liberanimalium.body.anim.extraction.CameraBoneExtractor;
import net.magister.liberanimalium.body.locomotion.ClimbEngine;
import net.magister.liberanimalium.chassis.FaunaAnimator;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.FlightWatch;
import net.magister.liberanimalium.chassis.ShoulderFooting;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.magister.liberanimalium.chassis.component.ranged.OmniAttackHandler;
import net.magister.liberanimalium.genetics.GeneticsSpawner;
import net.magister.liberanimalium.genetics.client.GeneticsTextureCache;
import net.magister.liberanimalium.render.gpu.BonePaletteTexture;
import net.magister.liberanimalium.render.gpu.GpuSkin;
import net.magister.liberanimalium.render.gpu.SkinnedFeedback;
import net.magister.liberanimalium.render.gpu.SkinnedMesh;
import net.magister.liberanimalium.render.gpu.SpeciesBatches;
import net.magister.liberanimalium.render.gpu.SpeciesTextureArrays;
import net.magister.liberanimalium.render.layer.BoneAudience;
import net.magister.liberanimalium.render.layer.BoneNames;
import net.magister.liberanimalium.render.layer.CarriedItemLayer;
import net.magister.liberanimalium.render.layer.MuzzleGlowLayer;
import net.magister.liberanimalium.render.layer.StuckArrowLayer;
import net.magister.liberanimalium.render.layer.WingMembraneLayer;
import net.magister.liberanimalium.render.membrane.MembraneFlex;
import net.magister.liberanimalium.render.nametag.NametagLooks;
import net.magister.liberanimalium.render.nametag.NametagSkinConfig;
import net.magister.liberanimalium.render.nametag.NametagSkinManager;
import net.magister.liberanimalium.render.vfx.FaunaWingTrail;
import net.magister.liberanimalium.render.vfx.FaunaWingTrailManager;
import net.magister.liberanimalium.render.vfx.FaunaWingTrailRenderer;
import net.magister.liberanimalium.render.vfx.LightningBoltRenderer;
import net.magister.liberanimalium.render.vfx.NoseRule;
import net.magister.liberanimalium.ride.render.RiderAnimator;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class FaunaRendererAz<T extends FaunaBase> extends AzEntityRenderer<T> {
   private static final Map<Integer, ResourceLocation> textureCache = new ConcurrentHashMap<>();
   private static final Map<Integer, String> cacheKeyMap = new ConcurrentHashMap<>();
   private static final Map<Integer, Boolean> saddleStateCache = new ConcurrentHashMap<>();
   private static final Map<Integer, String> customNameCache = new ConcurrentHashMap<>();
   private static final Map<Integer, Boolean> youngStateCache = new ConcurrentHashMap<>();
   private static final ThreadLocal<String> NEXT_BINDING = new ThreadLocal<>();
   private static final ThreadLocal<Float> savedPitch = ThreadLocal.withInitial(() -> 0.0F);
   private static final ThreadLocal<Float> savedPitchO = ThreadLocal.withInitial(() -> 0.0F);
   private static final ThreadLocal<Boolean> wasRolling = ThreadLocal.withInitial(() -> false);
   private static final ThreadLocal<Integer> renderDepth = ThreadLocal.withInitial(() -> 0);
   public static final float SHADOW_PER_WIDTH = 0.75F;
   private final float shadowPerWidth;
   private static final ConcurrentHashMap<String, ResourceLocation> MODEL_LOCATIONS = new ConcurrentHashMap<>();
   private static FaunaBase memoEntity;
   private static int memoFrame = Integer.MIN_VALUE;
   private static ResourceLocation memoLocation;
   private static final ConcurrentHashMap<String, ResourceLocation> SADDLE_LOCATIONS = new ConcurrentHashMap<>();
   private static final Map<Integer, int[]> OFFSCREEN_TICK = new ConcurrentHashMap<>();
   private static final Map<Integer, int[]> RENDER_FRAME = new ConcurrentHashMap<>();
   private static final PerfClock.Section CLOCK_FIRST = PerfClock.client("render.first");
   private static final PerfClock.Section CLOCK_REPEAT = PerfClock.client("render.repeat");
   private static boolean repeatLogged;
   private static final PerfClock.Section CLOCK_RENDER = PerfClock.client("render");
   private static final PerfClock.Section CLOCK_PROLOGUE = PerfClock.client("render.prologue");
   private static final PerfClock.Section CLOCK_EPILOGUE = PerfClock.client("render.epilogue");

   private static String bindingFor(String species) {
      String key = NEXT_BINDING.get();
      return key != null ? key : species;
   }

   public FaunaRendererAz(Context context, String species) {
      this(context, species, List.of());
   }

   public FaunaRendererAz(Context context, String species, List<AzRenderLayer<UUID, T>> speciesLayers) {
      this(context, species, speciesLayers, 0.75F);
   }

   public FaunaRendererAz(Context context, String species, List<AzRenderLayer<UUID, T>> speciesLayers, float shadowPerWidth) {
      super(buildConfig(species, speciesLayers, shadowPerWidth), context);
      this.shadowPerWidth = shadowPerWidth;
   }

   public AzEntityRendererPipeline<T> createPipeline(AzEntityRendererConfig<T> config) {
      return new FaunaRendererPipeline<>(config, this);
   }

   private static <T extends FaunaBase> AzEntityRendererConfig<T> buildConfig(String species, List<AzRenderLayer<UUID, T>> speciesLayers, float shadowPerWidth) {
      Builder<T> builder = AzEntityRendererConfig.<T>builder(FaunaRendererAz::getModelLocation, FaunaRendererAz::getTextureLocationCached)
         .setAnimatorProvider(() -> new FaunaAnimator<T>(species, bindingFor(species)))
         .setPrerenderEntry(FaunaRendererAz::preRender)
         .setPostRenderEntry(FaunaRendererAz::postRender)
         .setRenderType(entity -> RenderType.entityTranslucent(getTextureLocationCached(entity)))
         .setShadowRadius(shadowPerWidth)
         .withLodConfig(AzLodConfig.DISABLED)
         .addRenderLayer(new FaunaRendererAz.HeadExtractionLayer())
         .addRenderLayer(new WingMembraneLayer())
         .addRenderLayer(new CarriedItemLayer())
         .addRenderLayer(new MuzzleGlowLayer())
         .addRenderLayer(new StuckArrowLayer());

      for (AzRenderLayer<UUID, T> layer : speciesLayers) {
         builder.addRenderLayer(layer);
      }

      return builder.build();
   }

   private static <T extends FaunaBase> AzRendererPipelineContext<UUID, T> preRender(AzRendererPipelineContext<UUID, T> context) {
      FaunaProceduralAnimator.setCurrentModel(context.bakedModel());
      T body = (T)context.animatable();
      PoseStack poseStack = context.poseStack();
      float partialTick = context.partialTick();
      renderDepth.set(renderDepth.get() + 1);
      if (renderDepth.get() > 1) {
         return context;
      }

      if (context.rendererPipeline() instanceof FaunaRendererPipeline<?> pipeline) {
         FaunaProceduralAnimator.setRenderTransform(poseStack, pipeline.entityRenderTranslations());
      }

      if (body.getVehicle() instanceof Player owner) {
         wasRolling.set(true);
         savedPitch.set(body.getXRot());
         savedPitchO.set(body.xRotO);
         poseStack.last().pose().mul(ShoulderFooting.modelFrame(body, owner, partialTick, ShoulderFooting.renderYaw(body, partialTick)));
         body.setXRot(0.0F);
         body.xRotO = 0.0F;
         return context;
      } else {
         ClimbEngine climb = body.isClimbing() ? body.componentRegistry.get(ClimbEngine.class) : null;
         if (climb != null) {
            wasRolling.set(true);
            savedPitch.set(body.getXRot());
            savedPitchO.set(body.xRotO);
            float vanillaYaw = Mth.rotLerp(partialTick, body.yBodyRotO, body.yBodyRot);
            Matrix4f transform = new Matrix4f();
            transform.translate(0.0F, body.getBbHeight() / 2.0F, 0.0F);
            transform.rotate(climb.attitude(partialTick));
            transform.translate(0.0F, (float)(-climb.standOff(partialTick)), 0.0F);
            transform.rotate(Axis.YP.rotationDegrees(vanillaYaw + 180.0F));
            poseStack.last().pose().mul(transform);
            body.setXRot(0.0F);
            body.xRotO = 0.0F;
            return context;
         }

         if (body.supportsThreeDMedium()) {
            FaunaFlightRotationComponent rotComp = body.componentRegistry.get(FaunaFlightRotationComponent.class);
            if (rotComp != null && body.inThreeDMedium()) {
               FlightWatch.renderBranch(body, (body.isAquatic() ? "SWIM" : "FLIGHT") + (rotComp.isRolling() ? "(3D)" : "(2D)"));
               wasRolling.set(true);
               savedPitch.set(body.getXRot());
               savedPitchO.set(body.xRotO);
               float vanillaYaw = Mth.rotLerp(partialTick, body.yBodyRotO, body.yBodyRot);
               if (!rotComp.isRolling()) {
                  rotComp.flush2DIntent(
                     vanillaYaw,
                     -Mth.lerp(partialTick, body.xRotO, body.getXRot()) * (float) (Math.PI / 180.0),
                     rotComp.getRoll(partialTick) * (float) (Math.PI / 180.0)
                  );
               }

               Quaternionf orientation = rotComp.getRenderOrientation(partialTick);
               if (rotComp.isRolling()) {
                  orientation.mul(new Quaternionf().rotationZ(rotComp.getStrafeLean(partialTick) * (float) (Math.PI / 180.0)));
               }

               float sinkLean = rotComp.getSinkLean(partialTick);
               if (sinkLean > 0.001F) {
                  orientation.mul(new Quaternionf().rotationX(-sinkLean * (float) (Math.PI / 180.0)));
               }

               float halfHeight = body.getBbHeight() / 2.0F;
               Matrix4f transform = new Matrix4f();
               transform.translate(0.0F, halfHeight, 0.0F);
               transform.rotate(orientation);
               transform.translate(0.0F, -halfHeight, 0.0F);
               transform.rotate(Axis.YP.rotationDegrees(vanillaYaw + 180.0F));
               poseStack.last().pose().mul(transform);
               body.setXRot(0.0F);
               body.xRotO = 0.0F;
            } else {
               FlightWatch.renderBranch(body, rotComp == null ? "NO-ROTCOMP" : "NOT-FLYING(client)");
               wasRolling.set(false);
            }
         } else {
            FlightWatch.renderBranch(body, "NO-FLIGHT-SPECIES");
            wasRolling.set(false);
         }

         return context;
      }
   }

   private static <T extends FaunaBase> AzRendererPipelineContext<UUID, T> postRender(AzRendererPipelineContext<UUID, T> context) {
      int depth = renderDepth.get();
      if (depth > 0) {
         renderDepth.set(depth - 1);
      }

      if (depth == 1 && wasRolling.get()) {
         T body = (T)context.animatable();
         body.setXRot(savedPitch.get());
         body.xRotO = savedPitchO.get();
         wasRolling.set(false);
      }

      return context;
   }

   private static <T extends FaunaBase> ResourceLocation getModelLocation(T entity) {
      return MODEL_LOCATIONS.computeIfAbsent(BabyArt.modelKey(entity), FaunaRendererAz::modelLocation);
   }

   private static ResourceLocation modelLocation(String key) {
      return SpeciesAssets.locate("geo/" + key + ".geo.json");
   }

   public static <T extends FaunaBase> ResourceLocation getTextureLocationCached(T entity) {
      int frame = PerfClock.frameId();
      if (entity == memoEntity && frame == memoFrame) {
         return memoLocation;
      }

      ResourceLocation location = getTextureLocationValidated(entity);
      memoEntity = entity;
      memoFrame = frame;
      memoLocation = location;
      return location;
   }

   private static <T extends FaunaBase> ResourceLocation getTextureLocationValidated(T entity) {
      ResourceLocation cached = textureCache.get(entity.getId());
      if (cached != null) {
         if (GeneticsTextureCache.INSTANCE.isStillValid(cached)) {
            return cached;
         }

         releaseOldRef(entity.getId());
         textureCache.remove(entity.getId());
      }

      ResourceLocation texture = getTextureLocation(entity);
      textureCache.put(entity.getId(), texture);
      acquireRef(entity);
      return texture;
   }

   @Nullable
   private static <T extends FaunaBase> NametagSkinConfig getActiveNametagSkin(T entity) {
      String name = entity.variantSkin();
      if (name.isEmpty()) {
         if (!entity.hasCustomName()) {
            return null;
         }

         name = entity.getCustomName().getString();
      }

      String speciesId = entity.speciesId();
      return NametagSkinManager.getConfig(speciesId, name);
   }

   private static <T extends FaunaBase> ResourceLocation getTextureLocation(T entity) {
      ResourceLocation saddleOverlay = entity.isSaddled() ? getSaddleTextureLocation(entity) : null;
      NametagSkinConfig nametagConfig = getActiveNametagSkin(entity);
      if (nametagConfig != null && nametagConfig.composed()) {
         String name = entity.variantSkin().isEmpty() ? entity.getCustomName().getString() : entity.variantSkin();
         GeneticsSpawner.SpawnResult look = NametagLooks.of(entity.speciesId(), name, nametagConfig.genetics());
         if (look != null) {
            return GeneticsTextureCache.INSTANCE
               .getOrCompose(look.genotype, entity.speciesId(), nametagConfig.applySaddle() ? saddleOverlay : null, look.paletteDraws, BabyArt.young(entity));
         }
      } else if (nametagConfig != null) {
         ResourceLocation baseTexture = nametagConfig.texture();
         if (entity.isSaddled() && nametagConfig.applySaddle()) {
            return GeneticsTextureCache.INSTANCE.getOrComposeNametagSkin(baseTexture, saddleOverlay);
         }

         return baseTexture;
      }

      if (entity.hasGenotype()) {
         String speciesId = entity.speciesId();
         return GeneticsTextureCache.INSTANCE.getOrCompose(entity.getGenotype(), speciesId, saddleOverlay, entity.getPaletteDraws(), BabyArt.young(entity));
      } else {
         return BabyArt.plainTexture(entity);
      }
   }

   private static <T extends FaunaBase> ResourceLocation getSaddleModelLocation(T entity) {
      return SADDLE_LOCATIONS.computeIfAbsent(entity.speciesId(), FaunaRendererAz::saddleLocation);
   }

   @Nullable
   public static FaunaRendererAz.SeatStandIn seatStandIn(FaunaBase body, String bone) {
      AzBakedModel saddle = AzBakedModelCache.getInstance().getNullable(getSaddleModelLocation(body));
      AzBone seat = saddle == null ? null : saddle.getBoneOrNull(bone);
      return seat != null && seat.getParent() != null
         ? new FaunaRendererAz.SeatStandIn(seat.getParent().getName(), new Vector3f(seat.getPivotX(), seat.getPivotY(), seat.getPivotZ()))
         : null;
   }

   private static ResourceLocation saddleLocation(String species) {
      return SpeciesAssets.locate("geo/equipment/saddle/" + species + "_lvl1saddle.geo.json");
   }

   private static <T extends FaunaBase> ResourceLocation getSaddleTextureLocation(T entity) {
      String species = entity.speciesId();
      return SpeciesAssets.locate("textures/entity/genetics/" + species + "/saddle_iron.png");
   }

   public static void clearTextureCache(int bodyId) {
      releaseOldRef(bodyId);
      textureCache.remove(bodyId);
   }

   public static void clearAllCaches() {
      for (String key : cacheKeyMap.values()) {
         GeneticsTextureCache.INSTANCE.releaseRef(key);
      }

      textureCache.clear();
      cacheKeyMap.clear();
      saddleStateCache.clear();
      customNameCache.clear();
      youngStateCache.clear();
      FaunaRendererAz.WingTipPositionCache.cache.clear();
      MembraneFlex.clear();
      CubeMesh.invalidate();
      ClipIndex.clear();
      BoneFk.clearChains();
      BoneMatrices.clear();
      SkinnedMesh.clear();
      BonePaletteTexture.release();
      SpeciesTextureArrays.release();
      SpeciesBatches.release();
      SkinnedFeedback.releaseOutputs();
      GpuSkin.clear();
      BoneNames.clear();
      BonePalettes.clear();
      ModelMesh.clear();
      PoseInterpolator.clear();
   }

   public static void onBodyRemoved(FaunaBase body) {
      int id = body.getId();
      releaseOldRef(id);
      textureCache.remove(id);
      saddleStateCache.remove(id);
      customNameCache.remove(id);
      youngStateCache.remove(id);
      FaunaRendererAz.WingTipPositionCache.clear(id);
      MembraneFlex.forget(body.getUUID());
      FaunaProceduralAnimator.forget(id);
      RENDER_FRAME.remove(id);
      OFFSCREEN_TICK.remove(id);
      RigLod.forget(id);
      PoseInterpolator.forget(id);
      BonePalettes.forget(id);
      GpuSkin.forget(id);
      if (memoEntity == body) {
         memoEntity = null;
      }

      ExpressionEventClient.forget(id);
      BoneSpots.forget(id);
      RiderMatrixDataflow.remove(id);
      CameraBoneExtractor.remove(id);
      LightningBoltRenderer.forget(body.getUUID());
   }

   private static void releaseOldRef(int entityId) {
      String oldKey = cacheKeyMap.remove(entityId);
      if (oldKey != null) {
         GeneticsTextureCache.INSTANCE.releaseRef(oldKey);
      }
   }

   private static <T extends FaunaBase> void acquireRef(T entity) {
      String cacheKey = null;
      NametagSkinConfig nametagConfig = getActiveNametagSkin(entity);
      if (nametagConfig != null) {
         if (entity.isSaddled() && nametagConfig.applySaddle()) {
            ResourceLocation saddleOverlay = getSaddleTextureLocation(entity);
            cacheKey = GeneticsTextureCache.INSTANCE.buildNametagCacheKey(nametagConfig.texture(), saddleOverlay);
         }
      } else if (entity.hasGenotype()) {
         String speciesId = entity.speciesId();
         ResourceLocation saddleOverlay = entity.isSaddled() ? getSaddleTextureLocation(entity) : null;
         cacheKey = GeneticsTextureCache.INSTANCE
            .buildCacheKey(entity.getGenotype(), speciesId, saddleOverlay, entity.getPaletteDraws(), BabyArt.young(entity));
      }

      if (cacheKey != null) {
         cacheKeyMap.put(entity.getId(), cacheKey);
         GeneticsTextureCache.INSTANCE.addRef(cacheKey);
      }
   }

   private void handleVisualStateChanges(T body) {
      int id = body.getId();
      Boolean lastSaddled = saddleStateCache.get(id);
      boolean currentSaddled = body.isSaddled();
      boolean saddleChanged = lastSaddled == null || lastSaddled != currentSaddled;
      String lastName = customNameCache.get(id);
      String currentName = body.hasCustomName() ? body.getCustomName().getString() : "";
      if (lastName == null) {
         lastName = "";
      }

      boolean nameChanged = !lastName.equals(currentName);
      Boolean lastYoung = youngStateCache.get(id);
      boolean currentYoung = BabyArt.young(body);
      if (lastYoung == null || lastYoung != currentYoung) {
         youngStateCache.put(id, currentYoung);
         if (lastYoung != null) {
            clearTextureCache(id);
         }

         if (body.getAnimatorOrNull() instanceof FaunaAnimator<?> animator && !animator.bindingKey().equals(body.animBindingKey())) {
            body.setAnimator(null);
         }
      }

      if (saddleChanged || nameChanged) {
         clearTextureCache(id);
         if (saddleChanged) {
            saddleStateCache.put(id, currentSaddled);
         }

         if (nameChanged) {
            customNameCache.put(id, currentName);
         }

         if (saddleChanged) {
            AzAnimator<UUID, FaunaBase> animator = body.getAnimatorOrNull();
            if (animator != null) {
               AzAnimationControllerContainer<FaunaBase> container = animator.getAnimationControllerContainer();
               if (container != null) {
                  for (AzAnimationController<FaunaBase> controller : container.getAll()) {
                     controller.boneAnimationQueueCache().clear();
                  }
               }
            }
         }
      }
   }

   public boolean shouldRender(@NotNull T body, @NotNull Frustum frustum, double camX, double camY, double camZ) {
      if (!body.shouldRender(camX, camY, camZ)) {
         return false;
      }

      Minecraft mc = Minecraft.getInstance();
      if (body.getVehicle() != null && body.getVehicle() == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) {
         return false;
      }

      if (body.noCulling) {
         return true;
      }

      AABB box = body.getBoundingBoxForCulling().inflate(0.5);
      if (!box.hasNaN() && box.getSize() != 0.0 && !frustum.isVisible(box)) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null && body.hasPassenger(player)) {
            return true;
         }

         if (body.grip() != null && body.grip().shownWeight(1.0F) > 0.0F) {
            return true;
         }

         int[] last = OFFSCREEN_TICK.computeIfAbsent(body.getId(), id -> new int[]{Integer.MIN_VALUE});
         if (last[0] == body.tickCount) {
            return false;
         }

         if (!RigLod.due(body.tickCount, 3, body.getId())) {
            return false;
         }

         last[0] = body.tickCount;
         return true;
      } else {
         return true;
      }
   }

   public boolean shouldShowName(@NotNull T body) {
      return super.shouldShowName(body);
   }

   public void render(
      @NotNull T body, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight
   ) {
      long renderClock = CLOCK_RENDER.start();
      countRender(body);
      this.shadowRadius = this.shadowPerWidth * body.getBbWidth();

      try {
         this.renderTimed(body, entityYaw, partialTick, poseStack, bufferSource, packedLight);
      } finally {
         CLOCK_RENDER.end(renderClock);
      }
   }

   private static void countRender(FaunaBase body) {
      int frame = PerfClock.frameId();
      int[] slot = RENDER_FRAME.computeIfAbsent(body.getId(), id -> new int[]{Integer.MIN_VALUE});
      if (slot[0] == frame) {
         CLOCK_REPEAT.end(CLOCK_REPEAT.start());
         if (!repeatLogged) {
            repeatLogged = true;
            LogUtils.getLogger()
               .debug(
                  "[PerfClock] a body is drawn a second time in one frame; the caller:",
                  new Throwable("second render of " + body.getName().getString() + " in frame " + frame)
               );
         }
      } else {
         slot[0] = frame;
         CLOCK_FIRST.end(CLOCK_FIRST.start());
      }
   }

   private void renderTimed(T body, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      long prologue = CLOCK_PROLOGUE.start();
      long epilogue = Long.MIN_VALUE;
      this.handleVisualStateChanges(body);
      NEXT_BINDING.set(body.animBindingKey());
      AzAnimator<UUID, T> animator = this.provider.provideAnimator(null, body);
      if (animator != null) {
         ResourceLocation baseModelId = getModelLocation(body);
         AzBakedModel baseModel = AzBakedModelCache.getInstance().getNullable(baseModelId);
         if (baseModel != null) {
            AzBakedModel targetModel = baseModel;
            if (body.isSaddled()) {
               ResourceLocation saddleModelId = getSaddleModelLocation(body);
               AzBakedModel saddleModel = AzBakedModelCache.getInstance().getNullable(saddleModelId);
               if (saddleModel != null) {
                  targetModel = ModelCompositor.getOrComposite(baseModelId, baseModel, saddleModelId, saddleModel);
               }
            }

            animator.context().boneCache().setActiveModel(targetModel);
         }
      }

      RenderPassContext.beginExtraction(body.getId());
      RenderPassContext.setEntryPose(poseStack.last().pose());
      RiderMatrixDataflow.notifyRendered(body.getId());

      try {
         if (!body.isInvisible()) {
            poseStack.pushPose();
            int spawnTicks = body.getSpawnScaleTicks();
            if (spawnTicks > 0) {
               float duration = 30.0F;
               float progress = 1.0F - (spawnTicks - partialTick) / duration;
               progress = Mth.clamp(progress, 0.0F, 1.0F);
               float scale = 1.0F - (float)Math.pow(1.0F - progress, 3.0);
               poseStack.scale(scale, scale, scale);
            }

            float bodyScale = body.bodyScale();
            if (bodyScale != 1.0F) {
               poseStack.scale(bodyScale, bodyScale, bodyScale);
            }

            CLOCK_PROLOGUE.end(prologue);
            super.render(body, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            epilogue = CLOCK_EPILOGUE.start();
            this.renderWingTrails(body, poseStack, bufferSource);
            poseStack.popPose();
            return;
         }

         CLOCK_PROLOGUE.end(prologue);
      } finally {
         RenderPassContext.endExtraction();
         FaunaProceduralAnimator.clearCurrentModel();
         FaunaProceduralAnimator.clearRenderTransform();
         if (epilogue != Long.MIN_VALUE) {
            CLOCK_EPILOGUE.end(epilogue);
         }
      }
   }

   private void renderWingTrails(T body, PoseStack poseStack, MultiBufferSource bufferSource) {
      Vec3 leftTip = FaunaRendererAz.WingTipPositionCache.get(body.getId(), "left");
      Vec3 rightTip = FaunaRendererAz.WingTipPositionCache.get(body.getId(), "right");
      if (leftTip != null && rightTip != null) {
         if (body.getWingTrailManager() != null && body.animRig().vfx().wingTrails()) {
            body.getWingTrailManager().updateWingTips(leftTip, rightTip);
         }

         FaunaRendererAz.WingTipPositionCache.clear(body.getId());
      }

      FaunaWingTrailManager manager = body.getWingTrailManager();
      if (manager != null) {
         FaunaWingTrail leftTrail = manager.getLeftWingTrail();
         FaunaWingTrail rightTrail = manager.getRightWingTrail();
         if (leftTrail != null) {
            FaunaWingTrailRenderer.render(leftTrail, bufferSource, poseStack.last());
         }

         if (rightTrail != null) {
            FaunaWingTrailRenderer.render(rightTrail, bufferSource, poseStack.last());
         }
      }
   }

   private static class HeadExtractionLayer<T extends FaunaBase> implements AzRenderLayer<UUID, T>, BoneAudience {
      private final ThreadLocal<FaunaRendererAz.HeadExtractionLayer.RiderSeat> riderSeat = new ThreadLocal<>();
      private final ThreadLocal<MuzzleConfig> muzzle = new ThreadLocal<>();
      private final ThreadLocal<Set<String>> spotBones = new ThreadLocal<>();

      public void preRender(AzRendererPipelineContext<UUID, T> context) {
         T body = (T)context.animatable();
         String seatBone = RenderPassContext.isExtractionAllowed(body.getId()) && body.isVehicle()
            ? RiderAnimator.seat(body).attachBone(body.isSaddled())
            : null;
         FaunaRendererAz.SeatStandIn standIn = seatBone != null && context.bakedModel().getBoneOrNull(seatBone) == null
            ? FaunaRendererAz.seatStandIn(body, seatBone)
            : null;
         this.riderSeat
            .set(
               seatBone == null
                  ? null
                  : (
                     standIn != null
                        ? new FaunaRendererAz.HeadExtractionLayer.RiderSeat(standIn.parent(), standIn.pivot())
                        : new FaunaRendererAz.HeadExtractionLayer.RiderSeat(seatBone, null)
                  )
            );
         OmniAttackHandler breath = body.componentRegistry.get(OmniAttackHandler.class);
         AttackDefinition def = breath != null ? breath.definition() : null;
         this.muzzle.set(def != null ? def.muzzle() : MuzzleConfig.DEFAULT);
         Set<String> wanted = body.animEvents().bonesNamed();
         Set<String> nose = NoseRule.of(BabyArt.modelKey(body), context.bakedModel().getBonesByName().keySet(), body.animRig().bones().nose());
         if (!nose.isEmpty()) {
            wanted = new LinkedHashSet<>(wanted);
            wanted.addAll(nose);
         }

         this.spotBones.set(wanted);
      }

      public void render(AzRendererPipelineContext<UUID, T> context) {
      }

      @Override
      public void wantedBones(AzRendererPipelineContext<UUID, ?> context, Set<String> into) {
         AzBakedModel model = context.bakedModel();
         FaunaBase body = (FaunaBase)context.animatable();
         into.addAll(BoneNames.matching(model, CarriedItemLayer.headKey(body), name -> CarriedItemLayer.isHead(body, name)));
         MuzzleConfig wanted = this.muzzle.get();
         if (wanted != null) {
            String bone = wanted.bone();
            into.addAll(BoneNames.matching(model, "muzzle:" + bone, name -> MuzzleAnchor.matches(name, bone)));
         }

         Set<String> spots = this.spotBones.get();
         if (spots != null) {
            into.addAll(spots);
         }

         FaunaRendererAz.HeadExtractionLayer.RiderSeat seat = this.riderSeat.get();
         if (seat != null) {
            into.add(seat.bone());
         }
      }

      public void renderForBone(@NotNull AzRendererPipelineContext<UUID, T> context, @NotNull AzBone bone) {
         String boneName = bone.getName();
         T body = (T)context.animatable();
         if (CarriedItemLayer.isHead(body, boneName)) {
            PerEntityHeadMatrixExtractor.extractAndStore(body, bone, null, context.partialTick());
         }

         MuzzleConfig wanted = this.muzzle.get();
         if (wanted != null && MuzzleAnchor.matches(boneName, wanted.bone())) {
            Vec3 pivot = PerEntityHeadMatrixExtractor.extractBoneWorldPosition(body, bone, context.partialTick());
            Matrix4f muzzleFrame = BonePalettes.worldMatrix(body.getId(), boneName);
            Vec3 mouth = null;
            if (pivot != null && muzzleFrame != null) {
               Vector3f turned = MuzzleAnchor.rotatedOffset(muzzleFrame, new Vector3f(wanted.offsetX(), wanted.offsetY(), wanted.offsetZ()));
               mouth = pivot.add(turned.x(), turned.y(), turned.z());
            }

            if (mouth != null && mouth.distanceToSqr(body.position()) < 1024.0) {
               PerEntityHeadMatrixExtractor.storeMouthPosition(body.getId(), mouth, MuzzleAnchor.isExact(boneName, wanted.bone()), body.level().getGameTime());
            }
         }

         Set<String> spots = this.spotBones.get();
         if (spots != null && !spots.isEmpty() && spots.contains(boneName)) {
            Vec3 at = PerEntityHeadMatrixExtractor.extractBoneWorldPosition(body, bone, context.partialTick());
            if (at != null && at.distanceToSqr(body.position()) < 1024.0) {
               BoneSpots.store(body.getId(), boneName, at, body.level().getGameTime());
            }
         }

         FaunaRendererAz.HeadExtractionLayer.RiderSeat target = this.riderSeat.get();
         if (target != null && boneName.equals(target.bone())) {
            boolean passAllowed = RenderPassContext.isExtractionAllowed(body.getId());
            if (passAllowed) {
               Matrix4f boneViewMatrix = new Matrix4f(context.poseStack().last().pose());
               Vector3f seatPivot = target.pivot();
               if (seatPivot != null) {
                  boneViewMatrix.translate(seatPivot.x() / 16.0F, seatPivot.y() / 16.0F, seatPivot.z() / 16.0F);
               } else {
                  boneViewMatrix.translate(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);
               }

               Vector4f boneViewPos4 = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F).mul(boneViewMatrix);
               double viewSpaceDistance = Math.sqrt(
                  boneViewPos4.x() * boneViewPos4.x() + boneViewPos4.y() * boneViewPos4.y() + boneViewPos4.z() * boneViewPos4.z()
               );
               boolean isMainPass = viewSpaceDistance < 80.0;
               Vec3 bodyPos = new Vec3(body.getX(), body.getY(), body.getZ());
               Vec3 seatOffset = BonePalettes.localOffset(body.getId(), boneName, seatPivot);
               Vec3 boneWorldPos = seatOffset == null ? null : seatOffset.add(bodyPos);
               if (boneWorldPos == null) {
                  return;
               }

               Matrix4f entryInverse = RenderPassContext.entryInverse();
               if (entryInverse != null) {
                  RiderMatrixDataflow.store(body.getId(), new Matrix4f(entryInverse).mul(boneViewMatrix));
               }

               if (isMainPass && RiderMatrixDataflow.tryLockForFrame(body.getId())) {
                  Vec3 offset = BonePalettes.localOffset(body.getId(), boneName, seatPivot);
                  if (offset != null) {
                     CameraBoneExtractor.store(body.getId(), offset.subtract(0.0, AttitudePass.shiftAt(body, context.partialTick()), 0.0));
                  }
               }
            }
         }
      }

      private record RiderSeat(String bone, @Nullable Vector3f pivot) {
      }
   }

   public record SeatStandIn(String parent, Vector3f pivot) {
   }

   public static class WingTipPositionCache {
      private static final Map<Integer, Map<String, Vec3>> cache = new ConcurrentHashMap<>();

      public static void store(int bodyId, String boneName, Vec3 position) {
         cache.computeIfAbsent(bodyId, k -> new ConcurrentHashMap<>()).put(boneName, position);
      }

      @Nullable
      public static Vec3 get(int bodyId, String boneName) {
         Map<String, Vec3> bodyCache = cache.get(bodyId);
         return bodyCache != null ? bodyCache.get(boneName) : null;
      }

      public static void clear(int bodyId) {
         cache.remove(bodyId);
      }
   }
}
