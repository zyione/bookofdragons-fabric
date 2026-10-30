package net.magister.liberanimalium.compat;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import net.magister.liberanimalium.compat.v20.MeshesImpl;
import net.minecraft.client.renderer.RenderType;

public final class Meshes {
   private Meshes() {
   }

   public static BufferBuilder immediate(Mode mode, VertexFormat format) {
      return MeshesImpl.immediate(mode, format);
   }

   public static void draw(BufferBuilder builder) {
      MeshesImpl.draw(builder);
   }

   public static void upload(VertexBuffer target, Mode mode, Meshes.Vertices vertices) {
      ByteBuffer bytes = vertices.bytes;
      bytes.position(0);
      bytes.limit(vertices.count * vertices.format.getVertexSize());
      MeshesImpl.upload(target, mode, vertices.format, bytes);
   }

   public static final class Batch {
      private final MeshesImpl.Batch twin;

      public Batch(RenderType type, int capacity) {
         this.twin = new MeshesImpl.Batch(type, capacity);
      }

      public BufferBuilder open(Mode mode, VertexFormat format) {
         return this.twin.open(mode, format);
      }

      public boolean isOpen() {
         return this.twin.isOpen();
      }

      public void draw(VertexSorting sorting) {
         this.twin.draw(sorting);
      }

      public void abandon() {
         this.twin.abandon();
      }

      public void close() {
         this.twin.close();
      }
   }

   public static final class Vertices {
      private final VertexFormat format;
      private final ByteBuffer bytes;
      private final int[] sizes;
      private final boolean[] padding;
      private int element;
      private int at;
      private int count;

      public Vertices(VertexFormat format, int capacity) {
         this.format = format;
         this.bytes = ByteBuffer.allocate(capacity * format.getVertexSize()).order(ByteOrder.nativeOrder());
         List<VertexFormatElement> elements = format.getElements();
         this.sizes = new int[elements.size()];
         this.padding = new boolean[elements.size()];

         for (int i = 0; i < this.sizes.length; i++) {
            this.sizes[i] = Formats.byteSize(elements.get(i));
            this.padding[i] = Formats.isPadding(elements.get(i));
         }
      }

      public void putFloat(int offset, float value) {
         this.bytes.putFloat(this.at + offset, value);
      }

      public void putShort(int offset, short value) {
         this.bytes.putShort(this.at + offset, value);
      }

      public void nextElement() {
         this.at = this.at + this.sizes[this.element];
         this.element = (this.element + 1) % this.sizes.length;
         if (this.padding[this.element]) {
            this.nextElement();
         }
      }

      public void nextVertex() {
         if (this.element != 0) {
            throw new IllegalStateException("Not filled all elements of the vertex");
         }

         this.count++;
      }

      public int count() {
         return this.count;
      }

      public ByteBuffer bytes() {
         return this.bytes.asReadOnlyBuffer().order(ByteOrder.nativeOrder());
      }
   }
}
