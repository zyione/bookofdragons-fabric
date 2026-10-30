package net.magister.liberanimalium.genetics;

public enum Locus {
   b("Base Color", Locus.LocusCategory.COLOR),
   r("Dorsal Color", Locus.LocusCategory.COLOR),
   y("Underbelly Color", Locus.LocusCategory.COLOR),
   n("Gradient Color", Locus.LocusCategory.COLOR),
   e("Eye Color", Locus.LocusCategory.COLOR),
   g("Glow Color", Locus.LocusCategory.COLOR),
   j("Body Marking Color", Locus.LocusCategory.COLOR),
   a("Overwing Color", Locus.LocusCategory.COLOR),
   i("Underwing Color", Locus.LocusCategory.COLOR),
   z("Face/Limb Color", Locus.LocusCategory.COLOR),
   v("Wing Gradient Color", Locus.LocusCategory.COLOR),
   m("Tier Modifier", Locus.LocusCategory.MODIFIER),
   d("Dorsal Structure", Locus.LocusCategory.STRUCTURE),
   u("Underbelly Structure", Locus.LocusCategory.STRUCTURE),
   p("Body Pattern", Locus.LocusCategory.STRUCTURE),
   w("Wing Pattern", Locus.LocusCategory.STRUCTURE),
   x("Underwing Pattern", Locus.LocusCategory.STRUCTURE),
   f("Face Pattern", Locus.LocusCategory.STRUCTURE),
   l("Limb Pattern", Locus.LocusCategory.STRUCTURE),
   s("Eye Structure", Locus.LocusCategory.STRUCTURE),
   h("Gradient Structure", Locus.LocusCategory.STRUCTURE),
   k("Wing Gradient Pattern", Locus.LocusCategory.STRUCTURE),
   CON("Constitution", Locus.LocusCategory.ATTRIBUTE),
   ATH("Athleticism", Locus.LocusCategory.ATTRIBUTE),
   AGI("Agility", Locus.LocusCategory.ATTRIBUTE),
   PRW("Prowess", Locus.LocusCategory.ATTRIBUTE),
   POT("Potency", Locus.LocusCategory.ATTRIBUTE),
   VIG("Vigor", Locus.LocusCategory.ATTRIBUTE),
   c("Membrane Color", Locus.LocusCategory.COLOR),
   q("Membrane Structure", Locus.LocusCategory.STRUCTURE);

   private final String displayName;
   private final Locus.LocusCategory category;
   public static final Locus[] GENOTYPE_ORDER = new Locus[]{b, r, y, n, e, g, j, a, i, z, v, c, d, u, p, w, x, f, l, s, h, k, q, CON, ATH, AGI, PRW, POT, VIG};

   Locus(String displayName, Locus.LocusCategory category) {
      this.displayName = displayName;
      this.category = category;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public Locus.LocusCategory getCategory() {
      return this.category;
   }

   public Allele[] getDefaultAlleles() {
      switch (this.category) {
         case COLOR:
            return new Allele[]{Allele.of(this.name() + "1", 1), Allele.of(this.name() + "2", 2)};
         case MODIFIER:
            return new Allele[]{Allele.of("m1", 1), Allele.of("m2", 2), Allele.of("m3", 3), Allele.of("m4", 4)};
         case ATTRIBUTE:
            Allele[] attrAlleles = new Allele[10];

            for (int i = 0; i < 10; i++) {
               attrAlleles[i] = Allele.of(this.name() + (i + 1), i + 1);
            }

            return attrAlleles;
         case STRUCTURE:
            return new Allele[]{Allele.of(this.name() + "1", 1), Allele.of(this.name() + "2", 2)};
         default:
            throw new IllegalStateException("Unhandled locus category: " + this.category);
      }
   }

   public enum LocusCategory {
      COLOR,
      MODIFIER,
      STRUCTURE,
      ATTRIBUTE;
   }
}
