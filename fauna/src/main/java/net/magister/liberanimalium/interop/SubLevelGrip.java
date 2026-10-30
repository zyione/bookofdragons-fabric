package net.magister.liberanimalium.interop;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class SubLevelGrip {
   private static boolean looked;
   private static Object companion;
   private static Method intersecting;
   private static Constructor<?> bounds;
   private static Method removed;
   private static Method logicalPose;
   private static Method renderPose;
   private static Method orientation;
   private static Method scale;
   private static Method toPlot;
   private static Method fromPlot;
   private static Method plotOf;
   private static Method plotBounds;
   private static Method[] plotEdges;
   private static Class<?> serverSubLevel;
   private static Method systemOf;
   private static Method pipelineOf;
   private static Method addConstraint;
   private static Method wakeUp;
   private static Constructor<?> genericJoint;
   private static Method setFrame1;
   private static Method setMotor;
   private static Method removeJoint;
   private static Method validJoint;
   private static Object[] axes;
   private static Method massTracker;
   private static Method massOf;
   private static Method gravityOf;

   private SubLevelGrip() {
   }

   public static boolean present() {
      if (!looked) {
         look();
      }

      return intersecting != null;
   }

   public static List<Object> near(Level level, AABB search) {
      List<Object> out = new ArrayList<>();
      if (!present()) {
         return out;
      }

      try {
         for (Object sub : (Iterable)intersecting.invoke(companion, level, bounds.newInstance(search))) {
            if (!(Boolean)removed.invoke(sub)) {
               out.add(sub);
            }
         }
      } catch (ReflectiveOperationException | RuntimeException broken) {
         intersecting = null;
      }

      return out;
   }

   public static boolean removed(Object sub) {
      if (!present()) {
         return true;
      }

      try {
         return (Boolean)removed.invoke(sub);
      } catch (ReflectiveOperationException | RuntimeException broken) {
         return true;
      }
   }

   public static List<AABB> blocks(Level level, Object sub, AABB world) {
      List<AABB> out = new ArrayList<>();
      AABB plot = toPlot(sub, world);
      int[] edges = plotEdges(sub);
      if (plot != null && edges != null) {
         int x0 = Math.max(edges[0], (int)Math.floor(plot.minX));
         int x1 = Math.min(edges[3], (int)Math.floor(plot.maxX));
         int y0 = Math.max(edges[1], (int)Math.floor(plot.minY));
         int y1 = Math.min(edges[4], (int)Math.floor(plot.maxY));
         int z0 = Math.max(edges[2], (int)Math.floor(plot.minZ));
         int z1 = Math.min(edges[5], (int)Math.floor(plot.maxZ));
         MutableBlockPos at = new MutableBlockPos();

         for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
               if (level.hasChunkAt(at.set(x, y0, z))) {
                  for (int y = y0; y <= y1; y++) {
                     BlockState state = level.getBlockState(at.set(x, y, z));
                     if (!state.isAir()) {
                        VoxelShape shape = state.getCollisionShape(level, at);
                        if (!shape.isEmpty()) {
                           for (AABB part : shape.toAabbs()) {
                              out.add(part.move(x, y, z));
                           }
                        }
                     }
                  }
               }
            }
         }

         return out;
      } else {
         return out;
      }
   }

   public static double distance(Object sub, List<AABB> blocks, Vec3 world) {
      Vec3 plot = toPlot(sub, world);
      if (plot != null && !blocks.isEmpty()) {
         double nearest = Double.MAX_VALUE;

         for (AABB box : blocks) {
            nearest = Math.min(nearest, box.distanceToSqr(plot));
         }

         return Math.sqrt(nearest) * scale(sub);
      } else {
         return Double.MAX_VALUE;
      }
   }

   @Nullable
   public static Vec3 nearestBlock(List<AABB> blocks, Vec3 plot) {
      AABB best = null;
      double nearest = Double.MAX_VALUE;

      for (AABB box : blocks) {
         double d = box.distanceToSqr(plot);
         if (d < nearest) {
            nearest = d;
            best = box;
         }
      }

      return best == null ? null : Vec3.atCenterOf(BlockPos.containing(best.getCenter()));
   }

   @Nullable
   public static AABB region(Level level, Object sub, Vec3 heldBlock, double half, double depth) {
      int[] edges = plotEdges(sub);
      if (edges == null) {
         return null;
      }

      int reach = (int)Math.ceil(half);
      int top = (int)Math.floor(heldBlock.y);
      int fall = Math.max(0, (int)Math.ceil(depth) - 1);
      MutableBlockPos at = new MutableBlockPos();
      AABB out = null;

      for (int x = (int)Math.floor(heldBlock.x) - reach; x <= (int)Math.floor(heldBlock.x) + reach; x++) {
         for (int z = (int)Math.floor(heldBlock.z) - reach; z <= (int)Math.floor(heldBlock.z) + reach; z++) {
            if (!(Math.abs(x + 0.5 - heldBlock.x) > half + 1.0E-6)
               && !(Math.abs(z + 0.5 - heldBlock.z) > half + 1.0E-6)
               && x >= edges[0]
               && x <= edges[3]
               && z >= edges[2]
               && z <= edges[5]) {
               for (int y = top - fall; y <= top; y++) {
                  if (y >= edges[1] && y <= edges[4]) {
                     BlockState state = level.getBlockState(at.set(x, y, z));
                     if (!state.isAir()) {
                        VoxelShape shape = state.getCollisionShape(level, at);
                        if (!shape.isEmpty()) {
                           AABB box = shape.bounds().move(x, y, z);
                           out = out == null ? box : out.minmax(box);
                        }
                     }
                  }
               }
            }
         }
      }

      return out;
   }

   @Nullable
   public static Vec3 toPlot(Object sub, Vec3 world) {
      if (!present()) {
         return null;
      }

      try {
         return (Vec3)toPlot.invoke(logicalPose.invoke(sub), world);
      } catch (ReflectiveOperationException | RuntimeException broken) {
         return null;
      }
   }

   @Nullable
   public static Vec3 toWorld(Object sub, Vec3 plot, float partial) {
      Object pose = pose(sub, partial);
      if (pose == null) {
         return null;
      }

      try {
         return (Vec3)fromPlot.invoke(pose, plot);
      } catch (ReflectiveOperationException | RuntimeException broken) {
         return null;
      }
   }

   public static Quaterniond turn(Object sub, float partial) {
      Object pose = pose(sub, partial);
      if (pose == null) {
         return new Quaterniond();
      }

      try {
         return new Quaterniond((Quaterniondc)orientation.invoke(pose));
      } catch (ReflectiveOperationException | RuntimeException broken) {
         return new Quaterniond();
      }
   }

   public static double scale(Object sub) {
      if (!present()) {
         return 1.0;
      }

      try {
         Vector3dc s = (Vector3dc)scale.invoke(logicalPose.invoke(sub));
         return s == null ? 1.0 : s.x();
      } catch (ReflectiveOperationException | RuntimeException broken) {
         return 1.0;
      }
   }

   @Nullable
   private static Object pose(Object sub, float partial) {
      if (!present()) {
         return null;
      }

      try {
         return renderPose != null && renderPose.getDeclaringClass().isInstance(sub) ? renderPose.invoke(sub, partial) : logicalPose.invoke(sub);
      } catch (ReflectiveOperationException | RuntimeException broken) {
         return null;
      }
   }

   @Nullable
   private static AABB toPlot(Object sub, AABB world) {
      AABB out = null;

      for (int i = 0; i < 8; i++) {
         Vec3 corner = new Vec3(
            (i & 1) == 0 ? world.minX : world.maxX, (i & 2) == 0 ? world.minY : world.maxY, (i & 4) == 0 ? world.minZ : world.maxZ
         );
         Vec3 plot = toPlot(sub, corner);
         if (plot == null) {
            return null;
         }

         out = out == null ? new AABB(plot, plot) : out.minmax(new AABB(plot, plot));
      }

      return out;
   }

   @Nullable
   private static int[] plotEdges(Object sub) {
      if (!present()) {
         return null;
      }

      try {
         Object box = plotBounds.invoke(plotOf.invoke(sub));
         if (box == null) {
            return null;
         }

         int[] edges = new int[6];

         for (int i = 0; i < 6; i++) {
            edges[i] = (Integer)plotEdges[i].invoke(box);
         }

         return edges;
      } catch (ReflectiveOperationException | RuntimeException broken) {
         return null;
      }
   }

   @Nullable
   public static SubLevelGrip.Joint join(ServerLevel level, Object sub, Vec3 world, Quaterniondc worldTurn, Vec3 plot, Quaterniondc plotTurn) {
      if (present() && genericJoint != null && serverSubLevel.isInstance(sub)) {
         try {
            Object pipeline = pipelineOf.invoke(systemOf.invoke(null, level));
            Object config = genericJoint.newInstance(
               new Vector3d(world.x, world.y, world.z), new Vector3d(plot.x, plot.y, plot.z), worldTurn, plotTurn
            );
            Object handle = addConstraint.invoke(pipeline, null, sub, config);
            return handle == null ? null : new SubLevelGrip.Joint(handle);
         } catch (ReflectiveOperationException | RuntimeException broken) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static void wake(ServerLevel level, Object sub) {
      try {
         wakeUp.invoke(pipelineOf.invoke(systemOf.invoke(null, level)), sub);
      } catch (ReflectiveOperationException | RuntimeException var3) {
      }
   }

   public static double mass(Object sub) {
      try {
         return (Double)massOf.invoke(massTracker.invoke(sub));
      } catch (ReflectiveOperationException | RuntimeException broken) {
         return 0.0;
      }
   }

   public static double gravity(ServerLevel level) {
      try {
         Vector3d pull = (Vector3d)gravityOf.invoke(null, level);
         return pull == null ? 9.81 : pull.length();
      } catch (ReflectiveOperationException | RuntimeException broken) {
         return 9.81;
      }
   }

   private static synchronized void look() {
      if (!looked) {
         try {
            Class<?> api = Class.forName("dev.ryanhcode.sable.companion.SableCompanion");
            Class<?> box3dc = Class.forName("dev.ryanhcode.sable.companion.math.BoundingBox3dc");
            Class<?> access = Class.forName("dev.ryanhcode.sable.companion.SubLevelAccess");
            Class<?> pose = Class.forName("dev.ryanhcode.sable.companion.math.Pose3dc");
            Class<?> subLevel = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel");
            companion = api.getField("INSTANCE").get(null);
            bounds = Class.forName("dev.ryanhcode.sable.companion.math.BoundingBox3d").getConstructor(AABB.class);
            removed = subLevel.getMethod("isRemoved");
            logicalPose = access.getMethod("logicalPose");
            orientation = pose.getMethod("orientation");
            scale = pose.getMethod("scale");
            toPlot = pose.getMethod("transformPositionInverse", Vec3.class);
            fromPlot = pose.getMethod("transformPosition", Vec3.class);
            plotOf = subLevel.getMethod("getPlot");
            plotBounds = plotOf.getReturnType().getMethod("getBoundingBox");
            Class<?> box3ic = plotBounds.getReturnType();
            plotEdges = new Method[]{
               box3ic.getMethod("minX"),
               box3ic.getMethod("minY"),
               box3ic.getMethod("minZ"),
               box3ic.getMethod("maxX"),
               box3ic.getMethod("maxY"),
               box3ic.getMethod("maxZ")
            };
            intersecting = api.getMethod("getAllIntersecting", Level.class, box3dc);
         } catch (ReflectiveOperationException | LinkageError | RuntimeException absent) {
            intersecting = null;
            looked = true;
            return;
         }

         try {
            renderPose = Class.forName("dev.ryanhcode.sable.sublevel.ClientSubLevel").getMethod("renderPose", float.class);
         } catch (ReflectiveOperationException | LinkageError | RuntimeException absent) {
            renderPose = null;
         }

         try {
            serverSubLevel = Class.forName("dev.ryanhcode.sable.sublevel.ServerSubLevel");
            Class<?> system = Class.forName("dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem");
            Class<?> pipeline = Class.forName("dev.ryanhcode.sable.api.physics.PhysicsPipeline");
            Class<?> body = Class.forName("dev.ryanhcode.sable.api.physics.PhysicsPipelineBody");
            Class<?> configuration = Class.forName("dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration");
            Class<?> generic = Class.forName("dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration");
            Class<?> handle = Class.forName("dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle");
            Class<?> any = Class.forName("dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle");
            Class<Enum> axis = (Class<Enum>)Class.forName("dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis");
            systemOf = system.getMethod("get", Level.class);
            pipelineOf = system.getMethod("getPipeline");
            addConstraint = pipeline.getMethod("addConstraint", body, body, configuration);
            wakeUp = pipeline.getMethod("wakeUp", body);
            genericJoint = generic.getConstructor(Vector3dc.class, Vector3dc.class, Quaterniondc.class, Quaterniondc.class);
            setFrame1 = handle.getMethod("setFrame1", Vector3dc.class, Quaterniondc.class);
            setMotor = any.getMethod("setMotor", axis, double.class, double.class, double.class, boolean.class, double.class);
            removeJoint = any.getMethod("remove");
            validJoint = any.getMethod("isValid");
            String[] names = new String[]{"LINEAR_X", "LINEAR_Y", "LINEAR_Z", "ANGULAR_X", "ANGULAR_Y", "ANGULAR_Z"};
            axes = new Object[names.length];

            for (int i = 0; i < names.length; i++) {
               axes[i] = Enum.valueOf(axis, names[i]);
            }

            massTracker = serverSubLevel.getMethod("getMassTracker");
            massOf = massTracker.getReturnType().getMethod("getMass");
            gravityOf = Class.forName("dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData").getMethod("getGravity", Level.class);
         } catch (ReflectiveOperationException | LinkageError | RuntimeException absent) {
            genericJoint = null;
         }

         looked = true;
      }
   }

   public static final class Joint {
      private final Object handle;

      private Joint(Object handle) {
         this.handle = handle;
      }

      public void frame(Vec3 world, Quaterniondc turn) {
         try {
            SubLevelGrip.setFrame1.invoke(this.handle, new Vector3d(world.x, world.y, world.z), turn);
         } catch (ReflectiveOperationException | RuntimeException broken) {
            this.remove();
         }
      }

      public void drive(double stiffness, double damping, double force, double turnStiffness, double turnDamping, double torque) {
         try {
            for (int i = 0; i < 6; i++) {
               boolean straight = i < 3;
               SubLevelGrip.setMotor
                  .invoke(
                     this.handle,
                     SubLevelGrip.axes[i],
                     0.0,
                     straight ? stiffness : turnStiffness,
                     straight ? damping : turnDamping,
                     true,
                     straight ? force : torque
                  );
            }
         } catch (ReflectiveOperationException | RuntimeException broken) {
            this.remove();
         }
      }

      public boolean valid() {
         try {
            return (Boolean)SubLevelGrip.validJoint.invoke(this.handle);
         } catch (ReflectiveOperationException | RuntimeException broken) {
            return false;
         }
      }

      public void remove() {
         try {
            if (this.valid()) {
               SubLevelGrip.removeJoint.invoke(this.handle);
            }
         } catch (ReflectiveOperationException | RuntimeException var2) {
         }
      }
   }
}
