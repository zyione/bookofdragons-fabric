package net.magister.liberanimalium.fauna.nature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.behavior.Behavior;
import net.magister.liberanimalium.fauna.drive.Drive;
import net.magister.liberanimalium.fauna.know.Faculty;

public final class Nature {
   private final String name;
   private final List<String> ids;
   private final List<Supplier<Pursuit>> recipe;
   private final List<Set<Class<?>>> needs;
   private final List<String> facultyIds;
   private final List<Supplier<? extends Faculty>> facultyRecipe;

   private Nature(
      String name,
      List<String> ids,
      List<Supplier<Pursuit>> recipe,
      List<Set<Class<?>>> needs,
      List<String> facultyIds,
      List<Supplier<? extends Faculty>> facultyRecipe
   ) {
      this.name = name;
      this.ids = List.copyOf(ids);
      this.recipe = List.copyOf(recipe);
      this.needs = List.copyOf(needs);
      this.facultyIds = List.copyOf(facultyIds);
      this.facultyRecipe = List.copyOf(facultyRecipe);
   }

   public static Nature.Builder builder(String name) {
      return new Nature.Builder(name);
   }

   public String name() {
      return this.name;
   }

   public List<String> ids() {
      return this.ids;
   }

   public List<Pursuit> pursuits() {
      List<Pursuit> out = new ArrayList<>(this.recipe.size());

      for (Supplier<Pursuit> make : this.recipe) {
         out.add(make.get());
      }

      return List.copyOf(out);
   }

   public List<String> facultyIds() {
      return this.facultyIds;
   }

   public List<Faculty> faculties() {
      List<Faculty> out = new ArrayList<>(this.facultyRecipe.size());

      for (Supplier<? extends Faculty> make : this.facultyRecipe) {
         out.add(make.get());
      }

      return List.copyOf(out);
   }

   @Nullable
   Supplier<Pursuit> recipeOf(String id) {
      int i = this.ids.indexOf(id);
      return i < 0 ? null : this.recipe.get(i);
   }

   Set<Class<?>> needsOf(String id) {
      return this.needs.get(this.ids.indexOf(id));
   }

   @Nullable
   Supplier<? extends Faculty> facultyRecipeOf(String id) {
      int i = this.facultyIds.indexOf(id);
      return i < 0 ? null : this.facultyRecipe.get(i);
   }

   public Nature fitTo(AnimalHandle handle, Consumer<String> report) {
      List<String> keptIds = new ArrayList<>();
      List<Supplier<Pursuit>> keptRecipe = new ArrayList<>();
      List<Set<Class<?>>> keptNeeds = new ArrayList<>();

      for (int i = 0; i < this.ids.size(); i++) {
         Class<?> missing = null;

         for (Class<?> endowment : this.needs.get(i)) {
            if (handle.endowed(endowment).isEmpty()) {
               missing = endowment;
               break;
            }
         }

         if (missing != null) {
            report.accept(this.ids.get(i) + " omitted: this body has no " + missing.getSimpleName());
         } else {
            keptIds.add(this.ids.get(i));
            keptRecipe.add(this.recipe.get(i));
            keptNeeds.add(this.needs.get(i));
         }
      }

      return keptIds.size() == this.ids.size() ? this : new Nature(this.name, keptIds, keptRecipe, keptNeeds, this.facultyIds, this.facultyRecipe);
   }

   public Nature without(String derivedName, String... dropIds) {
      Set<String> drop = Set.of(dropIds);
      List<String> unknown = new ArrayList<>(drop);
      unknown.removeAll(this.ids);
      if (!unknown.isEmpty()) {
         throw new IllegalArgumentException("nature '" + this.name + "' composes no " + unknown + " to drop; it has " + this.ids);
      }

      List<String> keptIds = new ArrayList<>();
      List<Supplier<Pursuit>> keptRecipe = new ArrayList<>();
      List<Set<Class<?>>> keptNeeds = new ArrayList<>();

      for (int i = 0; i < this.ids.size(); i++) {
         if (!drop.contains(this.ids.get(i))) {
            keptIds.add(this.ids.get(i));
            keptRecipe.add(this.recipe.get(i));
            keptNeeds.add(this.needs.get(i));
         }
      }

      return new Nature(derivedName, keptIds, keptRecipe, keptNeeds, this.facultyIds, this.facultyRecipe);
   }

   public Nature.Builder extend(String derivedName) {
      return new Nature.Builder(derivedName, this);
   }

   @Override
   public String toString() {
      return "Nature[" + this.name + " " + this.ids + " " + this.facultyIds + "]";
   }

   public static final class Builder {
      private final String name;
      private final List<String> ids = new ArrayList<>();
      private final List<Supplier<Pursuit>> recipe = new ArrayList<>();
      private final List<Set<Class<?>>> needs = new ArrayList<>();
      private final List<String> facultyIds = new ArrayList<>();
      private final List<Supplier<? extends Faculty>> facultyRecipe = new ArrayList<>();

      private Builder(String name) {
         this.name = name;
      }

      private Builder(String name, Nature base) {
         this.name = name;
         this.ids.addAll(base.ids);
         this.recipe.addAll(base.recipe);
         this.needs.addAll(base.needs);
         this.facultyIds.addAll(base.facultyIds);
         this.facultyRecipe.addAll(base.facultyRecipe);
      }

      public <B extends Behavior> Nature.Builder pursue(
         Supplier<? extends Drive> drive, Supplier<B> behavior, Function<? super B, ? extends Handoff> handoff, Class<?>... endowments
      ) {
         String id = drive.get().id();
         if (this.ids.contains(id)) {
            throw new IllegalArgumentException("nature '" + this.name + "' already pursues '" + id + "'");
         }

         Set<Class<?>> needed = Collections.unmodifiableSet(new LinkedHashSet<>(List.of(endowments)));
         this.ids.add(id);
         this.needs.add(needed);
         this.recipe.add(() -> {
            B leaf = behavior.get();
            return new Pursuit(drive.get(), leaf, handoff.apply(leaf), needed);
         });
         return this;
      }

      Nature.Builder pursue(String id, Supplier<Pursuit> pursuitRecipe, Set<Class<?>> needed) {
         if (this.ids.contains(id)) {
            throw new IllegalArgumentException("nature '" + this.name + "' already pursues '" + id + "'");
         }

         this.ids.add(id);
         this.needs.add(needed);
         this.recipe.add(pursuitRecipe);
         return this;
      }

      Nature.Builder facultyIfAbsent(String id, Supplier<? extends Faculty> faculty) {
         if (this.facultyIds.contains(id)) {
            return this;
         }

         this.facultyIds.add(id);
         this.facultyRecipe.add(faculty);
         return this;
      }

      public Nature.Builder faculty(Supplier<? extends Faculty> faculty) {
         String id = faculty.get().id();
         if (this.facultyIds.contains(id)) {
            throw new IllegalArgumentException("nature '" + this.name + "' already keeps '" + id + "'");
         }

         this.facultyIds.add(id);
         this.facultyRecipe.add(faculty);
         return this;
      }

      public Nature build() {
         return new Nature(this.name, this.ids, this.recipe, this.needs, this.facultyIds, this.facultyRecipe);
      }
   }
}
