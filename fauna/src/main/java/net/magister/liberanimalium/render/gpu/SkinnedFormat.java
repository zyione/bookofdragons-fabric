package net.magister.liberanimalium.render.gpu;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormatElement.Type;
import com.mojang.blaze3d.vertex.VertexFormatElement.Usage;
import net.magister.liberanimalium.compat.Formats;

public final class SkinnedFormat {
   public static final VertexFormatElement ELEMENT_BONE = Formats.element(3, Type.USHORT, Usage.UV, 2);
   public static final VertexFormatElement ELEMENT_NORMAL_F = Formats.element(0, Type.FLOAT, Usage.NORMAL, 3);
   public static final VertexFormatElement ELEMENT_FACE_NORMAL = Formats.element(4, Type.FLOAT, Usage.UV, 3);
   public static final VertexFormatElement ELEMENT_TANGENT = Formats.element(5, Type.FLOAT, Usage.UV, 4);
   public static final VertexFormatElement ELEMENT_MID_TEX = Formats.element(6, Type.FLOAT, Usage.UV, 2);
   public static final VertexFormat FORMAT = Formats.builder()
      .add("Position", Formats.POSITION)
      .add("UV0", Formats.UV0)
      .add("Normal", ELEMENT_NORMAL_F)
      .add("Bone", ELEMENT_BONE)
      .add("FaceNormal", ELEMENT_FACE_NORMAL)
      .add("Tangent", ELEMENT_TANGENT)
      .add("MidTex", ELEMENT_MID_TEX)
      .build();
   public static final int VERTEX_BYTES = 72;

   private SkinnedFormat() {
   }
}
