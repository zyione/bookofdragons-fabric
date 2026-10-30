package net.magister.liberanimalium.genetics;

import java.util.Arrays;

public enum GeneticsLayer {
   BASE(1, "base", Locus.b, null, false),
   GLOW(12, "glow", Locus.g, null, false),
   EYES(13, "eyes", Locus.e, Locus.s, false),
   GRADIENT(2, "gradient", Locus.n, Locus.h, false),
   DORSAL(4, "dorsal", Locus.r, Locus.d, false),
   UNDERBELLY(5, "underbelly", Locus.y, Locus.u, false),
   MEMBRANE(6, "membrane", Locus.c, Locus.q, false),
   WING_GRADIENT(7, "winggradient", Locus.v, Locus.k, false),
   BODY_MARKINGS(3, "body", Locus.j, Locus.p, false),
   OVERWING(8, "overwing", Locus.a, Locus.w, false),
   UNDERWING(9, "underwing", Locus.i, Locus.x, false),
   FACE(10, "face", Locus.z, Locus.f, false),
   LIMB(11, "limb", Locus.z, Locus.l, false);

   private final int renderOrder;
   private final String texturePrefix;
   private final Locus colorLocus;
   private final Locus structureLocus;
   private final boolean usesNativeTexture;

   GeneticsLayer(int renderOrder, String texturePrefix, Locus colorLocus, Locus structureLocus, boolean usesNativeTexture) {
      this.renderOrder = renderOrder;
      this.texturePrefix = texturePrefix;
      this.colorLocus = colorLocus;
      this.structureLocus = structureLocus;
      this.usesNativeTexture = usesNativeTexture;
   }

   public String getTexturePrefix() {
      return this.texturePrefix;
   }

   public Locus getColorLocus() {
      return this.colorLocus;
   }

   public Locus getStructureLocus() {
      return this.structureLocus;
   }

   public boolean hasStructureVariants() {
      return this.structureLocus != null;
   }

   public boolean usesNativeTexture() {
      return this.usesNativeTexture;
   }

   public static GeneticsLayer[] getLayersInRenderOrder() {
      GeneticsLayer[] layers = values();
      Arrays.sort(layers, (a, b) -> Integer.compare(a.renderOrder, b.renderOrder));
      return layers;
   }
}
