package net.magister.liberanimalium.genetics.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.magister.liberanimalium.genetics.GeneticsNBT;
import net.magister.liberanimalium.genetics.Genotype;
import net.magister.liberanimalium.genetics.PaletteDraws;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfigRegistry;
import net.magister.liberanimalium.render.gpu.SpeciesTextureArrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneticsTextureCache {
   private static final Logger LOGGER = LoggerFactory.getLogger("GeneticsCache");
   public static final GeneticsTextureCache INSTANCE = new GeneticsTextureCache();
   private static final int MAX_CACHE_SIZE = 64;
   private final GeneticsTextureCompositor compositor;
   private final Map<String, GeneticsTextureCache.CachedTexture> cache;
   private final Set<ResourceLocation> validTextures = ConcurrentHashMap.newKeySet();
   private ResourceLocation fallbackTexture;

   private GeneticsTextureCache() {
      this.compositor = new GeneticsTextureCompositor();
      this.cache = new LinkedHashMap<String, GeneticsTextureCache.CachedTexture>(16, 0.75F, true) {
         @Override
         protected boolean removeEldestEntry(Entry<String, GeneticsTextureCache.CachedTexture> eldest) {
            if (this.size() > 64) {
               GeneticsTextureCache.CachedTexture target = eldest.getValue();
               if (target.isInUse()) {
                  GeneticsTextureCache.LOGGER.debug("Skipped eviction (in use, refCount={}): {}", target.getRefCount(), eldest.getKey());
                  return false;
               } else {
                  target.closeAndRelease();
                  GeneticsTextureCache.this.validTextures.remove(target.getResourceLocation());
                  GeneticsTextureCache.LOGGER.debug("Evicted texture: {}", eldest.getKey());
                  return true;
               }
            } else {
               return false;
            }
         }
      };
   }

   public synchronized ResourceLocation getOrCompose(Genotype genotype, String speciesId, @Nullable ResourceLocation saddleOverlay, PaletteDraws paletteDraws) {
      return this.getOrCompose(genotype, speciesId, saddleOverlay, paletteDraws, false);
   }

   public synchronized ResourceLocation getOrCompose(
      Genotype genotype, String speciesId, @Nullable ResourceLocation saddleOverlay, PaletteDraws paletteDraws, boolean young
   ) {
      if (genotype == null) {
         return this.getFallbackTexture();
      } else {
         String cacheKey = this.buildCacheKey(genotype, speciesId, saddleOverlay, paletteDraws, young);
         GeneticsTextureCache.CachedTexture cached = this.cache.get(cacheKey);
         if (cached != null) {
            return cached.getResourceLocation();
         } else {
            SpeciesGeneticsConfig config = SpeciesGeneticsConfigRegistry.getConfig(speciesId);
            NativeImage composited = this.compositor.compose(genotype, config, speciesId, saddleOverlay, paletteDraws, young);
            if (composited == null) {
               LOGGER.warn("Composition failed for {}, using fallback", speciesId);
               return this.getFallbackTexture();
            } else {
               TextureManager textureManager = Minecraft.getInstance().getTextureManager();
               DynamicTexture dynamicTexture = new DynamicTexture(composited);
               ResourceLocation resourceLocation = textureManager.register("genetics/" + cacheKey, dynamicTexture);
               this.cache.put(cacheKey, new GeneticsTextureCache.CachedTexture(dynamicTexture, resourceLocation));
               this.validTextures.add(resourceLocation);
               LOGGER.debug("Composed and cached texture: {} ({} total in cache)", cacheKey, this.cache.size());
               return resourceLocation;
            }
         }
      }
   }

   public synchronized ResourceLocation getOrComposeNametagSkin(ResourceLocation baseTexture, @Nullable ResourceLocation saddleOverlay) {
      if (saddleOverlay == null) {
         return baseTexture;
      } else {
         String cacheKey = "nametag_"
            + baseTexture.toString().replace(":", "_").replace("/", "_")
            + "_saddle_"
            + saddleOverlay.toString().replace(":", "_").replace("/", "_");
         GeneticsTextureCache.CachedTexture cached = this.cache.get(cacheKey);
         if (cached != null) {
            return cached.getResourceLocation();
         } else {
            NativeImage composited = this.compositor.composeSimpleOverlay(baseTexture, saddleOverlay);
            if (composited == null) {
               LOGGER.warn("Nametag composition failed, using base texture: {}", baseTexture);
               return baseTexture;
            } else {
               TextureManager textureManager = Minecraft.getInstance().getTextureManager();
               DynamicTexture dynamicTexture = new DynamicTexture(composited);
               ResourceLocation resourceLocation = textureManager.register("genetics/" + cacheKey, dynamicTexture);
               this.cache.put(cacheKey, new GeneticsTextureCache.CachedTexture(dynamicTexture, resourceLocation));
               this.validTextures.add(resourceLocation);
               LOGGER.debug("Composed and cached nametag texture: {} ({} total in cache)", cacheKey, this.cache.size());
               return resourceLocation;
            }
         }
      }
   }

   public synchronized void addRef(String cacheKey) {
      GeneticsTextureCache.CachedTexture cached = this.cache.get(cacheKey);
      if (cached != null) {
         cached.addRef();
      }
   }

   public synchronized void releaseRef(String cacheKey) {
      GeneticsTextureCache.CachedTexture cached = this.cache.get(cacheKey);
      if (cached != null) {
         cached.release();
      }
   }

   public boolean isStillValid(ResourceLocation resourceLocation) {
      return this.validTextures.contains(resourceLocation);
   }

   public String buildCacheKey(Genotype genotype, String speciesId, @Nullable ResourceLocation saddleOverlay, PaletteDraws paletteDraws) {
      return this.buildCacheKey(genotype, speciesId, saddleOverlay, paletteDraws, false);
   }

   public String buildCacheKey(Genotype genotype, String speciesId, @Nullable ResourceLocation saddleOverlay, PaletteDraws paletteDraws, boolean young) {
      String cacheKey = GeneticsNBT.generateCacheKey(genotype, speciesId, paletteDraws);
      if (young) {
         cacheKey = cacheKey + "_young";
      }

      if (saddleOverlay != null) {
         cacheKey = cacheKey + "_" + saddleOverlay.toString().replace(":", "_").replace("/", "_");
      }

      return cacheKey;
   }

   public String buildNametagCacheKey(ResourceLocation baseTexture, @Nullable ResourceLocation saddleOverlay) {
      return saddleOverlay == null
         ? null
         : "nametag_" + baseTexture.toString().replace(":", "_").replace("/", "_") + "_saddle_" + saddleOverlay.toString().replace(":", "_").replace("/", "_");
   }

   public ResourceLocation getFallbackTexture() {
      if (this.fallbackTexture == null) {
         this.fallbackTexture = new ResourceLocation("liberanimalium", "textures/entity/genetics/fallback.png");
      }

      return this.fallbackTexture;
   }

   public synchronized void evict(String cacheKey) {
      GeneticsTextureCache.CachedTexture removed = this.cache.remove(cacheKey);
      if (removed != null) {
         removed.closeAndRelease();
         this.validTextures.remove(removed.getResourceLocation());
         LOGGER.debug("Force-evicted texture: {}", cacheKey);
      }
   }

   public synchronized void evict(Genotype genotype, String speciesId, PaletteDraws paletteDraws) {
      if (genotype != null) {
         String cacheKey = GeneticsNBT.generateCacheKey(genotype, speciesId, paletteDraws);
         this.evict(cacheKey);
      }
   }

   public synchronized void clear() {
      for (GeneticsTextureCache.CachedTexture cached : this.cache.values()) {
         cached.closeAndRelease();
      }

      this.cache.clear();
      this.validTextures.clear();
      LOGGER.debug("Cleared genetics texture cache");
   }

   public int getCacheSize() {
      return this.cache.size();
   }

   private static class CachedTexture {
      private final DynamicTexture texture;
      private final ResourceLocation resourceLocation;
      private int refCount;

      public CachedTexture(DynamicTexture texture, ResourceLocation resourceLocation) {
         this.texture = texture;
         this.resourceLocation = resourceLocation;
         this.refCount = 0;
      }

      public ResourceLocation getResourceLocation() {
         return this.resourceLocation;
      }

      public void addRef() {
         this.refCount++;
      }

      public int release() {
         if (this.refCount > 0) {
            this.refCount--;
         }

         return this.refCount;
      }

      public int getRefCount() {
         return this.refCount;
      }

      public boolean isInUse() {
         return this.refCount > 0;
      }

      public void closeAndRelease() {
         SpeciesTextureArrays.forget(this.resourceLocation);
         this.texture.close();
         Minecraft.getInstance().getTextureManager().release(this.resourceLocation);
      }
   }
}
