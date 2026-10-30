package net.magister.liberanimalium.genetics;

import java.util.Objects;

public final class Allele {
   private final String id;
   private final int dominance;

   private Allele(String id, int dominance) {
      this.id = Objects.requireNonNull(id, "Allele ID cannot be null");
      this.dominance = dominance;
   }

   public static Allele of(String id, int dominance) {
      return new Allele(id, dominance);
   }

   public static Allele of(String id) {
      int dominance = 1;
      if (id.length() > 0) {
         int numStart = id.length();

         while (numStart > 0 && Character.isDigit(id.charAt(numStart - 1))) {
            numStart--;
         }

         if (numStart < id.length()) {
            try {
               dominance = Integer.parseInt(id.substring(numStart));
               if (dominance <= 0) {
                  dominance = 1;
               }
            } catch (NumberFormatException e) {
               dominance = 1;
            }
         }
      }

      return new Allele(id, dominance);
   }

   public String getId() {
      return this.id;
   }

   public int getDominance() {
      return this.dominance;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof Allele allele) ? false : this.id.equals(allele.id);
      }
   }

   @Override
   public int hashCode() {
      return this.id.hashCode();
   }

   @Override
   public String toString() {
      return this.id;
   }
}
