package net.magister.bookofdragons.client.render.misc;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import net.magister.bookofdragons.entity.custom.toy.PlushKind;
import net.magister.bookofdragons.entity.custom.toy.PlushSkins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.DyeColor;
import org.slf4j.Logger;

final class PlushTints {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<String, ResourceLocation> MADE = new HashMap<>();
   private static final int WHITE = 250;

   private PlushTints() {
   }

   static ResourceLocation texture(PlushKind kind, @Nullable DyeColor color) {
      PlushSkins.Skins skins = PlushSkins.of(kind);
      if (skins != null) {
         ResourceLocation fallback = skins.fallback() == null ? kind.textureFile() : skins.fallback();
         return color == null ? fallback : skins.byDye().getOrDefault(color, fallback);
      } else {
         return color != null && color != DyeColor.WHITE && kind.dyeable()
            ? MADE.computeIfAbsent(kind.id() + "_" + color.getName(), name -> make(kind, color, name))
            : kind.textureFile();
      }
   }

   static void clear() {
      for (ResourceLocation made : MADE.values()) {
         Minecraft.getInstance().getTextureManager().release(made);
      }

      MADE.clear();
   }

   private static ResourceLocation make(PlushKind kind, DyeColor color, String name) {
      ResourceManager resources = Minecraft.getInstance().getResourceManager();

      try (InputStream base = resources.open(kind.textureFile())) {
         NativeImage image = NativeImage.read(base);
         Optional<Resource> woolFile = resources.getResource(kind.woolFile());
         int dye = color.getFireworkColor();
         int red = dye >> 16 & 0xFF;
         int green = dye >> 8 & 0xFF;
         int blue = dye & 0xFF;
         if (woolFile.isPresent()) {
            try (InputStream in = woolFile.get().open()) {
               NativeImage wool = NativeImage.read(in);

               try {
                  int width = Math.min(image.getWidth(), wool.getWidth());
                  int height = Math.min(image.getHeight(), wool.getHeight());

                  for (int y = 0; y < height; y++) {
                     for (int x = 0; x < width; x++) {
                        int pixel = wool.getPixelRGBA(x, y);
                        if (pixel >>> 24 != 0) {
                           image.setPixelRGBA(
                              x, y, abgr(pixel >>> 24, (pixel & 0xFF) * red / 255, (pixel >> 8 & 0xFF) * green / 255, (pixel >> 16 & 0xFF) * blue / 255)
                           );
                        }
                     }
                  }
               } catch (Throwable var21) {
                  if (wool != null) {
                     try {
                        wool.close();
                     } catch (Throwable var20) {
                        var21.addSuppressed(var20);
                     }
                  }

                  throw var21;
               }

               if (wool != null) {
                  wool.close();
               }
            }
         } else {
            for (int y = 0; y < image.getHeight(); y++) {
               for (int x = 0; x < image.getWidth(); x++) {
                  int pixel = image.getPixelRGBA(x, y);
                  boolean white = pixel >>> 24 == 255 && (pixel & 0xFF) >= 250 && (pixel >> 8 & 0xFF) >= 250 && (pixel >> 16 & 0xFF) >= 250;
                  if (white) {
                     image.setPixelRGBA(x, y, abgr(255, red, green, blue));
                  }
               }
            }
         }

         ResourceLocation made = new ResourceLocation("bookofdragons", "dynamic/plush/" + name);
         Minecraft.getInstance().getTextureManager().register(made, new DynamicTexture(image));
         return made;
      } catch (Exception unreadable) {
         LOGGER.warn("{} could not be dyed {}: {}", new Object[]{kind.textureFile(), color.getName(), unreadable.toString()});
         return kind.textureFile();
      }
   }

   private static int abgr(int alpha, int red, int green, int blue) {
      return alpha << 24 | blue << 16 | green << 8 | red;
   }
}
