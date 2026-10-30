package net.magister.liberanimalium.render.gpu;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormatElement.Type;
import com.mojang.blaze3d.vertex.VertexFormatElement.Usage;
import java.util.List;
import net.magister.liberanimalium.compat.Formats;

public final class IrisEntityFormat {
   private static final VertexFormatElement POSITION = Formats.element(0, Type.FLOAT, Usage.POSITION, 3);
   private static final VertexFormatElement COLOR_F = Formats.element(0, Type.FLOAT, Usage.COLOR, 4);
   private static final VertexFormatElement UV0 = Formats.element(0, Type.FLOAT, Usage.UV, 2);
   private static final VertexFormatElement UV1_I = Formats.element(1, Type.INT, Usage.UV, 2);
   private static final VertexFormatElement UV2_I = Formats.element(2, Type.INT, Usage.UV, 2);
   private static final VertexFormatElement NORMAL_F = Formats.element(0, Type.FLOAT, Usage.NORMAL, 3);
   private static final VertexFormatElement ENTITY_I = Formats.element(11, Type.INT, Usage.UV, 3);
   private static final VertexFormatElement MID_TEX = Formats.element(12, Type.FLOAT, Usage.UV, 2);
   private static final VertexFormatElement TANGENT_F = Formats.element(13, Type.FLOAT, Usage.UV, 4);
   public static final List<String> IRIS_ENTITY_NAMES = List.of(
      "Position", "Color", "UV0", "UV1", "UV2", "Normal", "Padding", "iris_Entity", "mc_midTexCoord", "at_tangent", "Padding2"
   );
   public static final VertexFormat FORMAT = Formats.builder()
      .add("Position", POSITION)
      .add("Color", COLOR_F)
      .add("UV0", UV0)
      .add("UV1", UV1_I)
      .add("UV2", UV2_I)
      .add("Normal", NORMAL_F)
      .pad("Padding", 0)
      .add("iris_Entity", ENTITY_I)
      .add("mc_midTexCoord", MID_TEX)
      .add("at_tangent", TANGENT_F)
      .pad("Padding2", 0)
      .build();
   public static final int VERTEX_BYTES = 100;
   public static final String[] VARYINGS = new String[]{
      "outPosition", "outColor", "outUV0", "outUV1", "outUV2", "outNormal", "outEntity", "outMidTex", "outTangent"
   };

   private IrisEntityFormat() {
   }
}
