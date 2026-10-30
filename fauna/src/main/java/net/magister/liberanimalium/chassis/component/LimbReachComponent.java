package net.magister.liberanimalium.chassis.component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mod.azure.azurelib.model.AzBakedModel;
import net.magister.liberanimalium.anim.molang.SpringChannelSpec;
import net.magister.liberanimalium.anim.rig.GaitSpec;
import net.magister.liberanimalium.anim.rig.LimbDef;
import net.magister.liberanimalium.anim.rig.RiggedBody;
import net.magister.liberanimalium.anim.rig.StepsSpec;
import net.magister.liberanimalium.body.grip.GripGeometry;
import net.magister.liberanimalium.body.grip.GripSpec;
import net.magister.liberanimalium.body.state.TransportMode;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.render.AttitudePass;
import net.magister.liberanimalium.render.limbs.FaunaLegReachSolver;
import net.magister.liberanimalium.render.limbs.Footfalls;
import net.magister.liberanimalium.render.limbs.GripSolver;
import net.magister.liberanimalium.render.limbs.Limb;
import net.magister.liberanimalium.render.limbs.LimbRig;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class LimbReachComponent implements IFaunaComponent {
   private FaunaBase body;
   private LimbRig rig;
   private StepsSpec walkSteps;
   private StepsSpec walkStepsFrom;
   private GripSpec walkStepsGrip;
   private LimbRig gripRig;
   private GripSpec gripFor;
   private StepsSpec gripSteps;
   private AzBakedModel lastModel;

   public static LimbReachComponent of(FaunaBase body) {
      return body == null ? null : body.componentRegistry.get(LimbReachComponent.class);
   }

   public static LimbReachComponent of(RiggedBody body) {
      if (body instanceof FaunaBase fauna) {
         return of(fauna);
      } else {
         return body != null && body.animLimbReach() instanceof LimbReachComponent rc ? rc : null;
      }
   }

   public LimbReachComponent.Gait gait() {
      return this.rig == null ? LimbReachComponent.Gait.NONE : this.rig.gait;
   }

   public double[] footWatch(String limbName) {
      if (this.rig != null) {
         for (Limb L : this.rig.limbs) {
            if (L.def.name().equals(limbName)) {
               return new double[]{L.st.footTarget == null ? Double.NaN : L.st.footTarget.y, L.st.groundY};
            }
         }
      }

      return new double[]{Double.NaN, Double.NaN};
   }

   public void armTrace(Consumer<String> sink) {
      if (this.rig == null) {
         sink.accept("no limb has been placed for this entity");
      } else {
         this.rig.traceSink = sink;
      }
   }

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   public AzBakedModel lastModel() {
      return this.lastModel;
   }

   public List<String> applyGrip(
      AzBakedModel model, Vec3 drawn, Matrix4f modelToWorld, GripSpec spec, Supplier<GripGeometry.Box> boxOf, float weight, boolean close, float partial
   ) {
      if (this.body.level() != null && this.body.level().isClientSide()) {
         this.lastModel = model;
         if (this.gripFor != spec) {
            this.gripFor = spec;
            List<LimbDef> defs = new ArrayList<>();

            for (GripSpec.Gripper gripper : spec.grippers()) {
               if (gripper.closes()) {
                  defs.add(gripper.def());
               }
            }

            this.gripSteps = new StepsSpec(true, defs, null, GaitSpec.DEFAULT, SpringChannelSpec.EMPTY, List.of());
         }

         if (this.gripRig == null) {
            this.gripRig = new LimbRig();
         }

         FaunaLegReachSolver.resolve(this.gripRig, model, this.gripSteps);
         Matrix4f toModel = new Matrix4f(modelToWorld).invert();
         return GripSolver.solve(this.body, this.gripRig, model, spec, boxOf, weight, close, drawn, modelToWorld, toModel, partial);
      } else {
         return List.of();
      }
   }

   private StepsSpec withoutGripping(StepsSpec steps, GripSpec grip) {
      if (steps == this.walkStepsFrom && grip == this.walkStepsGrip) {
         return this.walkSteps;
      }

      Set<String> gripped = new HashSet<>();

      for (GripSpec.Gripper gripper : grip.grippers()) {
         if (gripper.closes()) {
            gripped.addAll(gripper.limb());
         }
      }

      List<LimbDef> kept = new ArrayList<>();

      for (LimbDef limb : steps.limbs) {
         if (!gripped.contains(limb.root()) && !gripped.contains(limb.mid()) && !gripped.contains(limb.foot())) {
            kept.add(limb);
         }
      }

      this.walkStepsFrom = steps;
      this.walkStepsGrip = grip;
      this.walkSteps = kept.size() == steps.limbs.size() ? steps : new StepsSpec(steps.enabled, kept, steps.attitude, steps.gait, steps.body, steps.problems());
      return this.walkSteps;
   }

   public List<String> applyReach(AzBakedModel model, Vec3 drawn, Matrix4f modelToWorld, float partialTick) {
      if (this.body.level() != null && this.body.level().isClientSide()) {
         this.lastModel = model;
         if (this.body.getStateContext().getTransportMode() != TransportMode.GROUNDED && !this.body.isClimbing()) {
            return List.of();
         }

         if (AttitudePass.dropping(this.body)) {
            return List.of();
         }

         StepsSpec steps = this.body.animSteps();
         if (!steps.isEmpty() && steps.enabled) {
            if (this.rig == null) {
               this.rig = new LimbRig();
            }

            GripComponent grip = this.body.grip();
            if (grip != null && grip.shownWeight(partialTick) > 0.0F) {
               steps = this.withoutGripping(steps, grip.spec());
            }

            List<String> touched = FaunaLegReachSolver.apply(this.body, this.rig, model, steps, drawn, modelToWorld, partialTick);
            Footfalls.tick(this.body, this.rig);
            return touched;
         } else {
            return List.of();
         }
      } else {
         return List.of();
      }
   }

   public record Gait(
      double lift,
      double sway,
      double stride,
      double frontLift,
      double frontSway,
      double frontStride,
      double rearLift,
      double rearSway,
      double rearStride,
      double supportRight,
      double supportForward
   ) {
      public static final LimbReachComponent.Gait NONE = new LimbReachComponent.Gait(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   }
}
