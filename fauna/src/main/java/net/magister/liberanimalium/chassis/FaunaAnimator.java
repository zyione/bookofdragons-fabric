package net.magister.liberanimalium.chassis;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import mod.azure.azurelib.animation.AzAnimatorConfig;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerBuilder;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.controller.AzBlendMode;
import mod.azure.azurelib.animation.controller.AzBoneMask;
import mod.azure.azurelib.animation.controller.keyframe.AzKeyframeCallbacks;
import mod.azure.azurelib.animation.easing.AzEasingType;
import mod.azure.azurelib.animation.easing.AzEasingTypeRegistry;
import mod.azure.azurelib.animation.easing.AzEasingTypes;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import mod.azure.azurelib.core.keyframe.event.data.SoundKeyframeData;
import net.magister.liberanimalium.anim.mask.BoneMask;
import net.magister.liberanimalium.anim.rig.Footing;
import net.magister.liberanimalium.anim.v2.LayerSpec;
import net.magister.liberanimalium.body.BabyArt;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.magister.liberanimalium.body.anim.binding.AnimationBindingLoader;
import net.magister.liberanimalium.body.anim.binding.AnimationBindingTable;
import net.magister.liberanimalium.body.anim.data.BoneAccessCache;
import net.magister.liberanimalium.body.anim.data.GaitPhase;
import net.magister.liberanimalium.body.state.FlightAnimationState;
import net.magister.liberanimalium.render.FaunaProceduralAnimator;
import net.magister.liberanimalium.render.PoseInterpolator;
import net.magister.liberanimalium.sound.FaunaSoundHelper;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class FaunaAnimator<T extends FaunaBase> extends AzEntityAnimator<T> {
   private final String species;
   private final String bindingKey;
   public static final double BONE_RESET_TICKS = 10.0;
   private boolean masksClaimed;

   public FaunaAnimator(String species) {
      this(species, species);
   }

   public FaunaAnimator(String species, String bindingKey) {
      super(AzAnimatorConfig.builder().withBoneResetTime(10.0).build());
      this.species = species;
      this.bindingKey = bindingKey;
   }

   public String bindingKey() {
      return this.bindingKey;
   }

   protected void applyMolangQueries(T body, double animTime, float partialTicks) {
      super.applyMolangQueries(body, animTime, partialTicks);
      BoneAccessCache cache = body.getBoneAccessCache();
      Footing foot = body.footing();
      double walked = Math.hypot(foot.x() - foot.groundOld().x, foot.z() - foot.groundOld().z);
      boolean climbs = body.isClimbing();
      GaitPhase.advance(
         cache, body.tickCount, GaitPhase.distance(climbs ? walked : body.getDeltaMovement().horizontalDistance(), climbs ? (float)walked : body.getHorizontalSpeed(), walked)
      );
   }

   protected void preAnimationSetup(T body, double animTime, float partialTicks) {
      super.preAnimationSetup(body, animTime, partialTicks);
      if (body.getStateContext() != null) {
         body.getStateContext().getBindingResolver().frameWeights(body, PoseInterpolator.blended(body) ? 1.0F : partialTicks);
      }
   }

   private AzKeyframeCallbacks<T> voiceKeyframes() {
      return AzKeyframeCallbacks.<T>builder().setSoundKeyframeHandler(event -> {
         String voice = ((SoundKeyframeData)event.getKeyframeData()).getSound();
         if (voice != null && !voice.isEmpty()) {
            FaunaSoundHelper.playLocal((FaunaBase)event.getAnimatable(), voice);
         }
      }).build();
   }

   public void registerControllers(AzAnimationControllerContainer<T> controllers) {
      AnimationBindingTable table = AnimationBindingLoader.get(this.bindingKey);

      for (LayerSpec layer : table.v2.layers()) {
         AzAnimationControllerBuilder<T> builder = AzAnimationController.builder(this, layer.name())
            .setKeyframeCallbacks(this.voiceKeyframes())
            .setTransitionLength(layer.transition());
         AzEasingType easing = easingOf(layer);
         if (easing != null) {
            builder.setEasingType(easing);
         }

         controllers.add(builder.build(), new AzAnimationController[0]);
      }
   }

   public static AzEasingType easingOf(LayerSpec layer) {
      String key = LayerSpec.easingKey(layer.easing());
      if (key == null) {
         return null;
      }

      if (AzEasingTypes.LINEAR == null) {
         return null;
      }

      AzEasingType easing = AzEasingTypeRegistry.getOrNull(key);
      if (easing == null) {
         LogUtils.getLogger().warn("[FaunaAnimator] layer {} names an easing AzureLib does not register: {}", layer.name(), layer.easing());
      }

      return easing;
   }

   private void claimMasks(T body) {
      if (!this.masksClaimed) {
         this.masksClaimed = true;
         AnimationBindingTable table = AnimationBindingLoader.get(this.bindingKey);
         if (!table.v2.layers().isEmpty()) {
            Set<String> bones = this.context().boneCache().getBakedModel().getBonesByName().keySet();

            for (LayerSpec layer : table.v2.layers()) {
               AzAnimationController<T> controller = this.getAnimationControllerContainer().getOrNull(layer.name());
               if (controller != null) {
                  if (layer.mode() == LayerSpec.Mode.ADDITIVE) {
                     controller.setBlendMode(AzBlendMode.ADDITIVE);
                  }

                  BoneMask mask = table.maskFor(layer.name());
                  if (mask.filters()) {
                     List<String> named = new ArrayList<>();
                     boolean allowList = !mask.patterns().isEmpty();

                     for (String bone : bones) {
                        if (mask.allows(bone) == allowList) {
                           named.add(bone);
                        }
                     }

                     controller.setBoneMask(allowList ? AzBoneMask.only(named) : AzBoneMask.except(named));
                  }
               }
            }
         }
      }
   }

   public void setCustomAnimations(T body, float partialTicks) {
      super.setCustomAnimations(body, partialTicks);
      this.claimMasks(body);
      FlightAnimationState animState = FlightAnimationState.compute(body);
      FaunaProceduralAnimator.apply(body, partialTicks, animState);
   }

   @NotNull
   public ResourceLocation getAnimationLocation(T body) {
      String clips = BabyArt.clipKey(body);
      return SpeciesAssets.locate("animations/" + clips + ".animation.json");
   }
}
