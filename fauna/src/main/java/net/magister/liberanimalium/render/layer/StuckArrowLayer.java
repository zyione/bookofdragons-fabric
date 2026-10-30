package net.magister.liberanimalium.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import mod.azure.azurelib.cache.object.GeoCube;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.layer.AzRenderLayer;
import net.magister.liberanimalium.body.StuckArrows;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.render.CubeMesh;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class StuckArrowLayer<T extends FaunaBase> implements AzRenderLayer<UUID, T>, BoneAudience {
   private static final float LEAN = 0.35F;
   private static Arrow shown;

   public void preRender(AzRendererPipelineContext<UUID, T> context) {
   }

   public void render(AzRendererPipelineContext<UUID, T> context) {
   }

   @Override
   public void wantedBones(AzRendererPipelineContext<UUID, ?> context, Set<String> into) {
      if (context.animatable() instanceof FaunaBase body) {
         List<StuckArrows.Arrow> arrows = StuckArrows.read((CompoundTag)body.getEntityData().get(FaunaBase.DATA_STUCK_ARROWS));
         if (!arrows.isEmpty()) {
            StuckArrowLayer.Surface surface = StuckArrowLayer.Surface.of(context.bakedModel());

            for (StuckArrows.Arrow arrow : arrows) {
               String bone = surface.boneFor(arrow);
               if (bone != null) {
                  into.add(bone);
               }
            }
         }
      }
   }

   public void renderForBone(@NotNull AzRendererPipelineContext<UUID, T> context, @NotNull AzBone bone) {
      T body = (T)context.animatable();
      List<StuckArrows.Arrow> arrows = StuckArrows.read((CompoundTag)body.getEntityData().get(FaunaBase.DATA_STUCK_ARROWS));
      if (!arrows.isEmpty()) {
         StuckArrowLayer.Surface surface = StuckArrowLayer.Surface.of(context.bakedModel());
         String name = bone.getName();

         for (StuckArrows.Arrow arrow : arrows) {
            if (name.equals(surface.boneFor(arrow))) {
               this.draw(context, bone, surface.boxes(name), arrow.seed());
            }
         }
      }
   }

   private void draw(AzRendererPipelineContext<UUID, T> context, AzBone bone, List<float[]> boxes, int seed) {
      RandomSource random = RandomSource.create(seed);
      Vector3f at = new Vector3f();
      Vector3f normal = new Vector3f();
      if (boxes.isEmpty()) {
         at.set(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);
         normal.set(0.0F, 1.0F, 0.0F);
      } else {
         onFace(pick(boxes, random), random, at, normal);
      }

      Vector3f in = new Vector3f(normal)
         .negate()
         .add((random.nextFloat() - 0.5F) * 0.35F, (random.nextFloat() - 0.5F) * 0.35F, (random.nextFloat() - 0.5F) * 0.35F);
      PoseStack pose = context.poseStack();
      pose.pushPose();
      pose.translate(at.x, at.y, at.z);
      Vector3f scale = pose.last().pose().getScale(new Vector3f());
      pose.scale(scale.x > 1.0E-4F ? 1.0F / scale.x : 1.0F, scale.y > 1.0E-4F ? 1.0F / scale.y : 1.0F, scale.z > 1.0E-4F ? 1.0F / scale.z : 1.0F);
      Arrow arrow = shown(((FaunaBase)context.animatable()).level());
      if (arrow != null) {
         float level = Mth.sqrt(in.x * in.x + in.z * in.z);
         arrow.setYRot((float)(Math.atan2(in.x, in.z) * 180.0F / (float)Math.PI));
         arrow.setXRot((float)(Math.atan2(in.y, level) * 180.0F / (float)Math.PI));
         arrow.yRotO = arrow.getYRot();
         arrow.xRotO = arrow.getXRot();
         arrow.setPos(((FaunaBase)context.animatable()).position());
         Minecraft.getInstance().getEntityRenderDispatcher().render(arrow, 0.0, 0.0, 0.0, 0.0F, 0.0F, pose, context.multiBufferSource(), context.packedLight());
      }

      pose.popPose();
   }

   private static float[] pick(List<float[]> boxes, RandomSource random) {
      float total = 0.0F;

      for (float[] box : boxes) {
         total += area(box);
      }

      float roll = random.nextFloat() * total;

      for (float[] box : boxes) {
         roll -= area(box);
         if (roll <= 0.0F) {
            return box;
         }
      }

      return boxes.get(boxes.size() - 1);
   }

   private static void onFace(float[] box, RandomSource random, Vector3f at, Vector3f normal) {
      float dx = box[3] - box[0];
      float dy = box[4] - box[1];
      float dz = box[5] - box[2];
      float yz = dy * dz;
      float xz = dx * dz;
      float xy = dx * dy;
      float roll = random.nextFloat() * (yz + xz + xy);
      int axis = roll < yz ? 0 : (roll < yz + xz ? 1 : 2);
      boolean high = random.nextBoolean();
      at.set(box[0] + random.nextFloat() * dx, box[1] + random.nextFloat() * dy, box[2] + random.nextFloat() * dz);
      normal.set(0.0F, 0.0F, 0.0F);
      float side = high ? 1.0F : -1.0F;
      switch (axis) {
         case 0:
            at.x = high ? box[3] : box[0];
            normal.x = side;
            break;
         case 1:
            at.y = high ? box[4] : box[1];
            normal.y = side;
            break;
         default:
            at.z = high ? box[5] : box[2];
            normal.z = side;
      }
   }

   private static float area(float[] box) {
      float dx = box[3] - box[0];
      float dy = box[4] - box[1];
      float dz = box[5] - box[2];
      return Math.max(1.0E-6F, 2.0F * (dx * dy + dy * dz + dx * dz));
   }

   private static Arrow shown(Level level) {
      if (shown == null || shown.level() != level) {
         shown = (Arrow)EntityType.ARROW.create(level);
      }

      return shown;
   }

   private static final class Surface {
      private static final Map<AzBakedModel, StuckArrowLayer.Surface> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
      private final Map<String, List<float[]>> byBone = new HashMap<>();
      private final List<String> bones = new ArrayList<>();
      private final List<Float> weights = new ArrayList<>();
      private float total;

      static StuckArrowLayer.Surface of(AzBakedModel model) {
         return CACHE.computeIfAbsent(model, StuckArrowLayer.Surface::new);
      }

      private Surface(AzBakedModel model) {
         Vector3f min = new Vector3f();
         Vector3f max = new Vector3f();

         for (Entry<String, AzBone> e : model.getBonesByName().entrySet()) {
            List<float[]> boxes = new ArrayList<>();
            float area = 0.0F;

            for (GeoCube cube : e.getValue().getCubes()) {
               min.set(Float.MAX_VALUE);
               max.set(-Float.MAX_VALUE);
               CubeMesh.of(cube).bounds(min, max);
               if (!(min.x > max.x)) {
                  float[] box = new float[]{min.x, min.y, min.z, max.x, max.y, max.z};
                  boxes.add(box);
                  area += StuckArrowLayer.area(box);
               }
            }

            this.byBone.put(e.getKey(), boxes);
            if (!boxes.isEmpty()) {
               this.bones.add(e.getKey());
               this.weights.add(area);
               this.total += area;
            }
         }
      }

      List<float[]> boxes(String bone) {
         return this.byBone.getOrDefault(bone, List.of());
      }

      String boneFor(StuckArrows.Arrow arrow) {
         if (!arrow.bone().isEmpty() && this.byBone.containsKey(arrow.bone())) {
            return arrow.bone();
         }

         if (this.bones.isEmpty()) {
            return null;
         }

         float roll = RandomSource.create(~arrow.seed()).nextFloat() * this.total;

         for (int i = 0; i < this.bones.size(); i++) {
            roll -= this.weights.get(i);
            if (roll <= 0.0F) {
               return this.bones.get(i);
            }
         }

         return this.bones.get(this.bones.size() - 1);
      }
   }
}
