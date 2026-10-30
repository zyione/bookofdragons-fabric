package net.magister.liberanimalium.chassis.component;

import java.util.HashMap;
import java.util.Map;
import net.magister.liberanimalium.anim.api.SignalView;
import net.magister.liberanimalium.anim.molang.SpringChannels;
import net.magister.liberanimalium.anim.rig.Attitude;
import net.magister.liberanimalium.anim.rig.GaitQueries;
import net.magister.liberanimalium.anim.rig.MotionTrack;
import net.magister.liberanimalium.anim.rig.RiggedBody;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.render.AttitudePass;

public final class AttitudeComponent implements IFaunaComponent {
   public int tick = Integer.MIN_VALUE;
   public double pitch;
   public double roll;
   public double lean;
   public double leanRaw;
   public double height = Double.NaN;
   public Object footing;
   public double pitchO;
   public double rollO;
   public double leanO;
   public double heightO = Double.NaN;
   public final SpringChannels body = new SpringChannels();
   public final double[] gaitVars = new double[GaitQueries.Var.COUNT];
   public final MotionTrack motion = new MotionTrack();
   public int motionTick = Integer.MIN_VALUE;
   public final Map<String, float[]> bodyWrites = new HashMap<>();
   public final Map<String, float[]> bodyLast = new HashMap<>();
   public final Map<String, float[]> bodyLastDelta = new HashMap<>();
   public SignalView snapshot;
   public AttitudePass.Applied applied;
   public boolean wrote;
   public boolean shifting;
   public Attitude.Cross cross = Attitude.Cross.NONE;
   public double speed;
   public double yawRate;
   public boolean gate;
   public boolean springs;
   public boolean measured;
   public boolean falling;
   public boolean dropping;
   public double fallen;
   public double fallBase;
   public double fallShare;
   public double fallShareO;
   public double castX = Double.NaN;
   public double castZ;
   public double castY;
   public double castYaw;
   public int castTick = Integer.MIN_VALUE;
   public int reused;
   public final Attitude.Probe[] probes = new Attitude.Probe[5];
   public final double[] grounds = new double[5];
   public final boolean[] voids = new boolean[5];
   public final double[] bands = new double[5];
   public final double[] ups = new double[5];
   public final double[] starts = new double[5];
   public double centreX;
   public double centreY;
   public double centreZ;
   public double forwardArm;
   public double backArm;
   public double sideArm;
   public double maxSlope;
   public double reach;
   public double jitterPitch;
   public Attitude.Write write = Attitude.Write.NONE;
   public String writes;

   public static AttitudeComponent of(FaunaBase body) {
      return body == null ? null : body.componentRegistry.get(AttitudeComponent.class);
   }

   public static AttitudeComponent of(RiggedBody body) {
      if (body instanceof FaunaBase fauna) {
         return of(fauna);
      } else {
         return body != null && body.animAttitude() instanceof AttitudeComponent ac ? ac : null;
      }
   }

   @Override
   public void onAttach(FaunaBase host) {
   }

   public float lastDelta(String bone, int axis) {
      float[] d = this.bodyLastDelta.get(bone);
      return d == null ? 0.0F : d[axis];
   }

   public void setLastDelta(String bone, int axis, float delta) {
      this.bodyLastDelta.computeIfAbsent(bone, b -> new float[6])[axis] = delta;
   }
}
