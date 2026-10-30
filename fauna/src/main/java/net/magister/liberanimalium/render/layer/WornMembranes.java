package net.magister.liberanimalium.render.layer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.render.BonePalettes;
import net.magister.liberanimalium.render.FaunaRendererPipeline;
import net.magister.liberanimalium.render.membrane.MembraneDefinitions;
import net.magister.liberanimalium.render.membrane.MembraneFlex;
import net.magister.liberanimalium.render.membrane.MembraneMesher;
import net.magister.liberanimalium.render.membrane.MembranePanel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class WornMembranes {
   private static final MembraneBones BONES = new MembraneBones();
   private static final MembraneMesher MESHER = new MembraneMesher();

   private WornMembranes() {
   }

   public static <T extends FaunaBase> void draw(
      AzRendererPipelineContext<UUID, T> context, Predicate<MembranePanel> worn, float lift, VertexConsumer out, int light, int overlay
   ) {
      if (context.rendererPipeline() instanceof FaunaRendererPipeline<?> entity) {
         T body = (T)context.animatable();
         MembraneDefinitions.Species def = WingMembraneLayer.panelsOf(body);
         if (def != null && !def.panels().isEmpty()) {
            MembraneBones.Resolved r = BONES.fill(body.getId(), def, entity.modelRenderTranslations());
            if (r != null) {
               MembraneFlex.FrameParams fp = MembraneFlex.beginFrame(body, context.partialTick());
               List<MembranePanel> panels = def.panels();

               for (int i = 0; i < panels.size(); i++) {
                  MembranePanel panel = panels.get(i);
                  if (worn.test(panel)) {
                     Vector3f p1 = r.positions.get(panel.corner()[0]);
                     Vector3f p2 = r.positions.get(panel.corner()[1]);
                     Vector3f p3 = r.positions.get(panel.corner()[2]);
                     Vector3f p4 = r.positions.get(panel.corner()[3]);
                     if (p1 != null && p2 != null && p3 != null && p4 != null) {
                        float creaseH = MembraneFlex.creaseHeight(panel, r.positions, p1, p2, p4, fp);
                        MESHER.drawOver(
                           panel, r.mats, p1, p2, p3, p4, creaseH * r.pins.scale()[i], r.pins.u0()[i], r.pins.u1()[i], r.upSide[i], lift, out, light, overlay
                        );
                     }
                  }
               }
            }
         }
      }
   }

   public static void upwardPoints(
      FaunaBase entity,
      float partialTick,
      Predicate<MembranePanel> worn,
      float lift,
      float minUp,
      float perPanel,
      Vec3 body,
      RandomSource random,
      Consumer<Vec3> out
   ) {
      MembraneDefinitions.Species def = WingMembraneLayer.panelsOf(entity);
      Matrix4f toWorld = BonePalettes.modelToWorld(entity.getId());
      if (def != null && !def.panels().isEmpty() && toWorld != null) {
         MembraneBones.Resolved r = BONES.fill(entity.getId(), def, toWorld);
         if (r != null) {
            MembraneFlex.FrameParams fp = MembraneFlex.beginFrame(entity, partialTick);
            Vector3f point = new Vector3f();
            List<MembranePanel> panels = def.panels();

            for (int i = 0; i < panels.size(); i++) {
               MembranePanel panel = panels.get(i);
               int count = (int)perPanel + (random.nextFloat() < perPanel - (int)perPanel ? 1 : 0);
               if (count != 0 && worn.test(panel)) {
                  Vector3f p1 = r.positions.get(panel.corner()[0]);
                  Vector3f p2 = r.positions.get(panel.corner()[1]);
                  Vector3f p3 = r.positions.get(panel.corner()[2]);
                  Vector3f p4 = r.positions.get(panel.corner()[3]);
                  if (p1 != null && p2 != null && p3 != null && p4 != null) {
                     float creaseH = MembraneFlex.creaseHeight(panel, r.positions, p1, p2, p4, fp);

                     for (int k = 0; k < count; k++) {
                        MESHER.pointOver(
                           panel,
                           r.mats,
                           p1,
                           p2,
                           p3,
                           p4,
                           creaseH * r.pins.scale()[i],
                           r.pins.u0()[i],
                           r.pins.u1()[i],
                           r.upSide[i],
                           lift,
                           random.nextFloat(),
                           random.nextFloat(),
                           point
                        );
                        if (MESHER.normal().y() < minUp) {
                           break;
                        }

                        out.accept(new Vec3(body.x + point.x(), body.y + point.y(), body.z + point.z()));
                     }
                  }
               }
            }
         }
      }
   }
}
