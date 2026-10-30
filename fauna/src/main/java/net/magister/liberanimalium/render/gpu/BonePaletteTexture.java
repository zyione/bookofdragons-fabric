package net.magister.liberanimalium.render.gpu;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

public final class BonePaletteTexture {
   public static final int ROW_TEXELS = 8;
   public static final int ROW_FLOATS = 32;
   private static int textureId = -1;
   private static int allocatedRows;
   private static ByteBuffer storage;
   private static FloatBuffer floats;
   public static final int PALETTE_UNIT = 3;
   private static float[] scratch = new float[0];

   private BonePaletteTexture() {
   }

   public static void pack(Matrix4f[] pose, Matrix3f[] normals, boolean[] draw, float[] out) {
      pack(pose, normals, draw, out, 0);
   }

   public static void pack(Matrix4f[] pose, Matrix3f[] normals, boolean[] draw, float[] out, int firstRow) {
      for (int i = 0; i < pose.length; i++) {
         int o = (firstRow + i) * 32;
         if (draw != null && !draw[i]) {
            for (int k = 0; k < 32; k++) {
               out[o + k] = 0.0F;
            }

            out[o + 15] = 1.0F;
         } else {
            pose[i].get(out, o);
            Matrix3f n = normals[i];
            out[o + 16] = n.m00();
            out[o + 17] = n.m01();
            out[o + 18] = n.m02();
            out[o + 19] = 0.0F;
            out[o + 20] = n.m10();
            out[o + 21] = n.m11();
            out[o + 22] = n.m12();
            out[o + 23] = 0.0F;
            out[o + 24] = n.m20();
            out[o + 25] = n.m21();
            out[o + 26] = n.m22();
            out[o + 27] = 0.0F;
            out[o + 28] = 0.0F;
            out[o + 29] = 0.0F;
            out[o + 30] = 0.0F;
            out[o + 31] = 0.0F;
         }
      }
   }

   public static int upload(Matrix4f[] pose, Matrix3f[] normals, boolean[] draw) {
      RenderSystem.assertOnRenderThread();
      int rows = pose.length;
      float[] scratch = rowScratch(rows);
      pack(pose, normals, draw, scratch);
      return uploadRows(scratch, rows);
   }

   public static int uploadRows(float[] packed, int rows) {
      RenderSystem.assertOnRenderThread();
      ensure(rows);
      floats.clear();
      floats.put(packed, 0, rows * 32);
      floats.flip();
      int previousUnit = GlStateManager._getActiveTexture();
      GlStateManager._activeTexture(33987);
      GlStateManager._bindTexture(textureId);
      unpackTightly();
      GL11.glTexSubImage2D(3553, 0, 0, 0, 8, rows, 6408, 5126, floats);
      GlStateManager._activeTexture(previousUnit);
      return textureId;
   }

   private static void unpackTightly() {
      GlStateManager._pixelStore(3314, 0);
      GlStateManager._pixelStore(3315, 0);
      GlStateManager._pixelStore(3316, 0);
      GlStateManager._pixelStore(3317, 4);
   }

   private static float[] rowScratch(int rows) {
      int need = rows * 32;
      if (scratch.length < need) {
         scratch = new float[need];
      }

      return scratch;
   }

   private static void ensure(int rows) {
      if (textureId == -1) {
         textureId = GlStateManager._genTexture();
         allocatedRows = 0;
      }

      if (rows > allocatedRows) {
         int want = Math.max(64, Integer.highestOneBit(rows - 1) * 2);
         int bytes = want * 32 * 4;
         storage = storage == null ? allocate(bytes) : reallocate(storage, bytes);
         floats = storage.asFloatBuffer();
         int previousUnit = GlStateManager._getActiveTexture();
         GlStateManager._activeTexture(33987);
         GlStateManager._bindTexture(textureId);
         GlStateManager._texParameter(3553, 10241, 9728);
         GlStateManager._texParameter(3553, 10240, 9728);
         GlStateManager._texParameter(3553, 10242, 33071);
         GlStateManager._texParameter(3553, 10243, 33071);
         GlStateManager._texParameter(3553, 33084, 0);
         GlStateManager._texParameter(3553, 33085, 0);
         unpackTightly();
         GL11.glTexImage2D(3553, 0, 34836, 8, want, 0, 6408, 5126, (FloatBuffer)null);
         GlStateManager._activeTexture(previousUnit);
         allocatedRows = want;
      }
   }

   public static void release() {
      if (textureId != -1) {
         GlStateManager._deleteTexture(textureId);
         textureId = -1;
         allocatedRows = 0;
      }
   }

   public static int rows() {
      return allocatedRows;
   }

   private static ByteBuffer allocate(int bytes) {
      long address = MemoryUtil.getAllocator(false).malloc(bytes);
      if (address == 0L) {
         throw new OutOfMemoryError("Failed to allocate " + bytes + " bytes");
      } else {
         return MemoryUtil.memByteBuffer(address, bytes);
      }
   }

   private static ByteBuffer reallocate(ByteBuffer buffer, int bytes) {
      long address = MemoryUtil.getAllocator(false).realloc(MemoryUtil.memAddress0(buffer), bytes);
      if (address == 0L) {
         throw new OutOfMemoryError("Failed to resize buffer from " + buffer.capacity() + " bytes to " + bytes + " bytes");
      } else {
         return MemoryUtil.memByteBuffer(address, bytes);
      }
   }
}
