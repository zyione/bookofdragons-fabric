package net.magister.liberanimalium.compat.v20;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.RenderType;

public final class MeshesImpl {
   private MeshesImpl() {
   }

   public static BufferBuilder immediate(Mode mode, VertexFormat format) {
      BufferBuilder builder = Tesselator.getInstance().getBuilder();
      builder.begin(mode, format);
      return builder;
   }

   public static void draw(BufferBuilder builder) {
      BufferUploader.drawWithShader(builder.end());
   }

   public static void upload(VertexBuffer target, Mode mode, VertexFormat format, ByteBuffer vertices) {
      BufferBuilder builder = new BufferBuilder(vertices.limit());
      builder.begin(mode, format);
      ((net.magister.liberanimalium.platform.client.BulkVertexUpload)builder).liberanimalium$upload(vertices);
      target.upload(builder.end());
   }

   public static final class Batch {
      private final RenderType type;
      private final BufferBuilder builder;

      public Batch(RenderType type, int capacity) {
         this.type = type;
         this.builder = new BufferBuilder(capacity);
      }

      public BufferBuilder open(Mode mode, VertexFormat format) {
         if (!this.builder.building()) {
            this.builder.begin(mode, format);
         }

         return this.builder;
      }

      public boolean isOpen() {
         return this.builder.building();
      }

      public void draw(VertexSorting sorting) {
         this.type.end(this.builder, sorting);
      }

      public void abandon() {
         this.builder.end().release();
         this.builder.discard();
      }

      public void close() {
      }
   }
}
