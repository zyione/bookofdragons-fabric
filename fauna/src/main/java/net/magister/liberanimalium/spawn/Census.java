package net.magister.liberanimalium.spawn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;

public final class Census {
   private final List<Census.Entry> entries;

   public Census(List<Census.Entry> entries) {
      this.entries = new ArrayList<>(entries);
   }

   public void add(Census.Entry entry) {
      this.entries.add(entry);
   }

   public static Census of(ServerLevel level) {
      Collection<String> hosts = SpawnRules.hosts();
      List<Census.Entry> entries = new ArrayList<>();

      for (Entity entity : level.getAllEntities()) {
         if (entity instanceof Mob mob && mob.isAlive()) {
            boolean fauna = mob instanceof FaunaBase;
            String host = SpeciesAssets.hostOf(mob);
            if (fauna || hosts.contains(host)) {
               entries.add(entryOf(mob, host));
            }
         }
      }

      return new Census(entries);
   }

   static Census.Entry entryOf(Mob mob, String host) {
      boolean tame = mob instanceof TamableAnimal t && t.isTame();
      boolean roosted = mob instanceof FaunaBase creature && creature.holdsRoost();
      String variant = mob instanceof FaunaBase creature && creature.variantId() != null ? creature.variantId().toString() : null;
      return new Census.Entry(host, BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(), !tame, roosted, mob.position(), mob.tickCount, variant);
   }

   public int roostless(String host) {
      int n = 0;

      for (Census.Entry e : this.entries) {
         if (e.host().equals(host) && e.wild() && !e.roosted()) {
            n++;
         }
      }

      return n;
   }

   public int roostlessOfType(String type) {
      int n = 0;

      for (Census.Entry e : this.entries) {
         if (e.type().equals(type) && e.wild() && !e.roosted()) {
            n++;
         }
      }

      return n;
   }

   public int roostlessOf(SpawnRule rule) {
      int n = 0;

      for (Census.Entry e : this.entries) {
         if (e.countsFor(rule) && e.wild() && !e.roosted()) {
            n++;
         }
      }

      return n;
   }

   public int nearOf(SpawnRule rule, Vec3 at, double radius) {
      double r2 = radius * radius;
      int n = 0;

      for (Census.Entry e : this.entries) {
         if (e.countsFor(rule) && e.position().distanceToSqr(at) <= r2) {
            n++;
         }
      }

      return n;
   }

   public int near(String host, Vec3 at, double radius) {
      double r2 = radius * radius;
      int n = 0;

      for (Census.Entry e : this.entries) {
         if (e.host().equals(host) && e.position().distanceToSqr(at) <= r2) {
            n++;
         }
      }

      return n;
   }

   public int nearOfType(String type, Vec3 at, double radius) {
      double r2 = radius * radius;
      int n = 0;

      for (Census.Entry e : this.entries) {
         if (e.type().equals(type) && e.position().distanceToSqr(at) <= r2) {
            n++;
         }
      }

      return n;
   }

   public List<Census.Entry> beacons(String type, long every, long period) {
      List<Census.Entry> out = new ArrayList<>();

      for (Census.Entry e : this.entries) {
         if (e.type().equals(type) && e.wild() && e.age() >= every && e.age() % every < period) {
            out.add(e);
         }
      }

      return out;
   }

   public Map<String, Census.Counts> byType(String host) {
      Map<String, Census.Counts> out = new TreeMap<>();

      for (Census.Entry e : this.entries) {
         if (e.host().equals(host)) {
            out.put(e.type(), out.getOrDefault(e.type(), new Census.Counts(0, 0, 0)).plus(e));
         }
      }

      return out;
   }

   public List<String> hosts() {
      return this.entries.stream().map(Census.Entry::host).distinct().sorted().toList();
   }

   public record Counts(int wildRoostless, int wildRoosted, int tame) {
      Census.Counts plus(Census.Entry e) {
         if (!e.wild()) {
            return new Census.Counts(this.wildRoostless, this.wildRoosted, this.tame + 1);
         } else {
            return e.roosted()
               ? new Census.Counts(this.wildRoostless, this.wildRoosted + 1, this.tame)
               : new Census.Counts(this.wildRoostless + 1, this.wildRoosted, this.tame);
         }
      }
   }

   public record Entry(String host, String type, boolean wild, boolean roosted, Vec3 position, long age, @Nullable String variant) {
      public Entry(String host, String type, boolean wild, boolean roosted, Vec3 position, long age) {
         this(host, type, wild, roosted, position, age, null);
      }

      boolean countsFor(SpawnRule rule) {
         return !this.type.equals(rule.typeId()) ? false : rule.variant() == null || rule.variant().toString().equals(this.variant);
      }
   }
}
