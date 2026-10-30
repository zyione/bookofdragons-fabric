package net.magister.liberanimalium.render.gpu;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.platform.client.ClientServices;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public final class FaunaShaders {
   private static final Logger LOGGER = LogUtils.getLogger();
   @Nullable
   private static ShaderInstance skinned;
   @Nullable
   private static ShaderInstance skinnedCutout;
   @Nullable
   private static ShaderInstance membrane;
   @Nullable
   private static ShaderInstance membraneCutout;
   @Nullable
   private static ShaderInstance instanced;
   @Nullable
   private static ShaderInstance instancedCutout;
   @Nullable
   private static ShaderInstance overlay;
   @Nullable
   private static String failure;
   private static int packFrame = Integer.MIN_VALUE;
   private static boolean packNow;
   private static Object irisApi;
   private static Method irisUsing;
   private static boolean irisLookupFailed;

   private FaunaShaders() {
   }

   public static void clientInit() {
      ClientEvents.SHADERS.register(FaunaShaders::register);
   }

   public static void register(ClientServices.ShaderSink event) {
      try {
         event.registerShader(new ResourceLocation("liberanimalium", "fauna_skinned"), SkinnedFormat.FORMAT, loaded -> skinned = loaded);
         event.registerShader(
            new ResourceLocation("liberanimalium", "fauna_skinned_cutout"), SkinnedFormat.FORMAT, loaded -> skinnedCutout = loaded
         );
         event.registerShader(
            new ResourceLocation("liberanimalium", "fauna_membrane"), LayeredEntityFormat.FORMAT, loaded -> membrane = loaded
         );
         event.registerShader(
            new ResourceLocation("liberanimalium", "fauna_membrane_cutout"), LayeredEntityFormat.FORMAT, loaded -> membraneCutout = loaded
         );
         event.registerShader(new ResourceLocation("liberanimalium", "fauna_instanced"), SkinnedFormat.FORMAT, loaded -> instanced = loaded);
         event.registerShader(
            new ResourceLocation("liberanimalium", "fauna_instanced_cutout"), SkinnedFormat.FORMAT, loaded -> instancedCutout = loaded
         );
         event.registerShader(new ResourceLocation("liberanimalium", "fauna_overlay"), SkinnedFormat.FORMAT, loaded -> overlay = loaded);
         SkinnedFeedback.compile(event.getResourceProvider());
         GpuParticles.compile(event.getResourceProvider());
      } catch (Exception e) {
         failure = String.valueOf(e.getMessage());
         LOGGER.error("The skinned shader would not load; bodies draw on the CPU path. {}", failure);
      }
   }

   @Nullable
   public static ShaderInstance skinned() {
      return skinned;
   }

   @Nullable
   public static ShaderInstance skinnedCutout() {
      return skinnedCutout;
   }

   @Nullable
   public static ShaderInstance membrane() {
      return membrane;
   }

   @Nullable
   public static ShaderInstance membraneCutout() {
      return membraneCutout;
   }

   @Nullable
   public static ShaderInstance instanced() {
      return instanced;
   }

   @Nullable
   public static ShaderInstance instancedCutout() {
      return instancedCutout;
   }

   @Nullable
   public static ShaderInstance overlay() {
      return overlay;
   }

   @Nullable
   public static String unavailableBecause() {
      if (failure != null) {
         return "shader did not load: " + failure;
      }

      if (skinned != null && skinnedCutout != null && membrane != null && membraneCutout != null && instanced != null && instancedCutout != null) {
         if (packActive()) {
            String feedback = SkinnedFeedback.unavailableBecause();
            if (feedback != null) {
               return "a shader pack is loaded and the feedback path cannot run: " + feedback;
            }
         }

         return null;
      } else {
         return "shader not registered yet";
      }
   }

   public static boolean packActive() {
      int frame = PerfClock.frameId();
      if (frame != packFrame) {
         packFrame = frame;
         packNow = shaderPackLoaded();
      }

      return packNow;
   }

   public static boolean available() {
      return unavailableBecause() == null;
   }

   private static boolean shaderPackLoaded() {
      if (irisLookupFailed) {
         return false;
      }

      try {
         if (irisApi == null) {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = apiClass.getMethod("getInstance").invoke(null);
            irisUsing = apiClass.getMethod("isShaderPackInUse");
            irisApi = instance;
         }

         return (Boolean)irisUsing.invoke(irisApi);
      } catch (Throwable t) {
         irisLookupFailed = true;
         return false;
      }
   }
}
