package net.magister.liberanimalium.render.layer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LayerAudience {
   private final List<Set<String>> sets = new ArrayList<>();
   private final List<Integer> active = new ArrayList<>();
   private final Set<String> union = new HashSet<>();
   private boolean complete;

   public Set<String> build(int layers, LayerAudience.Declarer declarer) {
      while (this.sets.size() < layers) {
         this.sets.add(new HashSet<>());
      }

      this.active.clear();
      this.union.clear();
      this.complete = true;

      for (int i = 0; i < layers; i++) {
         Set<String> mine = this.sets.get(i);
         mine.clear();
         if (!declarer.declare(i, mine)) {
            this.active.clear();
            this.union.clear();
            this.complete = false;
            return null;
         }

         if (!mine.isEmpty()) {
            this.active.add(i);
            this.union.addAll(mine);
         }
      }

      return this.union;
   }

   public boolean complete() {
      return this.complete;
   }

   public int activeCount() {
      return this.active.size();
   }

   public int activeLayer(int n) {
      return this.active.get(n);
   }

   public boolean wants(int n, String bone) {
      return this.sets.get(this.active.get(n)).contains(bone);
   }

   public Set<String> declared(int layer) {
      return layer < this.sets.size() ? this.sets.get(layer) : Set.of();
   }

   public interface Declarer {
      boolean declare(int var1, Set<String> var2);
   }
}
