package net.magister.liberanimalium.render.gpu;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormatElement.Type;
import com.mojang.blaze3d.vertex.VertexFormatElement.Usage;
import net.magister.liberanimalium.compat.Formats;

public final class LayeredEntityFormat {
   public static final VertexFormatElement ELEMENT_LAYER = Formats.element(3, Type.USHORT, Usage.UV, 1);
   public static final VertexFormat FORMAT = Formats.builder()
      .add("Position", Formats.POSITION)
      .add("Color", Formats.COLOR)
      .add("UV0", Formats.UV0)
      .add("UV1", Formats.UV1)
      .add("UV2", Formats.UV2)
      .add("Normal", Formats.NORMAL)
      .pad("Padding", 1)
      .add("Layer", ELEMENT_LAYER)
      .pad("Padding2", 2)
      .build();
   public static final int VERTEX_BYTES = 40;
   public static final int LAYER_OFFSET = 36;

   private LayeredEntityFormat() {
   }
}
