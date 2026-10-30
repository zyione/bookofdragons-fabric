package net.magister.liberanimalium.render.gpu;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.Field;
import net.magister.liberanimalium.mixin.BufferBuilderAccessor;
import net.minecraft.util.Mth;

public final class LayeredConsumer implements VertexConsumer {
   private final BufferBuilder builder;
   private final short layer;
   private static final Field POINTER = pointerField();

   public LayeredConsumer(BufferBuilder builder, int layer) {
      this.builder = builder;
      this.layer = (short)layer;
   }

   public BufferBuilder builder() {
      return this.builder;
   }

   public int layer() {
      return this.layer & 65535;
   }

   public void vertex(
      float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float nx, float ny, float nz
   ) {
      BufferBuilder b = this.builder;
      b.putFloat(0, x);
      b.putFloat(4, y);
      b.putFloat(8, z);
      b.putByte(12, (byte)(red * 255.0F));
      b.putByte(13, (byte)(green * 255.0F));
      b.putByte(14, (byte)(blue * 255.0F));
      b.putByte(15, (byte)(alpha * 255.0F));
      b.putFloat(16, u);
      b.putFloat(20, v);
      b.putShort(24, (short)(overlay & 65535));
      b.putShort(26, (short)(overlay >> 16 & 65535));
      b.putShort(28, (short)(light & 65535));
      b.putShort(30, (short)(light >> 16 & 65535));
      b.putByte(32, normalIntValue(nx));
      b.putByte(33, normalIntValue(ny));
      b.putByte(34, normalIntValue(nz));
      b.putShort(36, this.layer);
      advance(b, 40);
      b.endVertex();
   }

   private static byte normalIntValue(float n) {
      return (byte)((int)(Mth.clamp(n, -1.0F, 1.0F) * 127.0F) & 0xFF);
   }

   private static void advance(BufferBuilder b, int bytes) {
      if (b instanceof BufferBuilderAccessor access) {
         access.liberanimalium$setNextElementByte(access.liberanimalium$nextElementByte() + bytes);
      } else {
         if (POINTER == null) {
            throw new IllegalStateException("BufferBuilderAccessor is not applied and the field is unreachable");
         }

         try {
            POINTER.setInt(b, POINTER.getInt(b) + bytes);
         } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
         }
      }
   }

   private static Field pointerField() {
      try {
         Field f = BufferBuilder.class.getDeclaredField("nextElementByte");
         f.setAccessible(true);
         return f;
      } catch (ReflectiveOperationException | RuntimeException e) {
         return null;
      }
   }

   public VertexConsumer vertex(double x, double y, double z) {
      this.builder.vertex(x, y, z);
      return this;
   }

   public VertexConsumer color(int red, int green, int blue, int alpha) {
      this.builder.color(red, green, blue, alpha);
      return this;
   }

   public VertexConsumer uv(float u, float v) {
      this.builder.uv(u, v);
      return this;
   }

   public VertexConsumer overlayCoords(int u, int v) {
      this.builder.overlayCoords(u, v);
      return this;
   }

   public VertexConsumer uv2(int u, int v) {
      this.builder.uv2(u, v);
      return this;
   }

   public VertexConsumer normal(float x, float y, float z) {
      this.builder.normal(x, y, z);
      return this;
   }

   public void endVertex() {
      this.builder.putShort(0, this.layer);
      this.builder.nextElement();
      this.builder.endVertex();
   }

   public void defaultColor(int r, int g, int b, int a) {
      this.builder.defaultColor(r, g, b, a);
   }

   public void unsetDefaultColor() {
      this.builder.unsetDefaultColor();
   }
}
