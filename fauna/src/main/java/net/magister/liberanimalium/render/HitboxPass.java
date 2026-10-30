package net.magister.liberanimalium.render;

import java.util.List;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import net.magister.liberanimalium.anim.rig.RigFrame;
import net.magister.liberanimalium.anim.rig.RigPass;
import net.magister.liberanimalium.body.PartSpec;
import net.magister.liberanimalium.body.anim.data.PerEntityHeadMatrixExtractor;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.FaunaMultipartComponent;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

final class HitboxPass implements RigPass {
   private static final PerfClock.Section CLOCK_PARTS = PerfClock.client("parts.observe");

   @Override
   public String name() {
      return "hitbox";
   }

   @Override
   public void solve(RigFrame frame) {
      extractAndSyncMultipartHitboxes(frame);
   }

   private static void extractAndSyncMultipartHitboxes(RigFrame frame) {
      observeParts((FaunaBase)frame.body, frame.model);
   }

   private static void observeParts(FaunaBase body, AzBakedModel model) {
      long clock = CLOCK_PARTS.start();

      try {
         observePartsTimed(body, model);
      } finally {
         CLOCK_PARTS.end(clock);
      }
   }

   private static void observePartsTimed(FaunaBase body, AzBakedModel model) {
      BonePalettes.update(body.getId(), model, FaunaProceduralAnimator.modelToWorld());
      if (body.level().isClientSide()) {
         FaunaMultipartComponent multipart = body.componentRegistry.get(FaunaMultipartComponent.class);
         if (multipart != null && body.isMultipartEntity()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
               List<String> slots = multipart.plan().reportSlots();
               Vec3[] points = new Vec3[slots.size()];
               Vec3 centre = body.position().add(0.0, body.getBbHeight() * 0.5, 0.0);

               for (int i = 0; i < points.length; i++) {
                  String slot = slots.get(i);
                  if ("muzzle".equals(slot)) {
                     PerEntityHeadMatrixExtractor.HeadPositionData head = PerEntityHeadMatrixExtractor.getHeadPositionData(
                        body.getId(), body.level().getGameTime()
                     );
                     if (head != null && head.mouthPosition != null) {
                        points[i] = head.mouthPosition;
                     } else {
                        List<PartSpec> heads = multipart.plan().heads();
                        AzBone headBone = heads.isEmpty() ? null : model.getBoneOrNull(heads.get(0).bone());
                        if (headBone == null) {
                           points[i] = centre;
                        } else {
                           Vec3 world = BonePalettes.worldPosition(body.getId(), headBone.getName(), body.position());
                           if (world == null) {
                              return;
                           }

                           points[i] = world;
                        }
                     }
                  } else {
                     AzBone bone = model.getBoneOrNull(slot);
                     if (bone == null) {
                        points[i] = centre;
                     } else {
                        Vec3 world = BonePalettes.worldPosition(body.getId(), slot, body.position());
                        if (world == null) {
                           return;
                        }

                        points[i] = world;
                     }
                  }
               }

               multipart.observe(points, player.getId());
            }
         }
      }
   }
}
