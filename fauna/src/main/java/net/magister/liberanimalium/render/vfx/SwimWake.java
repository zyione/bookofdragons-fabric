package net.magister.liberanimalium.render.vfx;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.magister.liberanimalium.body.state.RiderMotion;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaMultipartComponent;
import net.magister.liberanimalium.interop.ForeignWakes;
import net.magister.liberanimalium.platform.part.PartBody;
import net.magister.liberanimalium.render.waves.FaunaWaveManager;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SwimWake {
   private static final double MOVING = 0.03;
   private static final float DEPTH = 0.12F;
   private static final float DEPTH_BY_SPEED = 0.6F;
   private static final float PART_DEPTH = 0.7F;
   private static final int REST_RIPPLE_TICKS = 12;
   private static final double RANGE = 96.0;
   private static final PerfClock.Section CLOCK = PerfClock.client("waves.bodies");
   private static final Int2ObjectOpenHashMap<Vec3[]> LAST = new Int2ObjectOpenHashMap();
   private static final MutableBlockPos PROBE = new MutableBlockPos();

   private SwimWake() {
   }

   public static void tick(FaunaBase body) {
      Level level = body.level();
      if (level.isClientSide() && body.animRig().vfx().flightWake()) {
         ForeignWakes.own(body.getType());
         long clock = CLOCK.start();

         try {
            step(level, body);
         } finally {
            CLOCK.end(clock);
         }
      }
   }

   private static void step(Level level, FaunaBase body) {
      int id = body.getId();
      Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
      if (body.distanceToSqr(camera) > 9216.0) {
         LAST.remove(id);
      } else {
         FaunaMultipartComponent multipart = body.componentRegistry.get(FaunaMultipartComponent.class);
         PartBody<?>[] parts = multipart == null ? null : multipart.getParts();
         int count = parts == null ? 0 : parts.length;
         Vec3[] last = (Vec3[])LAST.get(id);
         if (last == null || last.length != count + 1) {
            last = new Vec3[count + 1];
         }

         FaunaWaveManager waves = FaunaWaveManager.getInstance();
         Vec3 velocity = RiderMotion.of(body);
         float through = (float)Mth.clamp(Math.abs(velocity.y) * 2.5 + velocity.horizontalDistance(), 0.0, 2.0);
         boolean any = false;
         AABB hull = body.getBoundingBox();
         float width = body.getBbWidth();
         double surface = !body.isFlying() && body.isInWater()
            ? surfaceAt(level, body.getX(), body.getZ(), body.getY() - 0.5, hull.maxY)
            : Double.NaN;
         if (Double.isNaN(surface)) {
            if (last[0] != null && through > 0.3F) {
               waves.addRadialForce(last[0].x, last[0].y, last[0].z, Math.max(0.75F, width * 0.55F), 0.3F + through);
            }

            last[0] = null;
         } else {
            Vec3 at = new Vec3(body.getX(), surface, body.getZ());
            if (last[0] == null) {
               waves.addRadialForce(at.x, at.y, at.z, Math.max(0.75F, width * 0.55F), 0.4F + through);
               if (through > 0.5F) {
                  WaterSpray.beat(level, body, at, Math.min(1.0F, through * 0.5F));
               }
            } else {
               double moved = Math.hypot(at.x - last[0].x, at.z - last[0].z);
               if (moved > 0.03 && moved < 8.0) {
                  waves.addWakeSlice(last[0], at, Math.max(0.6F, width * 0.7F), 0.12F + (float)moved * 0.6F, 1.25);
               } else if (body.tickCount % 12 == 0) {
                  waves.addRadialForce(at.x, at.y, at.z, Math.max(0.5F, width * 0.35F), 0.12F);
               }
            }

            last[0] = at;
            any = true;
         }

         boolean hullWet = last[0] != null;

         for (int k = 0; k < count; k++) {
            AABB box = parts[k].getBoundingBox();
            double x = (box.minX + box.maxX) * 0.5;
            double z = (box.minZ + box.maxZ) * 0.5;
            double partSurface = hullWet && x > hull.minX && x < hull.maxX && z > hull.minZ && z < hull.maxZ
               ? Double.NaN
               : surfaceAt(level, x, z, box.minY, box.maxY);
            if (Double.isNaN(partSurface)) {
               last[k + 1] = null;
            } else {
               Vec3 at = new Vec3(x, partSurface, z);
               Vec3 was = last[k + 1];
               float partWidth = (float)Math.max(box.maxX - box.minX, box.maxZ - box.minZ);
               if (was == null) {
                  waves.addRadialForce(at.x, at.y, at.z, Math.max(0.4F, partWidth * 0.5F), 0.25F + Math.min(0.3F, through * 0.3F));
               } else {
                  double moved = Math.hypot(at.x - was.x, at.z - was.z);
                  if (moved > 0.03 && moved < 8.0) {
                     waves.addWakeSlice(was, at, Math.max(0.4F, partWidth * 0.7F), (0.12F + (float)moved * 0.6F) * 0.7F, 1.25);
                  }
               }

               last[k + 1] = at;
               any = true;
            }
         }

         if (any) {
            LAST.put(id, last);
         } else {
            LAST.remove(id);
         }
      }
   }

   public static void forget(int id) {
      LAST.remove(id);
   }

   private static double surfaceAt(Level level, double x, double z, double minY, double maxY) {
      int bx = Mth.floor(x);
      int bz = Mth.floor(z);
      int from = Mth.floor(minY - 0.1);
      int over = Mth.floor(maxY) + 1;

      for (int y = over; y >= from; y--) {
         PROBE.set(bx, y, bz);
         if (level.getFluidState(PROBE).is(FluidTags.WATER)) {
            if (y == over) {
               return Double.NaN;
            }

            double surface = y + 0.8888888888888888;
            return minY <= surface ? surface : Double.NaN;
         }
      }

      return Double.NaN;
   }
}
