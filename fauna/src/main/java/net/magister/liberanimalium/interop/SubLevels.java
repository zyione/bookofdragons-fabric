package net.magister.liberanimalium.interop;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.UnaryOperator;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

public final class SubLevels {
   private static boolean looked;
   private static Object companion;
   private static MethodHandle project;
   private static MethodHandle containing;
   private static MethodHandle inGrid;
   private static Class<?> moved;
   private static MethodHandle tracking;
   private static MethodHandle track;
   private static MethodHandle removed;
   private static Class<?> sticks;
   private static MethodHandle plotPosition;
   private static MethodHandle logicalPose;
   private static MethodHandle lastPose;
   private static MethodHandle orientation;
   private static MethodHandle toPlot;
   private static MethodHandle fromPlot;
   private static MethodHandle normalOut;
   private static MethodHandle collision;
   private static Class<?> carried;
   private static MethodHandle momentum;
   private static Field inherited;
   private static Field firstCollisions;
   private static final double BENEATH = 1.5;

   private SubLevels() {
   }

   public static boolean present() {
      if (!looked) {
         look();
      }

      return project != null;
   }

   public static Vec3 toWorld(Level level, Vec3 at) {
      if (!looked) {
         look();
      }

      if (project == null) {
         return at;
      }

      try {
         Vec3 world = (Vec3)project.invoke((Object)companion, (Level)level, (Vec3)at);
         return world == null ? at : world;
      } catch (Throwable broken) {
         project = null;
         return at;
      }
   }

   @Nullable
   public static Object under(Entity body) {
      if (!looked) {
         look();
      }

      if (tracking != null && moved.isInstance(body)) {
         try {
            return (Object)tracking.invoke((Entity)body);
         } catch (Throwable broken) {
            tracking = null;
            return null;
         }
      } else {
         return null;
      }
   }

   public static void resume(Entity body, @Nullable Object subLevel) {
      if (subLevel != null && track != null && under(body) == null) {
         try {
            if (removed != null && (boolean)removed.invoke((Object)subLevel)) {
               return;
            }

            track.invoke((Entity)body, (Object)subLevel);
         } catch (Throwable broken) {
            track = null;
         }
      }
   }

   @Nullable
   public static Vec3 plot(@Nullable Object subLevel, Vec3 world) {
      if (subLevel != null && toPlot != null) {
         try {
            return (Vec3)toPlot.invoke((Object)(Object)logicalPose.invoke((Object)subLevel), (Vec3)world);
         } catch (Throwable broken) {
            return null;
         }
      } else {
         return null;
      }
   }

