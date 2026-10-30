package net.magister.liberanimalium.body.anim.binding;

import java.util.Map;
import net.magister.liberanimalium.anim.event.EventTable;
import net.magister.liberanimalium.anim.mask.BoneMask;
import net.magister.liberanimalium.anim.rig.RigSpec;
import net.magister.liberanimalium.anim.rig.StepsSpec;
import net.magister.liberanimalium.anim.v2.BindingsV2;
import net.magister.liberanimalium.anim.v2.LayerSpec;

public final class AnimationBindingTable {
   public final String species;
   public final StepsSpec steps;
   public final EventTable events;
   public final BindingsV2 v2;
   public final Map<String, BoneMask> boneGroups;
   public final RigSpec rig;

   public AnimationBindingTable(String species, StepsSpec steps, EventTable events, BindingsV2 v2, Map<String, BoneMask> boneGroups, RigSpec rig) {
      this.species = species;
      this.steps = steps == null ? StepsSpec.EMPTY : steps;
      this.events = events == null ? EventTable.EMPTY : events;
      this.v2 = v2 == null ? BindingsV2.EMPTY : v2;
      this.boneGroups = boneGroups == null ? Map.of() : boneGroups;
      this.rig = rig == null ? RigSpec.EMPTY : rig;
   }

   public BoneMask maskFor(String layer) {
      for (LayerSpec spec : this.v2.layers()) {
         if (spec.name().equals(layer) && spec.mask() != null) {
            return this.boneGroups.getOrDefault(spec.mask(), BoneMask.ALL);
         }
      }

      return BoneMask.ALL;
   }

   public AnimationBindingTable(String species, StepsSpec steps, EventTable events) {
      this(species, steps, events, BindingsV2.EMPTY, Map.of(), RigSpec.EMPTY);
   }

   public AnimationBindingTable(String species, StepsSpec steps) {
      this(species, steps, EventTable.EMPTY);
   }

   public String speciesPrefix() {
      return "animation." + this.species + ".";
   }
}
