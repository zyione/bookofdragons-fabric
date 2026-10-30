package net.magister.liberanimalium.render.layer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.layer.AzRenderLayer;
import net.magister.liberanimalium.anim.rig.BoneMatrices;
import net.magister.liberanimalium.body.BabyArt;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.render.FaunaRenderTypes;
import net.magister.liberanimalium.render.FaunaRendererAz;
import net.magister.liberanimalium.render.FaunaRendererPipeline;
import net.magister.liberanimalium.render.gpu.LayeredConsumer;
import net.magister.liberanimalium.render.gpu.SpeciesBatches;
import net.magister.liberanimalium.render.membrane.MembraneDefinitions;
import net.magister.liberanimalium.render.membrane.MembraneFlex;
import net.magister.liberanimalium.render.membrane.MembraneMesher;
import net.magister.liberanimalium.render.membrane.MembranePanel;
import net.magister.liberanimalium.render.membrane.MembraneSeams;
import net.magister.liberanimalium.util.PerfClock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class WingMembraneLayer<T extends FaunaBase> implements AzRenderLayer<UUID, T>, BoneAudience {
   private final ThreadLocal<MembraneDefinitions.Species> frameDef = new ThreadLocal<>();
   private final MembraneMesher mesher = new MembraneMesher();
   private static final String CANVAS = "canvas";
   private static final PerfClock.Section CLOCK = PerfClock.client("membrane");
   private static final PerfClock.Section MESH = PerfClock.client("membrane.mesh");
   private static final PerfClock.Section FILL = PerfClock.client("membrane.fill");
   private final MembraneBones bones = new MembraneBones();

   static boolean isCanvas(String name) {
      int n = name.length() - "canvas".length();

      for (int i = 0; i <= n; i++) {
         if (name.regionMatches(true, i, "canvas", 0, "canvas".length())) {
            return true;
         }
      }

      return false;
   }

   public void preRender(AzRendererPipelineContext<UUID, T> context) {
      this.frameDef.set(panelsOf((FaunaBase)context.animatable()));
      AzBakedModel model = context.bakedModel();
      if (model != null && this.frameDef.get() != null) {
         for (String name : BoneNames.matching(model, "canvas", WingMembraneLayer::isCanvas)) {
            AzBone bone = model.getBoneOrNull(name);
            if (bone != null && !bone.isHidden()) {
               bone.setHidden(true);
            }
         }
      }
   }

   @Override
   public void wantedBones(AzRendererPipelineContext<UUID, ?> context, Set<String> into) {
   }

   public void renderForBone(AzRendererPipelineContext<UUID, T> context, AzBone bone) {
   }

   public void render(AzRendererPipelineContext<UUID, T> context) {
      long clock = CLOCK.start();

      try {
         this.renderTimed(context);
      } finally {
         CLOCK.end(clock);
      }
   }

   private MembraneBones.Resolved fillFromPalette(AzRendererPipelineContext<UUID, T> context, MembraneDefinitions.Species def) {
      if (context.rendererPipeline() instanceof FaunaRendererPipeline<?> entity) {
         long var10 = FILL.start();

         try {
            return this.bones.fill(((FaunaBase)context.animatable()).getId(), def, entity.modelRenderTranslations());
         } finally {
            FILL.end(var10);
         }
      } else {
         return null;
      }
   }

   MembraneBones.Resolved resolve(BoneMatrices layout, MembraneDefinitions.Species def) {
      return this.bones.resolve(layout, def);
   }

   static MembraneDefinitions.Species panelsOf(FaunaBase body) {
      String species = body.speciesId();
      MembraneDefinitions.Species young = BabyArt.young(body) ? MembraneDefinitions.get(species + "_baby") : null;
      return young != null ? young : MembraneDefinitions.get(species);
   }

   private void renderTimed(AzRendererPipelineContext<UUID, T> context) {
      T body = (T)context.animatable();
      if (!body.isInvisible()) {
         MembraneDefinitions.Species def = this.frameDef.get();
         if (def != null && !def.panels().isEmpty()) {
            RenderType bodyType = context.renderType();
            VertexConsumer consumer;
            if (!(FaunaRenderTypes.isSpecies(bodyType) && context.vertexConsumer() instanceof LayeredConsumer layered)) {
               ResourceLocation tex = FaunaRendererAz.getTextureLocationCached(body);
               consumer = context.multiBufferSource().getBuffer(FaunaRenderTypes.translucent(tex));
            } else if (SpeciesBatches.deferring()) {
               consumer = SpeciesBatches.membrane(FaunaRenderTypes.speciesOf(bodyType), layered.layer());
            } else {
               VertexConsumer current = context.multiBufferSource().getBuffer(bodyType);
               consumer = current == layered.builder() ? layered : new LayeredConsumer((BufferBuilder)current, layered.layer());
            }

            int light = context.packedLight();
            int overlay = context.packedOverlay();
            MembraneBones.Resolved r = this.fillFromPalette(context, def);
            if (r != null) {
               Map<String, Vector3f> positions = r.positions;
               Map<String, Matrix4f> mats = r.mats;
               MembraneFlex.FrameParams fp = MembraneFlex.beginFrame(body, context.partialTick());
               MembraneSeams.Pins pins = r.pins;
               List<MembranePanel> panels = def.panels();

               for (int i = 0; i < panels.size(); i++) {
                  MembranePanel panel = panels.get(i);
                  Vector3f p1 = positions.get(panel.corner()[0]);
                  Vector3f p2 = positions.get(panel.corner()[1]);
                  Vector3f p3 = positions.get(panel.corner()[2]);
                  Vector3f p4 = positions.get(panel.corner()[3]);
                  if (p1 != null && p2 != null && p3 != null && p4 != null) {
                     float creaseH = MembraneFlex.creaseHeight(panel, positions, p1, p2, p4, fp);
                     long t = MESH.start();

                     try {
                        this.mesher.draw(panel, mats, p1, p2, p3, p4, creaseH * pins.scale()[i], pins.u0()[i], pins.u1()[i], consumer, light, overlay);
                     } finally {
                        MESH.end(t);
                     }
                  }
               }
            }
         }
      }
   }
}