   @Nullable
   public static Vec3 world(@Nullable Object subLevel, Vec3 place) {
      if (subLevel != null && fromPlot != null) {
         try {
            return (Vec3)fromPlot.invoke((Object)(Object)logicalPose.invoke((Object)subLevel), (Vec3)place);
         } catch (Throwable broken) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static HitResult inWorld(Level level, HitResult hit) {
      return (HitResult)(hit instanceof BlockHitResult block && inPlot(level, hit.getLocation())
         ? new BlockHitResult(toWorld(level, hit.getLocation()), block.getDirection(), block.getBlockPos(), block.isInside())
         : hit);
   }

   public static Vec3 turnOut(Level level, Vec3 place, Vec3 direction) {
      if (normalOut != null && inPlot(level, place)) {
         Object subLevel = at(level, place);
         if (subLevel == null) {
            return direction;
         }

         try {
            return (Vec3)normalOut.invoke((Object)(Object)logicalPose.invoke((Object)subLevel), (Vec3)direction);
         } catch (Throwable broken) {
            return direction;
         }
      } else {
         return direction;
      }
   }

   @Nullable
   public static Object at(Level level, Vec3 place) {
      if (!looked) {
         look();
      }

      if (containing == null) {
         return null;
      }

      try {
         return (Object)containing.invoke((Object)companion, (Level)level, (Position)place);
      } catch (Throwable broken) {
         containing = null;
         return null;
      }
   }

   @Nullable
   public static Object beneath(Entity body) {
      if (!present()) {
         return null;
      } else {
         Vec3 feet = body.position().add(0.0, 0.25, 0.0);
         BlockHitResult hit = body.level().clip(new ClipContext(feet, feet.subtract(0.0, 1.5, 0.0), Block.COLLIDER, Fluid.NONE, body));
         if (hit.getType() != Type.BLOCK) {
            return null;
         } else {
            return inPlot(body.level(), hit.getLocation()) ? at(body.level(), hit.getLocation()) : null;
         }
      }
   }

   public static boolean inPlot(Level level, Vec3 at) {
      if (!looked) {
         look();
      }

      if (inGrid == null) {
         return false;
      }

      try {
         return (boolean)inGrid.invoke((Object)companion, (Level)level, (Position)at);
      } catch (Throwable broken) {
         inGrid = null;
         return false;
      }
   }

   public static void unpin(Entity body) {
      if (!looked) {
         look();
      }

      if (plotPosition != null && sticks.isInstance(body)) {
         try {
            plotPosition.invoke((Entity)body, (Vec3)((Vec3)null));
         } catch (Throwable broken) {
            plotPosition = null;
         }
      }
   }

   public static void pin(Entity body, @Nullable Vec3 place) {
      if (!looked) {
         look();
      }

      if (place != null && plotPosition != null && sticks.isInstance(body)) {
         try {
            plotPosition.invoke((Entity)body, (Vec3)place);
         } catch (Throwable broken) {
            plotPosition = null;
         }
      }
   }

   public static void untrack(Entity body) {
      if (!looked) {
         look();
      }

      if (track != null && moved.isInstance(body)) {
         try {
            track.invoke((Entity)body, (Object)null);
         } catch (Throwable broken) {
            track = null;
         }
      }
   }

   public static void forget(Entity body) {
      if (!looked) {
         look();
      }

      if (collision != null && moved.isInstance(body)) {
         try {
            if (momentum != null && carried.isInstance(body)) {
               ((Vector3d)momentum.invoke((Entity)body)).zero();
            }

            Object info = (Object)collision.invoke((Entity)body);
            if (info == null) {
               return;
            }

            inherited.set(info, null);
            if (firstCollisions != null) {
               firstCollisions.set(info, null);
            }
         } catch (Throwable broken) {
            collision = null;
         }
      }
   }

   @Nullable
   public static UnaryOperator<Vec3> ride(@Nullable Object subLevel, Vec3 probe) {
      if (subLevel != null && fromPlot != null) {
         try {
            Object now = (Object)logicalPose.invoke((Object)subLevel);
            Object before = (Object)lastPose.invoke((Object)subLevel);
            Vec3 moved = (Vec3)fromPlot.invoke((Object)now, (Vec3)(Vec3)toPlot.invoke((Object)before, (Vec3)probe));
            return moved.distanceToSqr(probe) < 1.0E-10 ? null : point -> {
               try {
                  return (Vec3)fromPlot.invoke((Object)now, (Vec3)(Vec3)toPlot.invoke((Object)before, (Vec3)point));
               } catch (Throwable brokenx) {
                  return point;
               }
            };
         } catch (Throwable broken) {
            fromPlot = null;
            return null;
         }
      } else {
         return null;
      }
   }

   public static float turn(@Nullable Object subLevel) {
      if (subLevel != null && orientation != null) {
         try {
            Quaterniondc now = (Quaterniondc)orientation.invoke((Object)(Object)logicalPose.invoke((Object)subLevel));
            Quaterniondc before = (Quaterniondc)orientation.invoke((Object)(Object)lastPose.invoke((Object)subLevel));
            Quaterniond relative = now.div(before, new Quaterniond());
            return Math.abs(relative.w) < 1.0E-9 ? 0.0F : (float)Math.toDegrees(2.0 * relative.y / relative.w);
         } catch (Throwable broken) {
            orientation = null;
            return 0.0F;
         }
      } else {
         return 0.0F;
      }
   }

   private static synchronized void look() {
      if (!looked) {
         Lookup lookup = MethodHandles.publicLookup();

         try {
            Class<?> api = Class.forName("dev.ryanhcode.sable.companion.SableCompanion");
            Object instance = api.getField("INSTANCE").get(null);
            MethodHandle handle = lookup.unreflect(api.getMethod("projectOutOfSubLevel", Level.class, Vec3.class));
            companion = instance;
            project = instance == null ? null : handle;
            containing = lookup.unreflect(api.getMethod("getContaining", Level.class, Position.class));
            inGrid = lookup.unreflect(api.getMethod("isInPlotGrid", Level.class, Position.class));
         } catch (ReflectiveOperationException | LinkageError absent) {
            project = null;
         }

         try {
            Class<?> extension = Class.forName("dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension");
            Method get = extension.getMethod("sable$getTrackingSubLevel");
            tracking = lookup.unreflect(get);

            for (Method method : extension.getMethods()) {
               if (method.getName().equals("sable$setTrackingSubLevel") && method.getParameterCount() == 1) {
                  track = lookup.unreflect(method);
               }
            }

            moved = extension;
            Class<?> subLevel = get.getReturnType();
            removed = lookup.unreflect(subLevel.getMethod("isRemoved"));
            Class<?> access = Class.forName("dev.ryanhcode.sable.companion.SubLevelAccess");
            Method pose = access.getMethod("logicalPose");
            logicalPose = lookup.unreflect(pose);
            lastPose = lookup.unreflect(access.getMethod("lastPose"));
            orientation = lookup.unreflect(pose.getReturnType().getMethod("orientation"));
            toPlot = lookup.unreflect(pose.getReturnType().getMethod("transformPositionInverse", Vec3.class));
            fromPlot = lookup.unreflect(pose.getReturnType().getMethod("transformPosition", Vec3.class));
            normalOut = lookup.unreflect(pose.getReturnType().getMethod("transformNormal", Vec3.class));
            Method info = extension.getMethod("sable$getCollisionInfo");
            inherited = info.getReturnType().getField("inheritedMotion");
            firstCollisions = info.getReturnType().getField("firstCollisions");
            collision = lookup.unreflect(info);
         } catch (ReflectiveOperationException | LinkageError absent) {
            if (tracking == null) {
               track = null;
            }
         }

         try {
            Class<?> living = Class.forName("dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LivingEntityMovementExtension");
            momentum = lookup.unreflect(living.getMethod("sable$getInheritedVelocity"));
            carried = living;
         } catch (ReflectiveOperationException | LinkageError absent) {
            momentum = null;
         }

         try {
            Class<?> extension = Class.forName("dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.EntityStickExtension");
            plotPosition = lookup.unreflect(extension.getMethod("sable$setPlotPosition", Vec3.class));
            sticks = extension;
         } catch (ReflectiveOperationException | LinkageError absent) {
            plotPosition = null;
         }

         looked = true;
      }
   }
}
