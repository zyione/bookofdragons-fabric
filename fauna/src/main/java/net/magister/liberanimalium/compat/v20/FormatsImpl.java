package net.magister.liberanimalium.compat.v20;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormatElement.Type;
import com.mojang.blaze3d.vertex.VertexFormatElement.Usage;

public final class FormatsImpl {
   public static final VertexFormatElement POSITION = DefaultVertexFormat.ELEMENT_POSITION;
   public static final VertexFormatElement COLOR = DefaultVertexFormat.ELEMENT_COLOR;
   public static final VertexFormatElement UV0 = DefaultVertexFormat.ELEMENT_UV0;
   public static final VertexFormatElement UV1 = DefaultVertexFormat.ELEMENT_UV1;
   public static final VertexFormatElement UV2 = DefaultVertexFormat.ELEMENT_UV2;
   public static final VertexFormatElement NORMAL = DefaultVertexFormat.ELEMENT_NORMAL;

   private FormatsImpl() {
   }

   public static VertexFormatElement element(int index, Type type, Usage usage, int count) {
      return new VertexFormatElement(index, type, usage, count);
   }

   public static int byteSize(VertexFormatElement element) {
      return element.getByteSize();
   }

   public static boolean isPadding(VertexFormatElement element) {
      return element.getUsage() == Usage.PADDING;
   }

   public static final class Builder {
      private final com.google.common.collect.ImmutableMap.Builder<String, VertexFormatElement> elements = ImmutableMap.builder();

      public void add(String name, VertexFormatElement element) {
         this.elements.put(name, element);
      }

      public void pad(String name, int bytes) {
         this.elements.put(name, bytes == 1 ? DefaultVertexFormat.ELEMENT_PADDING : new VertexFormatElement(0, Type.BYTE, Usage.PADDING, bytes));
      }

      public VertexFormat build() {
         return new VertexFormat(this.elements.build());
      }
   }
}
