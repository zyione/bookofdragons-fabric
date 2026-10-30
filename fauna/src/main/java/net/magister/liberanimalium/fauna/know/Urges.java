package net.magister.liberanimalium.fauna.know;

import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.api.Relation;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.api.Urged;
import net.magister.liberanimalium.fauna.core.PairSession;
import net.magister.liberanimalium.fauna.debug.UrgeProbe;
import net.magister.liberanimalium.fauna.sense.Perception;

final class Urges {
   private Urges() {
   }

   static void integrate(Knowledge knowledge, AnimalHandle handle, Perception perception) {
      SpeciesProfile species = handle.species();

      for (Urged urge : perception.drainUrges()) {
         if (Halter.of(handle).ownedBy(urge.ownerId()) && !urge.targetId().equals(handle.faunaId()) && !urge.targetId().equals(urge.ownerId())) {
            float word = knowledge.obedience().share();
            if (urge.calm()) {
               Representation rep = knowledge.known.get(urge.targetId());
               if (rep != null) {
                  Relation before = rep.relation();
                  knowledge.callOff(handle, rep, word);
                  probe(handle, urge, word, before, rep.relation());
               } else {
                  PairSession.noteSettled(handle.faunaId(), urge.targetId(), "territory_contest", handle.gameTime());
                  PairSession.close(handle.faunaId(), urge.targetId());
               }
            } else if (word <= 0.0F) {
               probe(handle, urge, word, Relation.NONE, Relation.NONE);
            } else {
               Representation rep = knowledge.known
                  .computeIfAbsent(
                     urge.targetId(),
                     id -> knowledge.meet(
                        id, urge.targetKind().typeKey(), urge.targetKind().sameSpecies(), species.relationTo(urge.targetKind()), urge.position(), urge.tick()
                     )
                  );
               Relation before = rep.relation();
               knowledge.bend(rep, Bend.urgedOn(rep.relation(), word, urge.targetKind().sameSpecies()));
               probe(handle, urge, word, before, rep.relation());
            }
         }
      }
   }

   private static void probe(AnimalHandle handle, Urged urge, float word, Relation before, Relation after) {
      if (UrgeProbe.listening()) {
         UrgeProbe.urged(new UrgeProbe.Urge(handle.faunaId(), urge.ownerId(), urge.targetId(), urge.calm(), word, before, after, handle.gameTime()));
      }
   }
}
