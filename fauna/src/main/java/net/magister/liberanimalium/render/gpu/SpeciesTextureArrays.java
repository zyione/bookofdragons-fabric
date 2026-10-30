package net.magister.liberanimalium.render.gpu;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.slf4j.Logger;

public final class SpeciesTextureArrays {
   private static final Logger LOGGER = LogUtils.getLogger();
   static final int FIRST_CAPACITY = 8;
   public static final int UNIT = 0;
   private static final Map<String, SpeciesTextureArrays.SpeciesArray> ARRAYS = new HashMap<>();
   public static final int NONE = -1;

   private SpeciesTextureArrays() {
   }

   public static int layerFor(String species, ResourceLocation texture) {
      SpeciesTextureArrays.SpeciesArray array = ARRAYS.computeIfAbsent(species, SpeciesTextureArrays.SpeciesArray::new);
      Integer known = array.layers.get(texture);
      if (known != null) {
         return known;
      }

      if (array.refused.contains(texture)) {
         return -1;
      }

      int layer = array.add(texture);
      if (layer == -1) {
         array.refused.add(texture);
      }

      return layer;
   }

   public static void bind(String species) {
      SpeciesTextureArrays.SpeciesArray array = ARRAYS.get(species);
      int id = array == null ? 0 : array.id;
      int previous = GlStateManager._getActiveTexture();
      GlStateManager._activeTexture(33984);
      GL11.glBindTexture(35866, id);
      GlStateManager._activeTexture(previous);
   }

   public static void unbind() {
      int previous = GlStateManager._getActiveTexture();
      GlStateManager._activeTexture(33984);
      GL11.glBindTexture(35866, 0);
      GlStateManager._activeTexture(previous);
   }

   public static void forget(ResourceLocation texture) {
      for (SpeciesTextureArrays.SpeciesArray array : ARRAYS.values()) {
         array.remove(texture);
      }
   }

   public static void release() {
      for (SpeciesTextureArrays.SpeciesArray array : ARRAYS.values()) {
         array.delete();
      }

      ARRAYS.clear();
   }

   public static List<String> report() {
      List<String> out = new ArrayList<>();

      for (SpeciesTextureArrays.SpeciesArray a : ARRAYS.values()) {
         out.add(
            String.format(Locale.ROOT, "%s: %dx%d, %d layers of %d, %d refused", a.species, a.width, a.height, a.layers.size(), a.capacity, a.refused.size())
         );
      }

      return out;
   }

   static int[] scaleNearest(int[] src, int sw, int sh, int dw, int dh) {
      if (sw == dw && sh == dh) {
         return src;
      }

      int[] out = new int[dw * dh];

      for (int y = 0; y < dh; y++) {
         int sy = (int)((long)y * sh / dh);
         int rowS = sy * sw;
         int rowD = y * dw;

         for (int x = 0; x < dw; x++) {
            out[rowD + x] = src[rowS + (int)((long)x * sw / dw)];
         }
      }

      return out;
   }

   static int fit(int have, int want) {
      return Math.max(have, want);
   }

   static boolean exact(int w, int h, int sw, int sh) {
      return sw > 0 && sh > 0 && w % sw == 0 && h % sh == 0;
   }

   private static SpeciesTextureArrays.Pixels read(ResourceLocation texture) {
      if (Minecraft.getInstance().getTextureManager().getTexture(texture, null) instanceof DynamicTexture dynamic) {
         NativeImage image = dynamic.getPixels();
         if (image != null && image.format() == Format.RGBA) {
            try {
               return new SpeciesTextureArrays.Pixels(image.getPixelsRGBA(), image.getWidth(), image.getHeight());
            } catch (IllegalStateException closed) {
               return null;
            }
         } else {
            return null;
         }
      } else {
         Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
         if (resource.isEmpty()) {
            return null;
         }

         try (InputStream in = resource.get().open()) {
            NativeImage image = NativeImage.read(in);

            SpeciesTextureArrays.Pixels var15;
            label110: {
               try {
                  if (image.format() == Format.RGBA) {
                     var15 = new SpeciesTextureArrays.Pixels(image.getPixelsRGBA(), image.getWidth(), image.getHeight());
                     break label110;
                  }

                  var15 = null;
               } catch (Throwable var10) {
                  if (image != null) {
                     try {
                        image.close();
                     } catch (Throwable var8) {
                        var10.addSuppressed(var8);
                     }
                  }

                  throw var10;
               }

               if (image != null) {
                  image.close();
               }

               return var15;
            }

            if (image != null) {
               image.close();
            }

            return var15;
         } catch (Exception e) {
            LOGGER.warn("Could not read {} for the species texture array: {}", texture, e.toString());
            return null;
         }
      }
   }

