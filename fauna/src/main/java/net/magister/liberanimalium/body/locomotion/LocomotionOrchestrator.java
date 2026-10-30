package net.magister.liberanimalium.body.locomotion;

import java.util.EnumMap;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.body.locomotion.transition.LandTransition;
import net.magister.liberanimalium.body.locomotion.transition.LocomotionTransition;
import net.magister.liberanimalium.body.locomotion.transition.SplashDownTransition;
import net.magister.liberanimalium.body.locomotion.transition.TakeoffTransition;
import net.magister.liberanimalium.body.locomotion.transition.TransitionPrimitive;
import net.magister.liberanimalium.body.locomotion.transition.WallHitTransition;
import net.magister.liberanimalium.body.locomotion.transition.WaterEnterTransition;
import net.magister.liberanimalium.body.locomotion.transition.WaterExitTransition;
import net.magister.liberanimalium.body.state.TransportMode;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.IFaunaComponent;
import net.magister.liberanimalium.chassis.component.ThrottleComponent;
import net.minecraft.world.phys.Vec3;

public class LocomotionOrchestrator implements IFaunaComponent {
   private FaunaBase body;
   private final EnumMap<LocomotionTransition, TransitionPrimitive> transitions = new EnumMap<>(LocomotionTransition.class);
   private TransitionPrimitive activeTransition;

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
      this.transitions.put(LocomotionTransition.TAKEOFF, new TakeoffTransition());
      this.transitions.put(LocomotionTransition.LAND, new LandTransition());
      this.transitions.put(LocomotionTransition.WATER_ENTER, new WaterEnterTransition());
      this.transitions.put(LocomotionTransition.WATER_EXIT, new WaterExitTransition());
      this.transitions.put(LocomotionTransition.SPLASH_DOWN, new SplashDownTransition());
      this.transitions.put(LocomotionTransition.WALL_HIT, new WallHitTransition());
   }

   public LocomotionMode activeMode() {
      return LocomotionMode.fromTransportMode(this.body.getTransportMode());
   }

   public void requestMode(LocomotionMode mode, ModeChangeCause cause) {
      LocomotionMode resolved = resolveRequestedMode(mode, this.body.isModeAvailable(LocomotionMode.FLY));
      this.body.setTransportMode(resolved.toTransportMode());
      if (resolved == LocomotionMode.FLY) {
         ThrottleComponent throttle = this.body.componentRegistry.get(ThrottleComponent.class);
         if (throttle != null) {
            throttle.noteFlightGranted();
         }
      }
   }

   public void acceptClientMode(TransportMode claimed) {
      this.body.setTransportMode(claimed);
   }

   public boolean isTransitionActive() {
      return this.activeTransition != null;
   }

   public boolean isTakeoffActive() {
      return this.activeTransition instanceof TakeoffTransition;
   }

   @Nullable
   public LandTransition activeLanding() {
      return this.activeTransition instanceof LandTransition landing ? landing : null;
   }

   @Nullable
   public WallHitTransition activeWallHit() {
      return this.activeTransition instanceof WallHitTransition hit ? hit : null;
   }

   public boolean attitudeOwned() {
      return this.activeTransition != null && this.activeTransition.ownsAttitude();
   }

   public boolean ejectGrace() {
      TransitionPrimitive hit = this.transitions.get(LocomotionTransition.WALL_HIT);
      return hit instanceof WallHitTransition wall && wall.ejectGrace(this.body);
   }

   public boolean requestWallHit(Vec3 impact, float severity, ModeChangeCause cause) {
      return this.requestWallHit(impact, severity, null, cause);
   }

   public boolean requestWallHit(Vec3 impact, float severity, @Nullable WallHit.Choice forced, ModeChangeCause cause) {
      if (this.activeTransition != null) {
         return false;
      }

      WallHitTransition hit = (WallHitTransition)this.transitions.get(LocomotionTransition.WALL_HIT);
      hit.arm(impact, severity, forced);
      return this.requestTransition(LocomotionTransition.WALL_HIT, cause);
   }

   public String activeTransitionName() {
      return this.activeTransition == null ? "none" : this.activeTransition.getClass().getSimpleName();
   }

   public boolean requestTransition(LocomotionTransition transition, ModeChangeCause cause) {
      if (this.activeTransition != null) {
         return false;
      }

      TransitionPrimitive prim = this.transitions.get(transition);
      if (prim != null && prim.precondition(this.body)) {
         switch (transition) {
            case TAKEOFF:
               this.requestMode(LocomotionMode.FLY, cause);
               break;
            case WATER_ENTER:
            case SPLASH_DOWN:
               this.requestMode(LocomotionMode.SWIM, cause);
               break;
            case WATER_EXIT:
               this.requestMode(LocomotionMode.WALK, cause);
            case LAND:
            case WALL_HIT:
         }

         if (!this.body.isControlledByLocalInstance()) {
            return true;
         }

         this.activeTransition = prim;
         prim.begin(this.body);
         return true;
      } else {
         return false;
      }
   }

   public void interruptTransition(ModeChangeCause cause) {
      if (this.activeTransition != null) {
         TransitionPrimitive prim = this.activeTransition;
         this.activeTransition = null;
         prim.interrupt(this.body);
         this.requestMode(prim.fallbackMode(), cause);
      }
   }

   public void tick(SteeringIntent intent, LocomotionOrchestrator.IntentSource source) {
      if (this.activeTransition != null) {
         boolean ownsPhysics = this.activeTransition.ownsPhysics();
         if (this.activeTransition.tick(this.body)) {
            this.activeTransition = null;
         }

         if (ownsPhysics) {
            return;
         }
      }

      ModeEngine engine = this.engineFor(this.activeMode());
      if (engine != null) {
         engine.tick(intent);
      }
   }

   private ModeEngine engineFor(LocomotionMode mode) {
      return switch (mode) {
         case WALK -> (WalkEngine)this.body.componentRegistry.get(WalkEngine.class);
         case FLY -> (FlightEngine)this.body.componentRegistry.get(FlightEngine.class);
         case SWIM, DIVE -> (WaterEngine)this.body.componentRegistry.get(WaterEngine.class);
         case CLIMB -> (ClimbEngine)this.body.componentRegistry.get(ClimbEngine.class);
      };
   }

   public static LocomotionMode resolveRequestedMode(LocomotionMode requested, boolean flyAvailable) {
      return requested == LocomotionMode.FLY && !flyAvailable ? LocomotionMode.WALK : requested;
   }

   public enum IntentSource {
      RIDER,
      AI;
   }
}
