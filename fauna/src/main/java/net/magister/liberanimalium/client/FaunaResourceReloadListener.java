package net.magister.liberanimalium.client;

import net.magister.liberanimalium.body.BodyPlans;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.magister.liberanimalium.body.anim.binding.AnimationBindingLoader;
import net.magister.liberanimalium.genetics.client.GeneticsTextureCache;
import net.magister.liberanimalium.genetics.client.PaletteRecolorer;
import net.magister.liberanimalium.render.FaunaRendererAz;
import net.magister.liberanimalium.render.ModelCompositor;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class FaunaResourceReloadListener implements ResourceManagerReloadListener {
   public void onResourceManagerReload(ResourceManager resourceManager) {
      ModelCompositor.clearCache();
      PaletteRecolorer.clearCache();
      FaunaRendererAz.clearAllCaches();
      GeneticsTextureCache.INSTANCE.clear();
      AnimationBindingLoader.invalidate();
      BodyPlans.invalidate();
      SpeciesAssets.invalidate();
   }
}
