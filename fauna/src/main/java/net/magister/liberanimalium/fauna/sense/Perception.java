package net.magister.liberanimalium.fauna.sense;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Buck;
import net.magister.liberanimalium.fauna.api.HeardSound;
import net.magister.liberanimalium.fauna.api.Hurt;
import net.magister.liberanimalium.fauna.api.Kindness;
import net.magister.liberanimalium.fauna.api.SensedBody;
import net.magister.liberanimalium.fauna.api.SensedItem;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.api.Touch;
import net.magister.liberanimalium.fauna.api.Urged;
import net.magister.liberanimalium.fauna.api.Witness;
import net.magister.liberanimalium.fauna.debug.FaunaDebug;
import net.magister.liberanimalium.fauna.debug.StimulusLog;
import net.minecraft.world.phys.Vec3;

public final class Perception {
   private List<Perception.Seen> visible = List.of();
   private List<SensedItem> items = List.of();
   private long scans;
   private final Deque<HeardSound> sounds = new ArrayDeque<>();
   private final Deque<Hurt> hurts = new ArrayDeque<>();
   private final Deque<Kindness> kindnesses = new ArrayDeque<>();
   private final Deque<Touch> touches = new ArrayDeque<>();
   private final Deque<Buck> bucks = new ArrayDeque<>();
   private final Deque<Witness> witnesses = new ArrayDeque<>();
   private final Deque<Urged> urges = new ArrayDeque<>();
   private final Deque<Record> posted = new ArrayDeque<>();

   public long scanSerial() {
      return this.scans;
   }

   public void scan(AnimalHandle handle) {
      SpeciesProfile.Senses senses = handle.senses();
      Vec3 eye = handle.eyePosition();
      Vec3 facing = handle.sightDirection();
      List<SensedBody> candidates = handle.scanNearby(senses.sightRadius());
      List<Perception.Seen> found = new ArrayList<>(candidates.size());

      for (SensedBody body : candidates) {
         float score = SightSense.score(senses, eye, facing, body);
         if (score > 0.0F) {
            found.add(new Perception.Seen(body, score));
         }
      }

      this.visible = List.copyOf(found);
      this.items = List.copyOf(handle.scanItems(senses.sightRadius()));
      this.scans++;
   }

   public List<SensedItem> items() {
      return this.items;
   }

   public List<Perception.Seen> visible() {
      return this.visible;
   }

   public void forget(UUID id) {
      if (!this.visible.isEmpty()) {
         List<Perception.Seen> kept = new ArrayList<>(this.visible.size());

         for (Perception.Seen seen : this.visible) {
            if (!seen.body().id().equals(id)) {
               kept.add(seen);
            }
         }

         this.visible = List.copyOf(kept);
      }
   }

   public boolean deliver(AnimalHandle handle, HeardSound sound) {
      double ownSpeed = handle.velocity().length();
      if (!HearingSense.hears(handle.species().senses(), ownSpeed, sound)) {
         return false;
      }

      push(this.sounds, sound);
      if (FaunaDebug.anyoneWatching()) {
         StimulusLog.note(handle.faunaId(), sound, this.strengthOf(handle, sound));
      }

      return true;
   }

   public void deliver(AnimalHandle handle, Hurt hurt) {
      push(this.hurts, hurt);
      if (FaunaDebug.anyoneWatching()) {
         StimulusLog.note(handle.faunaId(), hurt, TouchSense.severity(hurt, handle.maxHealth()));
      }
   }

   public List<HeardSound> drainSounds() {
      return drain(this.sounds);
   }

   public List<Hurt> drainHurts() {
      return drain(this.hurts);
   }

   public void deliver(AnimalHandle handle, Kindness kindness) {
      push(this.kindnesses, kindness);
   }

   public boolean deliver(AnimalHandle handle, Witness witness) {
      if (handle.headClear() && handle.sightLine(handle.eyePosition(), witness.position())) {
         double range = handle.species().senses().sightRadius();
         if (handle.bodyPosition().distanceToSqr(witness.position()) > range * range) {
            return false;
         }

         push(this.witnesses, witness);
         return true;
      } else {
         return false;
      }
   }

   public List<Witness> drainWitnesses() {
      return drain(this.witnesses);
   }

   public List<Kindness> drainKindnesses() {
      return drain(this.kindnesses);
   }

   public void deliver(AnimalHandle handle, Touch touch) {
      push(this.touches, touch);
   }

   public List<Touch> drainTouches() {
      return drain(this.touches);
   }

   public void deliver(AnimalHandle handle, Buck buck) {
      push(this.bucks, buck);
   }

   public List<Buck> drainBucks() {
      return drain(this.bucks);
   }

   public void deliver(AnimalHandle handle, Urged urged) {
      push(this.urges, urged);
   }

   public List<Urged> drainUrges() {
      return drain(this.urges);
   }

   public void post(AnimalHandle handle, Record stimulus) {
      if (!(stimulus instanceof HeardSound)
         && !(stimulus instanceof Hurt)
         && !(stimulus instanceof Kindness)
         && !(stimulus instanceof Touch)
         && !(stimulus instanceof Buck)
         && !(stimulus instanceof Witness)
         && !(stimulus instanceof Urged)) {
         push(this.posted, stimulus);
      } else {
         throw new IllegalArgumentException(stimulus.getClass().getSimpleName() + " has its own door; the core reads it");
      }
   }

   public List<Record> drainPosted() {
      return drain(this.posted);
   }

   public float strengthOf(AnimalHandle handle, HeardSound sound) {
      return HearingSense.strength(handle.species().senses(), handle.velocity().length(), sound);
   }

   private static <T> void push(Deque<T> queue, T value) {
      if (queue.size() >= 16) {
         queue.pollFirst();
      }

      queue.addLast(value);
   }

   private static <T> List<T> drain(Deque<T> queue) {
      if (queue.isEmpty()) {
         return List.of();
      }

      List<T> out = new ArrayList<>(queue);
      queue.clear();
      return out;
   }

   public record Seen(SensedBody body, float score) {
   }
}
