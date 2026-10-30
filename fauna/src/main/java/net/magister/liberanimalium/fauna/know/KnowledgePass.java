package net.magister.liberanimalium.fauna.know;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.HeardSound;
import net.magister.liberanimalium.fauna.api.SensedItem;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.identity.Temper;
import net.minecraft.world.phys.Vec3;

final class KnowledgePass implements FacultyContext {
   private final Knowledge knowledge;
   final AnimalHandle handle;
   final Temper temper;
   final long now;
   final SpeciesProfile species;
   final List<HeardSound> mysteries = new ArrayList<>();
   final List<Record> stimuli = new ArrayList<>();
   final List<SensedItem> items;
   final double reach;
   final float space;

   KnowledgePass(Knowledge knowledge, AnimalHandle handle, Temper temper, long now, SpeciesProfile species, List<SensedItem> items, double reach, float space) {
      this.knowledge = knowledge;
      this.handle = handle;
      this.temper = temper;
      this.now = now;
      this.species = species;
      this.items = items;
      this.reach = reach;
      this.space = space;
   }

   @Override
   public AnimalHandle handle() {
      return this.handle;
   }

   @Override
   public Temper temper() {
      return this.temper;
   }

   @Override
   public long now() {
      return this.now;
   }

   @Override
   public SpeciesProfile species() {
      return this.species;
   }

   @Override
   public List<HeardSound> mysteries() {
      return this.mysteries;
   }

   @Override
   public <T extends Record> List<T> stimuli(Class<T> type) {
      if (this.stimuli.isEmpty()) {
         return List.of();
      }

      List<T> out = new ArrayList<>();

      for (Record stimulus : this.stimuli) {
         if (type.isInstance(stimulus)) {
            out.add(type.cast(stimulus));
         }
      }

      return out;
   }

   @Override
   public List<SensedItem> items() {
      return this.items;
   }

   @Override
   public Collection<Representation> known() {
      return this.knowledge.all();
   }

   @Override
   public Vec3 estimate(Representation rep) {
      return this.knowledge.estimate(rep, this.now, this.species);
   }

   @Override
   public double reach() {
      return this.reach;
   }

   @Override
   public float space() {
      return this.space;
   }

   @Override
   public <T extends Faculty> Optional<T> faculty(Class<T> type) {
      return this.knowledge.faculty(type);
   }
}
