package net.magister.bookofdragons.entity.genetics.data;

import java.util.Base64;

public final class GeneticsSequenceData {
   private static final String A = "pzGNPWY4vZLNT5MdWUD9MCI0nsFAVNGRXgQvAQ9F+u4=";

   public static byte[] seed() {
      return Base64.getDecoder().decode("pzGNPWY4vZLNT5MdWUD9MCI0nsFAVNGRXgQvAQ9F+u4=");
   }
}
