package net.magister.liberanimalium.compat;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormatElement.Type;
import com.mojang.blaze3d.vertex.VertexFormatElement.Usage;
import net.magister.liberanimalium.compat.v20.FormatsImpl;

public final class Formats {
   public static final VertexFormatElement POSITION = FormatsImpl.POSITION;
   public static final VertexFormatElement COLOR = FormatsImpl.COLOR;
   public static final VertexFormatElement UV0 = FormatsImpl.UV0;
   public static final VertexFormatElement UV1 = FormatsImpl.UV1;
   public static final VertexFormatElement UV2 = FormatsImpl.UV2;
   public static final VertexFormatElement NORMAL = FormatsImpl.NORMAL;

   private Formats() {
   }

   public static VertexFormatElement element(int index, Type type, Usage usage, int count) {
      return FormatsImpl.element(index, type, usage, count);
   }

   public static int byteSize(VertexFormatElement element) {
      return FormatsImpl.byteSize(element);
   }

   public static boolean isPadding(VertexFormatElement element) {
      return FormatsImpl.isPadding(element);
   }

   public static Formats.Builder builder() {
      return new Formats.Builder();
   }

   public static final class Builder {
      private final FormatsImpl.Builder twin = new FormatsImpl.Builder();

      private Builder() {
      }

      public Formats.Builder add(String name, VertexFormatElement element) {
         this.twin.add(name, element);
         return this;
      }

      public Formats.Builder pad(String name, int bytes) {
         this.twin.pad(name, bytes);
         return this;
      }

      public VertexFormat build() {
         return this.twin.build();
      }
   }
}
