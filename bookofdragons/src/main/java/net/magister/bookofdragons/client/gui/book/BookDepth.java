package net.magister.bookofdragons.client.gui.book;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class BookDepth {
   public static final int LAYERS = 7;
   private static final int W = 384;
   private static final int H = 256;
   private static final Logger LOGGER = LogManager.getLogger("BookDepth");
   private static final ResourceLocation BOOK = new ResourceLocation("bookofdragons", "textures/gui/book/book.png");
   private static final ResourceLocation DEPTH = new ResourceLocation("bookofdragons", "textures/gui/book/book_depth.png");
   private static final Map<Integer, ResourceLocation> FRONT = new HashMap<>();
   private static boolean built;

   private BookDepth() {
   }

   public static void reset() {
      Minecraft mc = Minecraft.getInstance();

      for (ResourceLocation tex : FRONT.values()) {
         mc.getTextureManager().release(tex);
      }

      FRONT.clear();
      built = false;
   }

   public static void drawOver(GuiGraphics graphics, int depth, int leftX, int topY) {
      if (depth > 0) {
         if (!built) {
            build();
         }

         ResourceLocation tex = FRONT.get(Math.min(depth, 7));
         if (tex != null) {
            graphics.blit(tex, leftX, topY, 0.0F, 0.0F, 384, 256, 384, 256);
         }
      }
   }

   private static void build() {
      built = true;

      try {
         NativeImage book = read(BOOK);

         label120: {
            try {
               NativeImage depth = read(DEPTH);

               label122: {
                  try {
                     if (book == null || depth == null || book.getWidth() != depth.getWidth() || book.getHeight() != depth.getHeight()) {
                        LOGGER.warn("[BookDepth] book_depth.png missing or not the size of book.png; ribbons lie over the book");
                        break label122;
                     }

                     Minecraft mc = Minecraft.getInstance();

                     for (int d = 1; d <= 7; d++) {
                        NativeImage front = new NativeImage(book.getWidth(), book.getHeight(), true);

                        for (int y = 0; y < book.getHeight(); y++) {
                           for (int x = 0; x < book.getWidth(); x++) {
                              int layer = layer(depth.getPixelRGBA(x, y));
                              front.setPixelRGBA(x, y, layer >= 1 && layer <= d ? book.getPixelRGBA(x, y) : 0);
                           }
                        }

                        ResourceLocation tex = new ResourceLocation("bookofdragons", "book_front_" + d);
                        mc.getTextureManager().register(tex, new DynamicTexture(front));
                        FRONT.put(d, tex);
                     }
                  } catch (Throwable var10) {
                     if (depth != null) {
                        try {
                           depth.close();
                        } catch (Throwable var9) {
                           var10.addSuppressed(var9);
                        }
                     }

                     throw var10;
                  }

                  if (depth != null) {
                     depth.close();
                  }
                  break label120;
               }

               if (depth != null) {
                  depth.close();
               }
            } catch (Throwable var11) {
               if (book != null) {
                  try {
                     book.close();
                  } catch (Throwable var8) {
                     var11.addSuppressed(var8);
                  }
               }

               throw var11;
            }

            if (book != null) {
               book.close();
            }

            return;
         }

         if (book != null) {
            book.close();
         }
      } catch (Exception e) {
         LOGGER.warn("[BookDepth] cannot build the book's layers: {}", e.toString());
      }
   }

   private static int layer(int abgr) {
      return abgr >>> 24 < 128 ? 0 : Math.round((abgr & 0xFF) / 32.0F);
   }

   private static NativeImage read(ResourceLocation location) throws Exception {
      Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
      if (resource.isEmpty()) {
         return null;
      }

      try (InputStream in = resource.get().open()) {
         return NativeImage.read(Format.RGBA, in);
      }
   }
}
