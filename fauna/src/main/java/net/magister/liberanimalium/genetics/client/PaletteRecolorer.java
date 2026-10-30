package net.magister.liberanimalium.genetics.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaletteRecolorer {
   private static final Logger LOGGER = LoggerFactory.getLogger("PaletteRecolorer");
   private static final Map<String, int[]> PALETTE_CACHE = new ConcurrentHashMap<>();

   private PaletteRecolorer() {
   }

   public static void clearCache() {
      PALETTE_CACHE.clear();
      LOGGER.debug("Palette cache cleared");
   }

   public static int[] load1x6Palette(ResourceLocation palettePng) {
      String cacheKey = palettePng.toString();
      int[] cached = PALETTE_CACHE.get(cacheKey);
      if (cached != null) {
         return cached;
      }

      try {
         Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(palettePng);
         if (resource.isEmpty()) {
            throw new IOException("Palette not found: " + palettePng);
         }

         int[] var14;
         try (InputStream in = resource.get().open()) {
            NativeImage img = NativeImage.read(in);

            try {
               if (img.getWidth() != 6 || img.getHeight() != 1) {
                  throw new IOException("Palette must be 6x1, got " + img.getWidth() + "x" + img.getHeight() + " at " + palettePng);
               }

               int[] pal = new int[6];

               for (int x = 0; x < 6; x++) {
                  pal[x] = img.getPixelRGBA(x, 0);
               }

               PALETTE_CACHE.put(cacheKey, pal);
               LOGGER.debug("Loaded palette: {}", palettePng);
               var14 = pal;
            } catch (Throwable var10) {
               if (img != null) {
                  try {
                     img.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (img != null) {
               img.close();
            }
         }

         return var14;
      } catch (IOException e) {
         LOGGER.warn("[Palette1x6] {}", e.getMessage());
         return new int[]{-16777216, -3407617, -6749953, -10092289, -13434625, -65281};
      }
   }

   public static NativeImage recolorMultiChannel(NativeImage source, int[] palRed, int[] palGreen, int[] palBlue, int[] palGray) {
      int w = source.getWidth();
      int h = source.getHeight();
      NativeImage out = new NativeImage(w, h, true);

      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            int px = source.getPixelRGBA(x, y);
            int a = px >> 24 & 0xFF;
            int b = px >> 16 & 0xFF;
            int g = px >> 8 & 0xFF;
            int r = px & 0xFF;
            if (a == 0) {
               out.setPixelRGBA(x, y, 0);
            } else {
               int[] selectedPalette = null;
               int value = 0;
               if (r == g && g == b) {
                  selectedPalette = palGray;
                  value = r;
               } else if (r > 0 && g == 0 && b == 0) {
                  selectedPalette = palRed;
                  value = r;
               } else if (g > 0 && r == 0 && b == 0) {
                  selectedPalette = palGreen;
                  value = g;
               } else if (b > 0 && r == 0 && g == 0) {
                  selectedPalette = palBlue;
                  value = b;
               }

               if (selectedPalette == null) {
                  out.setPixelRGBA(x, y, px);
               } else {
                  int idx = grayToIndex6(value);
                  if (idx < 0) {
                     out.setPixelRGBA(x, y, px);
                  } else {
                     int palColor = selectedPalette[idx];
                     int palAlpha = palColor >> 24 & 0xFF;
                     int outAlpha = Math.min(a, palAlpha);
                     int rgb = palColor & 16777215;
                     int outPx = outAlpha << 24 | rgb;
                     out.setPixelRGBA(x, y, outPx);
                  }
               }
            }
         }
      }

      return out;
   }

   private static int grayToIndex6(int g) {
      if (g >= 1 && g <= 3) {
         return 0;
      } else if (g >= 50 && g <= 52) {
         return 1;
      } else if (g >= 101 && g <= 103) {
         return 2;
      } else if (g >= 152 && g <= 154) {
         return 3;
      } else if (g >= 204 && g <= 206) {
         return 4;
      } else {
         return g >= 254 ? 5 : -1;
      }
   }
}
