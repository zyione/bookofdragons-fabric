package net.magister.liberanimalium.anim.validate;

import java.util.ArrayList;
import java.util.List;

public record BindingReport(String species, List<String> problems, int rowsChecked) {
   public static BindingReport clean(String species, int rowsChecked) {
      return new BindingReport(species, List.of(), rowsChecked);
   }

   public boolean isClean() {
      return this.problems.isEmpty();
   }

   public static final class Builder {
      private final String species;
      private final List<String> problems = new ArrayList<>();
      private int rowsChecked;

      public Builder(String species) {
         this.species = species;
      }

      public void problem(String message) {
         this.problems.add(message);
      }

      public void checked() {
         this.rowsChecked++;
      }

      public BindingReport build() {
         return new BindingReport(this.species, List.copyOf(this.problems), this.rowsChecked);
      }
   }
}