   private record Pixels(int[] abgr, int width, int height) {
   }

   private static final class SpeciesArray {
      final String species;
      int id;
      int width;
      int height;
      int capacity;
      final Map<ResourceLocation, Integer> layers = new HashMap<>();
      final Set<ResourceLocation> refused = new HashSet<>();
      final ArrayDeque<Integer> free = new ArrayDeque<>();
      int next;

      SpeciesArray(String species) {
         this.species = species;
      }

      int add(ResourceLocation texture) {
         RenderSystem.assertOnRenderThread();
         SpeciesTextureArrays.Pixels pixels = SpeciesTextureArrays.read(texture);
         if (pixels == null) {
            return -1;
         }

         int w = SpeciesTextureArrays.fit(this.width, pixels.width);
         int h = SpeciesTextureArrays.fit(this.height, pixels.height);
         if (SpeciesTextureArrays.exact(w, h, pixels.width, pixels.height) && (this.width <= 0 || SpeciesTextureArrays.exact(w, h, this.width, this.height))) {
            if ((this.id == 0 || w != this.width || h != this.height) && !this.recreate(w, h, Math.max(this.capacity, 8))) {
               return -1;
            }

            Integer slot = this.free.poll();
            if (slot == null) {
               if (this.next >= this.capacity && !this.recreate(this.width, this.height, this.capacity * 2)) {
                  return -1;
               }

               slot = this.next++;
            }

            this.upload(slot, pixels);
            this.layers.put(texture, slot);
            return slot;
         } else {
            SpeciesTextureArrays.LOGGER
               .warn(
                  "{} is {}x{}, which does not scale exactly into the {} array at {}x{}; it draws on the CPU path",
                  new Object[]{texture, pixels.width, pixels.height, this.species, w, h}
               );
            return -1;
         }
      }

      void remove(ResourceLocation texture) {
         Integer slot = this.layers.remove(texture);
         if (slot != null) {
            this.free.add(slot);
         }

         this.refused.remove(texture);
      }

      boolean recreate(int w, int h, int wantCapacity) {
         int max = GL11.glGetInteger(35071);
         if (wantCapacity > max) {
            SpeciesTextureArrays.LOGGER.warn("The {} texture array wants {} layers, the driver allows {}", new Object[]{this.species, wantCapacity, max});
            return false;
         }

         int old = this.id;
         this.id = GL11.glGenTextures();
         int previous = GlStateManager._getActiveTexture();
         GlStateManager._activeTexture(33984);
         GL11.glBindTexture(35866, this.id);
         GL11.glTexParameteri(35866, 10241, 9728);
         GL11.glTexParameteri(35866, 10240, 9728);
         GL11.glTexParameteri(35866, 33084, 0);
         GL11.glTexParameteri(35866, 33085, 0);
         GL12.glTexImage3D(35866, 0, 32856, w, h, wantCapacity, 0, 6408, 5121, (ByteBuffer)null);
         GL11.glBindTexture(35866, 0);
         GlStateManager._activeTexture(previous);
         this.width = w;
         this.height = h;
         this.capacity = wantCapacity;
         if (old != 0) {
            GL11.glDeleteTextures(old);

            for (Entry<ResourceLocation, Integer> e : new ArrayList<>(this.layers.entrySet())) {
               SpeciesTextureArrays.Pixels p = SpeciesTextureArrays.read(e.getKey());
               if (p == null) {
                  this.layers.remove(e.getKey());
                  this.free.add(e.getValue());
               } else {
                  this.upload(e.getValue(), p);
               }
            }
         }

         return true;
      }

      void upload(int slot, SpeciesTextureArrays.Pixels pixels) {
         int[] scaled = SpeciesTextureArrays.scaleNearest(pixels.abgr, pixels.width, pixels.height, this.width, this.height);
         int previous = GlStateManager._getActiveTexture();
         GlStateManager._activeTexture(33984);
         GL11.glBindTexture(35866, this.id);
         GlStateManager._pixelStore(3314, 0);
         GlStateManager._pixelStore(3315, 0);
         GlStateManager._pixelStore(3316, 0);
         GlStateManager._pixelStore(3317, 4);
         GL12.glTexSubImage3D(35866, 0, 0, 0, slot, this.width, this.height, 1, 6408, 5121, scaled);
         GL11.glBindTexture(35866, 0);
         GlStateManager._activeTexture(previous);
      }

      void delete() {
         if (this.id != 0) {
            GL11.glDeleteTextures(this.id);
            this.id = 0;
         }

         this.layers.clear();
         this.refused.clear();
         this.free.clear();
         this.next = 0;
         this.capacity = 0;
         this.width = 0;
         this.height = 0;
      }
   }
}
