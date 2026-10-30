package net.magister.liberanimalium.fauna.know;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.api.ShelterProbe;
import net.magister.liberanimalium.fauna.api.Terrain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Bedding implements Faculty {
   @Nullable
   private Bed bed;
   private long lastGroundProbe = -4611686018427387904L;
   @Nullable
   private BlockPos bedRoost;
   private long lastBedSearch = -4611686018427387904L;
   private static final long NEVER = -4611686018427387904L;

   @Override
   public String id() {
      return "bedding";
   }

   @Nullable
   public Bed bed() {
      return this.bed;
   }

   @Override
   public void integrate(FacultyContext ctx) {
      AnimalHandle handle = ctx.handle();
      long now = ctx.now();
      Optional<Halter.Roost> roost = Halter.of(handle).roost();
      if (!roost.isPresent()) {
         ctx.faculty(Territory.class).ifPresent(t -> t.setRoostAnchor(null));
         if (this.bed != null && this.bed.isRoost()) {
            this.bed = null;
            this.bedRoost = null;
         }

         if (now - this.lastGroundProbe >= 100L) {
            this.lastGroundProbe = now;
            BlockPos here = BlockPos.containing(handle.bodyPosition());
            Terrain terrain = terrain(ctx);
            if (ShelterProbe.roofed(terrain, here)) {
               ShelterProbe.Reading reading = ShelterProbe.score(terrain, bodyOf(handle), Vec3.atBottomCenterOf(here));
               this.remember(here, reading, Bed.Source.FOUND, now);
            }
         }
      } else {
         BlockPos controller = roost.get().controller();
         ctx.faculty(Territory.class).ifPresent(t -> t.setRoostAnchor(controller));
         if (this.bed == null || !this.bed.isRoost() || !controller.equals(this.bedRoost)) {
            Bed home = this.probeRoost(ctx, roost.get());
            if (home != null) {
               this.bed = home;
               this.bedRoost = controller;
            }
         }
      }
   }

   @Nullable
   public Bed searchBed(FacultyContext ctx) {
      AnimalHandle handle = ctx.handle();
      long now = ctx.now();
      if (now - this.lastBedSearch < 400L) {
         return this.bed;
      }

      this.lastBedSearch = now;
      Terrain terrain = terrain(ctx);
      ShelterProbe.Body body = bodyOf(handle);
      double radius = searchRadius(handle);
      Vec3 origin = Packmates.centroid(ctx.known(), ctx::estimate, id -> false);
      if (origin == null) {
         origin = ctx.faculty(Territory.class).map(Territory::homeCenter).orElse(null);
      }

      if (origin == null) {
         origin = handle.bodyPosition();
      }

      BlockPos here = BlockPos.containing(handle.bodyPosition());
      if (ShelterProbe.roofed(terrain, here)) {
         this.remember(here, ShelterProbe.score(terrain, body, Vec3.atBottomCenterOf(here)), Bed.Source.FOUND, now);
      }

      List<BlockPos> roofed = new ArrayList<>();
      int span = (int)Math.ceil(radius);
      double radiusSq = radius * radius;
      BlockPos centre = BlockPos.containing(origin);

      for (int dx = -span; dx <= span; dx += 2) {
         for (int dz = -span; dz <= span; dz += 2) {
            if (!(dx * dx + dz * dz > radiusSq)) {
               BlockPos raw = centre.offset(dx, 0, dz);
               if (ShelterProbe.roofed(terrain, raw)) {
                  roofed.add(raw);
               }
            }
         }
      }

      int stride = Math.max(1, roofed.size() / 24);

      for (int i = 0; i < roofed.size(); i += stride) {
         BlockPos candidate = roofed.get(i);
         if (terrain.standable(candidate)) {
            this.remember(candidate, ShelterProbe.score(terrain, body, Vec3.atBottomCenterOf(candidate)), Bed.Source.FOUND, now);
         }
      }

      return this.bed;
   }

   public boolean confirmBed(FacultyContext ctx) {
      AnimalHandle handle = ctx.handle();
      long now = ctx.now();
      if (this.bed == null) {
         return false;
      } else {
         Terrain terrain = terrain(ctx);
         ShelterProbe.Reading reading = ShelterProbe.score(terrain, bodyOf(handle), this.bed.centre());
         if (this.bed.isRoost()) {
            this.bed = new Bed(this.bed.at(), reading.total(), Bed.Source.ROOST, now);
            return true;
         } else if (ShelterProbe.roofed(terrain, this.bed.at()) && !(reading.total() < 1.5F)) {
            this.bed = new Bed(this.bed.at(), reading.total(), Bed.Source.FOUND, now);
            return true;
         } else {
            this.forgetBed();
            return false;
         }
      }
   }

   public void forgetBed() {
      this.bed = null;
      this.bedRoost = null;
   }

   public static double searchRadius(AnimalHandle handle) {
      return Math.min(handle.navRange(), handle.senses().sightRadius() * 0.5);
   }

   private void remember(BlockPos candidate, ShelterProbe.Reading reading, Bed.Source source, long now) {
      if (this.bed == null || !this.bed.isRoost() || source == Bed.Source.ROOST) {
         float total = reading.total();
         if (source == Bed.Source.ROOST || !(total < 1.5F)) {
            if (this.bed == null || this.bed.source() != source || !(total <= this.bed.score()) || this.bed.at().equals(candidate)) {
               this.bed = new Bed(candidate, total, source, now);
            }
         }
      }
   }

   @Nullable
   private Bed probeRoost(FacultyContext ctx, Halter.Roost roost) {
      AnimalHandle handle = ctx.handle();
      long now = ctx.now();
      Terrain terrain = terrain(ctx);
      ShelterProbe.Body body = bodyOf(handle);
      BlockPos best = null;
      float bestScore = -1.0F;
      BlockPos min = roost.min();
      BlockPos max = roost.max();

      for (int x = min.getX(); x <= max.getX(); x += 2) {
         for (int z = min.getZ(); z <= max.getZ(); z += 2) {
            for (int y = min.getY(); y <= max.getY(); y += 2) {
               BlockPos at = new BlockPos(x, y, z);
               if (terrain.standable(at)) {
                  float total = ShelterProbe.score(terrain, body, Vec3.atBottomCenterOf(at)).total();
                  if (total > bestScore) {
                     bestScore = total;
                     best = at;
                  }
               }
            }
         }
      }

      if (best == null) {
         best = roost.controller();
         bestScore = 0.0F;
      }

      return new Bed(best, bestScore, Bed.Source.ROOST, now);
   }

   private static ShelterProbe.Body bodyOf(AnimalHandle handle) {
      AABB bounds = handle.bounds();
      return new ShelterProbe.Body(bounds.getXsize(), handle.eyePosition().y - handle.bodyPosition().y);
   }

   private static Terrain terrain(final FacultyContext ctx) {
      final Terrain world = ctx.handle().terrain();
      final long now = ctx.now();
      return new Terrain() {
         @Override
         public boolean solid(BlockPos pos) {
            return world.solid(pos);
         }

         @Override
         public boolean fluid(BlockPos pos) {
            return world.fluid(pos);
         }

         @Override
         public int skyLight(BlockPos pos) {
            return world.skyLight(pos);
         }

         @Override
         public boolean standable(BlockPos pos) {
            return world.standable(pos);
         }

         @Override
         public float danger(Vec3 at) {
            return ctx.faculty(Places.class).map(p -> p.dangerAt(at, now)).orElse(0.0F);
         }
      };
   }
}
