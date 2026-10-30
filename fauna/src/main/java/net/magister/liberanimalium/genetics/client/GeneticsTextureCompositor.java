package net.magister.liberanimalium.genetics.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.magister.liberanimalium.body.BabyArt;
import net.magister.liberanimalium.genetics.GeneticsLayer;
import net.magister.liberanimalium.genetics.Genotype;
import net.magister.liberanimalium.genetics.Locus;
import net.magister.liberanimalium.genetics.PaletteDraws;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneticsTextureCompositor {
   private static final Logger LOGGER = LoggerFactory.getLogger("GeneticsCompositor");
   private final PaletteLayerResolver layerResolver;

   public GeneticsTextureCompositor() {
      this.layerResolver = new PaletteLayerResolver();
   }

   public GeneticsTextureCompositor(PaletteLayerResolver layerResolver) {
      this.layerResolver = layerResolver;
   }

   public NativeImage compose(
      Genotype genotype, SpeciesGeneticsConfig config, String speciesId, @Nullable ResourceLocation saddleOverlay, PaletteDraws paletteDraws
   ) {
      return this.compose(genotype, config, speciesId, saddleOverlay, paletteDraws, false);
   }

   public NativeImage compose(
      Genotype genotype, SpeciesGeneticsConfig config, String speciesId, @Nullable ResourceLocation saddleOverlay, PaletteDraws paletteDraws, boolean young
   ) {
      NativeImage result = null;
      int layersComposited = 0;
      Map<Locus, Integer> consumedColorUnits = new HashMap<>();

      for (GeneticsLayer layer : GeneticsLayer.getLayersInRenderOrder()) {
         if (!this.layerResolver.shouldRenderLayer(layer, config)) {
            LOGGER.debug("Skipping layer: {}", layer.name());
         } else {
            NativeImage layerImage = null;
            Locus structLocus = layer.getStructureLocus();
            int structCount = structLocus != null ? config.getExpressionCount(structLocus) : 1;
            int reservedChannels = structLocus != null ? config.getMultiChannelCount(structLocus) : 1;

            for (int sIdx = 0; sIdx < structCount; sIdx++) {
               if (structLocus != null) {
                  config.getPairIndexOffset(structLocus);
               }

               String debugInfo;
               if (layer.usesNativeTexture()) {
                  ResourceLocation nativeTexturePath = this.layerResolver.getNativeTexture(layer, genotype, config, speciesId, paletteDraws);
                  if (young && nativeTexturePath != null) {
                     nativeTexturePath = BabyArt.layer(nativeTexturePath);
                  }

                  debugInfo = "native: " + nativeTexturePath;

                  try {
                     layerImage = this.loadNativeImage(nativeTexturePath);
                  } catch (Exception e) {
                     LOGGER.debug("Failed to load native texture: {} - {}", nativeTexturePath, e.getMessage());
                     continue;
                  }
               } else {
                  ResourceLocation grayscalePath = this.layerResolver.getGrayscaleTexture(layer, genotype, config, speciesId, sIdx, paletteDraws);
                  if (young && grayscalePath != null) {
                     grayscalePath = BabyArt.layer(grayscalePath);
                  }

                  NativeImage sourceImage = null;

                  try {
                     sourceImage = this.loadNativeImage(grayscalePath);
                  } catch (Exception e) {
                     LOGGER.debug("Failed to load grayscale texture: {}", grayscalePath);
                     continue;
                  }

                  if (sourceImage == null) {
                     continue;
                  }

                  boolean[] channels = this.detectActiveChannels(sourceImage);
                  PaletteLayerResolver.PaletteBlendResult resGray = null;
                  PaletteLayerResolver.PaletteBlendResult resRed = null;
                  PaletteLayerResolver.PaletteBlendResult resGreen = null;
                  PaletteLayerResolver.PaletteBlendResult resBlue = null;
                  StringBuilder debugBuilder = new StringBuilder();
                  debugBuilder.append("grayscale: ").append(grayscalePath);
                  Locus colorLocus = layer.getColorLocus();
                  int colorBaseStart = config.getPairIndexOffset(colorLocus);
                  if (channels[0]) {
                     resGray = this.consumeAndResolve(
                        layer, genotype, config, speciesId, colorLocus, colorBaseStart, consumedColorUnits, "Gray", debugBuilder, paletteDraws
                     );
                  }

                  if (channels[1]) {
                     resRed = this.consumeAndResolve(
                        layer, genotype, config, speciesId, colorLocus, colorBaseStart, consumedColorUnits, "Red", debugBuilder, paletteDraws
                     );
                  }

                  if (channels[2]) {
                     resGreen = this.consumeAndResolve(
                        layer, genotype, config, speciesId, colorLocus, colorBaseStart, consumedColorUnits, "Green", debugBuilder, paletteDraws
                     );
                  }

                  if (channels[3]) {
                     resBlue = this.consumeAndResolve(
                        layer, genotype, config, speciesId, colorLocus, colorBaseStart, consumedColorUnits, "Blue", debugBuilder, paletteDraws
                     );
                  }

                  if (structLocus != null) {
                     int used = this.countTrue(channels);
                     int waste = reservedChannels - used;
                     if (waste > 0) {
                        int current = consumedColorUnits.getOrDefault(colorLocus, 0);
                        consumedColorUnits.put(colorLocus, current + waste);
                        debugBuilder.append(" [Skipped ").append(waste).append(" unused channels]");
                     }
                  }

                  int[] palGray = this.loadPaletteFromBlend(resGray);
                  int[] palRed = this.loadPaletteFromBlend(resRed);
                  int[] palGreen = this.loadPaletteFromBlend(resGreen);
                  int[] palBlue = this.loadPaletteFromBlend(resBlue);
                  layerImage = PaletteRecolorer.recolorMultiChannel(sourceImage, palRed, palGreen, palBlue, palGray);
                  sourceImage.close();
                  debugInfo = debugBuilder.toString();
               }

               if (layerImage != null) {
                  if (result == null) {
                     result = new NativeImage(layerImage.getWidth(), layerImage.getHeight(), true);
                     this.clearToTransparent(result);
                     LOGGER.trace("Initialized result texture for {} with size {}x{}", new Object[]{speciesId, layerImage.getWidth(), layerImage.getHeight()});
                  }

                  this.alphaBlendOver(result, layerImage);
                  layerImage.close();
                  layersComposited++;
                  LOGGER.trace("Composited layer: {} Expr#{} ({})", new Object[]{layer.name(), sIdx, debugInfo});
               }
            }
         }
      }

      if (layersComposited == 0) {
         LOGGER.warn("No layers composited for {} genotype {}", speciesId, genotype.toString());
         if (result != null) {
            result.close();
         }

         return null;
      } else {
         if (saddleOverlay != null && result != null) {
            NativeImage saddleImage = this.loadNativeImage(saddleOverlay);
            if (saddleImage != null) {
               this.alphaBlendOver(result, saddleImage);
               saddleImage.close();
               LOGGER.trace("Composited saddle overlay: {}", saddleOverlay);
            }
         }

         LOGGER.debug("[GeneticsCompositor] Finished composing {} texture: {} layers rendered", speciesId, layersComposited);
         return result;
      }
   }

   public NativeImage composeSimpleOverlay(ResourceLocation base, ResourceLocation overlay) {
      NativeImage baseImage = this.loadNativeImage(base);
      if (baseImage == null) {
         LOGGER.warn("Failed to load base texture for simple overlay: {}", base);
         return null;
      }

      NativeImage overlayImage = this.loadNativeImage(overlay);
      if (overlayImage != null) {
         this.alphaBlendOver(baseImage, overlayImage);
         overlayImage.close();
      } else {
         LOGGER.warn("Failed to load overlay texture: {}", overlay);
      }

      return baseImage;
   }

   private int countTrue(boolean[] bools) {
      int c = 0;

      for (boolean b : bools) {
         if (b) {
            c++;
         }
      }

      return c;
   }

   private PaletteLayerResolver.PaletteBlendResult consumeAndResolve(
      GeneticsLayer layer,
      Genotype genotype,
      SpeciesGeneticsConfig config,
      String speciesId,
      Locus colorLocus,
      int colorBaseStart,
      Map<Locus, Integer> consumedMap,
      String channelName,
      StringBuilder debugBuilder,
      PaletteDraws paletteDraws
   ) {
      int formIndex = consumedMap.getOrDefault(colorLocus, 0);
      int actualPairIndex = colorBaseStart + formIndex * 3;
      PaletteLayerResolver.PaletteBlendResult result = this.layerResolver
         .getPaletteBlend(layer, genotype, config, speciesId, actualPairIndex, formIndex, paletteDraws);
      if (result == null) {
         debugBuilder.append("\n  [").append(channelName).append("]: MISSING");
         consumedMap.put(colorLocus, formIndex + 1);
         return null;
      }

      if (result.isSinglePalette) {
         debugBuilder.append("\n  [").append(channelName).append("]: ").append(result.palette1.getPath());
      } else {
         debugBuilder.append("\n  [")
            .append(channelName)
            .append("]: Blend ")
            .append(result.percent1)
            .append("% (")
            .append(result.palette1.getPath())
            .append(" + ")
            .append(result.palette2.getPath())
            .append(")");
      }

      consumedMap.put(colorLocus, formIndex + 1);
      return result;
   }

   private int[] loadPaletteFromBlend(PaletteLayerResolver.PaletteBlendResult result) {
      if (result == null) {
         return null;
      }

      if (result.isSinglePalette) {
         return PaletteRecolorer.load1x6Palette(result.palette1);
      }

      int[] p1 = PaletteRecolorer.load1x6Palette(result.palette1);
      int[] p2 = PaletteRecolorer.load1x6Palette(result.palette2);
      return this.blendPalettes(p1, p2, result.percent1 / 100.0F);
   }

   private int[] blendPalettes(int[] p1, int[] p2, float ratio) {
      int[] result = new int[6];
      float recessiveOpacity = 1.0F - ratio;

      for (int i = 0; i < 6; i++) {
         int c1 = p1[i];
         int c2 = p2[i];
         int a1 = c1 >> 24 & 0xFF;
         int b1 = c1 >> 16 & 0xFF;
         int g1 = c1 >> 8 & 0xFF;
         int r1 = c1 & 0xFF;
         int a2 = c2 >> 24 & 0xFF;
         int b2 = c2 >> 16 & 0xFF;
         int g2 = c2 >> 8 & 0xFF;
         int r2 = c2 & 0xFF;
         float effAlpha = a2 / 255.0F * recessiveOpacity;
         int outR = (int)(r2 * effAlpha + r1 * (1.0F - effAlpha));
         int outG = (int)(g2 * effAlpha + g1 * (1.0F - effAlpha));
         int outB = (int)(b2 * effAlpha + b1 * (1.0F - effAlpha));
         int outA = Math.max(a1, (int)(a2 * recessiveOpacity));
         result[i] = outA << 24 | outB << 16 | outG << 8 | outR;
      }

      return result;
   }

   private void clearToTransparent(NativeImage image) {
      for (int y = 0; y < image.getHeight(); y++) {
         for (int x = 0; x < image.getWidth(); x++) {
            image.setPixelRGBA(x, y, 0);
         }
      }
   }

   private void alphaBlendOver(NativeImage dst, NativeImage src) {
      int width = Math.min(dst.getWidth(), src.getWidth());
      int height = Math.min(dst.getHeight(), src.getHeight());

      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            int srcColor = src.getPixelRGBA(x, y);
            int dstColor = dst.getPixelRGBA(x, y);
            int srcA = srcColor >> 24 & 0xFF;
            int srcB = srcColor >> 16 & 0xFF;
            int srcG = srcColor >> 8 & 0xFF;
            int srcR = srcColor & 0xFF;
            int dstA = dstColor >> 24 & 0xFF;
            int dstB = dstColor >> 16 & 0xFF;
            int dstG = dstColor >> 8 & 0xFF;
            int dstR = dstColor & 0xFF;
            if (srcA != 0) {
               if (srcA == 255) {
                  dst.setPixelRGBA(x, y, srcColor);
               } else {
                  float sA = srcA / 255.0F;
                  float dA = dstA / 255.0F;
                  float oA = sA + dA * (1.0F - sA);
                  if (oA < 0.001F) {
                     dst.setPixelRGBA(x, y, 0);
                  } else {
                     int outR = (int)((srcR * sA + dstR * dA * (1.0F - sA)) / oA);
                     int outG = (int)((srcG * sA + dstG * dA * (1.0F - sA)) / oA);
                     int outB = (int)((srcB * sA + dstB * dA * (1.0F - sA)) / oA);
                     int outA = (int)(oA * 255.0F);
                     outR = Math.min(255, Math.max(0, outR));
                     outG = Math.min(255, Math.max(0, outG));
                     outB = Math.min(255, Math.max(0, outB));
                     outA = Math.min(255, Math.max(0, outA));
                     int outColor = outA << 24 | outB << 16 | outG << 8 | outR;
                     dst.setPixelRGBA(x, y, outColor);
                  }
               }
            }
         }
      }
   }

   private NativeImage loadNativeImage(ResourceLocation texturePath) {
      try {
         Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texturePath);
         if (resource.isEmpty()) {
            LOGGER.debug("Native texture not found: {}", texturePath);
            return null;
         }

         try (InputStream in = resource.get().open()) {
            return NativeImage.read(in);
         }
      } catch (IOException e) {
         LOGGER.debug("Failed to load native texture: {} - {}", texturePath, e.getMessage());
         return null;
      }
   }

   private boolean[] detectActiveChannels(NativeImage image) {
      boolean[] channels = new boolean[4];
      int w = image.getWidth();
      int h = image.getHeight();

      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            int px = image.getPixelRGBA(x, y);
            int a = px >> 24 & 0xFF;
            if (a != 0) {
               int r = px & 0xFF;
               int g = px >> 8 & 0xFF;
               int b = px >> 16 & 0xFF;
               if (r == g && g == b) {
                  channels[0] = true;
               } else if (r > 0 && g == 0 && b == 0) {
                  channels[1] = true;
               } else if (g > 0 && r == 0 && b == 0) {
                  channels[2] = true;
               } else if (b > 0 && r == 0 && g == 0) {
                  channels[3] = true;
               }

               if (channels[0] && channels[1] && channels[2] && channels[3]) {
                  return channels;
               }
            }
         }
      }

      return channels;
   }
}
