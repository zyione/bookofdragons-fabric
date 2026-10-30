package net.magister.liberanimalium.genetics;

import java.util.HashMap;
import java.util.Map;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfigRegistry;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public class GeneticsNBT {
   private static final String KEY_SPECIES = "Species";
   private static final String KEY_GENOTYPE = "Genotype";
   private static final String KEY_LAYOUT = "Layout";
   private static final Map<String, Byte> ALLELE_TO_ID = new HashMap<>();
   private static final Map<Byte, String> ID_TO_ALLELE = new HashMap<>();

   public static CompoundTag save(Genotype genotype, String speciesId, PaletteDraws paletteDraws) {
      CompoundTag tag = new CompoundTag();
      tag.putString("Species", speciesId);
      tag.putByteArray("Genotype", pack(genotype));
      tag.putIntArray("Layout", SpeciesGeneticsConfigRegistry.getConfig(speciesId).getExpressionCounts());
      if (paletteDraws != null) {
         paletteDraws.saveTo(tag);
      }

      return tag;
   }

   public static CompoundTag save(Genotype genotype, String speciesId) {
      return save(genotype, speciesId, null);
   }

   public static Genotype load(CompoundTag tag, SpeciesGeneticsConfig config) {
      if (!tag.contains("Genotype")) {
         throw new IllegalStateException("NBT missing genotype data");
      } else if (tag.getTagType("Genotype") == 7) {
         return unpack(tag.getByteArray("Genotype"), config);
      } else {
         throw new IllegalStateException("Unknown Genotype NBT array format, expected ByteArrayTag");
      }
   }

   public static String getSpeciesId(CompoundTag tag) {
      return tag.getString("Species");
   }

   @Nullable
   public static int[] layout(CompoundTag tag) {
      if (!tag.contains("Layout", 11)) {
         return null;
      }

      int[] layout = tag.getIntArray("Layout");
      return layout.length == Locus.GENOTYPE_ORDER.length ? layout : null;
   }

   public static byte[] bytes(CompoundTag tag) {
      return tag.getByteArray("Genotype");
   }

   public static Genotype unpackAll(byte[] packed) {
      Genotype.Builder builder = Genotype.builder();

      for (int i = 0; i + 1 < packed.length; i += 2) {
         builder.addPair(ID_TO_ALLELE.getOrDefault(packed[i], "b1"), ID_TO_ALLELE.getOrDefault(packed[i + 1], "b1"));
      }

      return builder.build();
   }

   public static byte[] pack(Genotype genotype) {
      int alleleCount = genotype.getAlleleCount();
      byte[] packed = new byte[alleleCount];

      for (int i = 0; i < alleleCount; i++) {
         Allele a = genotype.getAllele(i);
         Byte id = ALLELE_TO_ID.get(a.getId());
         packed[i] = id != null ? id : 0;
      }

      return packed;
   }

   public static Genotype unpack(byte[] packed, SpeciesGeneticsConfig config) {
      int totalAlleles = config.getTotalAlleleCount();
      Genotype.Builder builder = Genotype.builder();

      for (int i = 0; i < packed.length && i < totalAlleles; i += 2) {
         byte id1 = packed[i];
         byte id2 = i + 1 < packed.length ? packed[i + 1] : 0;
         String s1 = ID_TO_ALLELE.getOrDefault(id1, "b1");
         String s2 = ID_TO_ALLELE.getOrDefault(id2, "b1");
         builder.addPair(s1, s2);
      }

      return builder.build();
   }

   public static String generateCacheKey(Genotype genotype, String speciesId, PaletteDraws paletteDraws) {
      byte[] packed = pack(genotype);
      StringBuilder sb = new StringBuilder(speciesId);
      sb.append("_");

      for (byte b : packed) {
         sb.append(String.format("%02x", b & 255));
      }

      if (paletteDraws != null && !paletteDraws.isEmpty()) {
         String pdString = paletteDraws.toCacheString();
         sb.append("_pd").append(Integer.toHexString(Math.abs(pdString.hashCode())));
      }

      return sb.toString();
   }

   static {
      byte currentId = 1;

      for (Locus locus : Locus.values()) {
         for (Allele allele : locus.getDefaultAlleles()) {
            byte allocated = currentId++;
            ALLELE_TO_ID.put(allele.getId(), allocated);
            ID_TO_ALLELE.put(allocated, allele.getId());
         }
      }
   }
}
