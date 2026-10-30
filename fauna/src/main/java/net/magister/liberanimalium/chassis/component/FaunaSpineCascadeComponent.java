package net.magister.liberanimalium.chassis.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.magister.liberanimalium.anim.rig.Cascade;
import net.magister.liberanimalium.anim.rig.CascadeSpec;
import net.magister.liberanimalium.body.anim.binding.AnimationBindingLoader;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class FaunaSpineCascadeComponent implements IFaunaComponent {
   private FaunaBase body;
   private List<FaunaSpineCascadeComponent.Segment> segments = List.of();
   private Vec3[] accum = new Vec3[0];
   private Vec3[] prevAccum = new Vec3[0];
   private final Map<String, Integer> boneIndex = new HashMap<>();
   private CascadeSpec resolvedFrom;
   private final Map<String, float[]> written = new HashMap<>();
   public int debugTickCount = 0;
   public Vec3 debugLastOmega = Vec3.ZERO;

   public float[] writtenOn(String bone) {
      return this.written.computeIfAbsent(bone, b -> new float[]{Float.NaN, Float.NaN, Float.NaN, 0.0F, 0.0F, 0.0F});
   }

   @Override
   public void onAttach(FaunaBase host) {
      this.body = host;
   }

   private void resolve() {
      CascadeSpec spec = AnimationBindingLoader.get(this.body.animBindingKey()).rig.cascade();
      if (spec != this.resolvedFrom) {
         this.resolvedFrom = spec;
         List<FaunaSpineCascadeComponent.Segment> built = new ArrayList<>();

         for (CascadeSpec.Group group : spec.groups()) {
            for (String bone : group.bones()) {
               CascadeSpec.Row row = group.rowFor(bone);
               if (row != null) {
                  built.add(new FaunaSpineCascadeComponent.Segment(bone, group.lead(), false, false, row.strength(), row.decay(), row.maxBend()));
               }
            }
         }

         for (Entry<String, CascadeSpec.Limb> entry : spec.limbs()) {
            CascadeSpec.Limb limb = entry.getValue();
            built.add(new FaunaSpineCascadeComponent.Segment(entry.getKey(), false, true, limb.left(), limb.strength(), limb.decay(), limb.maxBend()));
         }

         this.segments = List.copyOf(built);
         this.accum = new Vec3[this.segments.size()];
         this.prevAccum = new Vec3[this.segments.size()];
         this.boneIndex.clear();

         for (int i = 0; i < this.segments.size(); i++) {
            this.accum[i] = Vec3.ZERO;
            this.prevAccum[i] = Vec3.ZERO;
            this.boneIndex.put(this.segments.get(i).bone(), i);
         }
      }
   }

   @Override
   public void clientTick() {
      FaunaFlightRotationComponent rotComp = this.body.componentRegistry.get(FaunaFlightRotationComponent.class);
      if (rotComp != null) {
         this.resolve();
         if (!this.segments.isEmpty()) {
            Vec3 omega = rotComp.getLocalOmegaAtLag(0, 0.0F);
            this.debugLastOmega = omega;
            this.debugTickCount++;
            boolean grounded = !this.body.isFlying();

            for (int i = 0; i < this.segments.size(); i++) {
               FaunaSpineCascadeComponent.Segment s = this.segments.get(i);
               this.prevAccum[i] = this.accum[i];
               this.accum[i] = s.limb()
                  ? Cascade.limb(this.accum[i], omega, s.strength(), s.decay(), s.maxBend(), s.left(), grounded)
                  : Cascade.spine(this.accum[i], omega, s.strength(), s.decay(), s.maxBend(), s.lead());
            }
         }
      }
   }

   public Vec3 getAccumForBone(String boneName, float partialTick) {
      Integer idx = this.boneIndex.get(boneName);
      if (idx == null) {
         return Vec3.ZERO;
      }

      Vec3 p = this.prevAccum[idx];
      Vec3 c = this.accum[idx];
      return new Vec3(
         Mth.lerp(partialTick, p.x, c.x),
         Mth.lerp(partialTick, p.y, c.y),
         Mth.lerp(partialTick, p.z, c.z)
      );
   }

   public List<FaunaSpineCascadeComponent.Segment> getSegments() {
      return this.segments;
   }

   @Override
   public void save(CompoundTag tag) {
   }

   @Override
   public void load(CompoundTag tag) {
   }

   public record Segment(String bone, boolean lead, boolean limb, boolean left, double strength, double decay, double maxBend) {
   }
}
