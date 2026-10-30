package net.magister.liberanimalium.ride.render;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.magister.liberanimalium.body.anim.binding.AnimSignals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RiderPoseLoader extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LoggerFactory.getLogger("RiderPoseLoader");

   public RiderPoseLoader() {
      super(new Gson(), "rider_poses");
   }

   protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {
      Map<String, RiderPoseProfile> loaded = new HashMap<>();

      for (Entry<ResourceLocation, JsonElement> e : objects.entrySet()) {
         String species = e.getKey().getPath();

         try {
            RiderPoseProfile profile = RiderPoseProfile.parse(e.getValue().getAsJsonObject(), name -> AnimSignals.scalarIndex(name) >= 0);

            for (String line : profile.report()) {
               LOGGER.warn("[RiderPose] {}: {}", species, line);
            }

            loaded.put(species, profile);
         } catch (RuntimeException ex) {
            LOGGER.error("[RiderPose] {}: not a rider pose file ({})", species, ex.toString());
         }
      }

      RiderPoses.set(loaded);
      LOGGER.debug("[RiderPose] Loaded {} rider profile(s): {}", loaded.size(), loaded.keySet());
   }
}
