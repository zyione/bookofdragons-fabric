package net.magister.liberanimalium.render;

import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;
import net.magister.liberanimalium.anim.rig.BoneAxis;
import net.magister.liberanimalium.anim.rig.PointChain;
import net.magister.liberanimalium.anim.rig.RigFrame;
import net.magister.liberanimalium.anim.rig.RigPass;
import net.magister.liberanimalium.anim.rig.RigWrite;
import net.magister.liberanimalium.body.state.TransportMode;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaSpineCascadeComponent;
import net.minecraft.world.phys.Vec3;

final class CascadePass implements RigPass {
   @Override
   public String name() {
      return "cascade";
   }

   @Override
   public void solve(RigFrame frame) {
      applyCascadeRotations(frame);
   }

   private static void applyCascadeRotations(RigFrame frame) {
      FaunaBase body = (FaunaBase)frame.body;
      FaunaSpineCascadeComponent cascade = body.componentRegistry.get(FaunaSpineCascadeComponent.class);
      if (cascade != null) {
         Set<String> chained = new HashSet<>();
         CascadeSwitch mode = CascadeSwitch.get();
         boolean skipChained = RigLod.of(body).chainActive()
            && (mode == CascadeSwitch.OFF || mode == CascadeSwitch.GROUND && (body.getTransportMode() == TransportMode.GROUNDED || body.isClimbing()));
         if (skipChained) {
            for (Entry<String, PointChain.Params> physics : frame.spec.physics().entrySet()) {
               chained.addAll(frame.spec.chain(physics.getKey()));
               if (physics.getValue().headLed()) {
                  chained.addAll(frame.spec.chain("head"));
               }
            }
         }

         for (String prefix : frame.spec.prefixes()) {
            for (FaunaSpineCascadeComponent.Segment seg : cascade.getSegments()) {
               if (!chained.contains(seg.bone())) {
                  String bone = prefix + seg.bone();
                  Vec3 offset = cascade.getAccumForBone(seg.bone(), frame.partialTick);
                  if (frame.write.has(bone)) {
                     float[] memo = cascade.writtenOn(bone);
                     addOnce(frame.write, bone, BoneAxis.ROT_X, (float)(offset.x * (float) (Math.PI / 180.0)), memo, 0);
                     addOnce(frame.write, bone, BoneAxis.ROT_Y, (float)(offset.y * (float) (Math.PI / 180.0)), memo, 1);
                     addOnce(frame.write, bone, BoneAxis.ROT_Z, (float)(offset.z * (float) (Math.PI / 180.0)), memo, 2);
                  }
               }
            }
         }
      }
   }

   static void addOnce(RigWrite write, String bone, BoneAxis axis, float delta, float[] memo, int i) {
      float cur = write.read(bone, axis);
      boolean untouched = !Float.isNaN(memo[i]) && Math.abs(cur - memo[i]) < 1.0E-6F;
      float base = untouched ? cur - memo[3 + i] : cur;
      write.add(bone, axis, base + delta - cur);
      memo[i] = write.read(bone, axis);
      memo[3 + i] = delta;
   }
}
