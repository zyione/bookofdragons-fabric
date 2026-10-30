package net.magister.bookofdragons.client.gui.book;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.cache.AzBakedModelCache;
import net.magister.bookofdragons.compat.ModelView;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.liberanimalium.anim.rig.BoneMatrices;
import net.magister.liberanimalium.body.BabyArt;
import net.magister.liberanimalium.body.BodyPlan;
import net.magister.liberanimalium.body.BodyPlans;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.compat.Verts;
import net.magister.liberanimalium.genetics.client.GeneticsTextureCache;
import net.magister.liberanimalium.render.ModelMesh;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class DragonPortrait {
   private static final Logger LOGGER = LogManager.getLogger("DragonPortrait");
   private static final int SIZE = 128;
   private static final float YAW = 145.0F;
   private static final float PITCH = 12.0F;
   private static final float FILL = 0.72F;
   private static final Map<UUID, DragonPortrait.Picture> PICTURES = new HashMap<>();
   private static final Set<Integer> FAILED = new HashSet<>();

   private DragonPortrait() {
   }

   public static void clear() {
      for (DragonPortrait.Picture picture : PICTURES.values()) {
         Minecraft.getInstance().getTextureManager().release(picture.texture());
      }

      PICTURES.clear();
      FAILED.clear();
   }

   public static void draw(GuiGraphics graphics, UUID uuid, DragonType type, int stage, CompoundTag genetics, int x, int y, int w, int h) {
      int size = Math.min(w, h);
      int px = x + (w - size) / 2;
      int py = y + (h - size) / 2;
      DragonPortrait.Picture picture = genetics == null ? null : picture(graphics, uuid, type, stage, genetics);
      if (picture == null) {
         BookRenderHelper.renderScaledTexture(graphics, speciesPortrait(type), px, py, size, size, 128, 128);
      } else {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         graphics.blit(picture.texture(), px, py, size, size, 0.0F, 0.0F, 128, 128, 128, 128);
         RenderSystem.disableBlend();
      }
   }

   private static DragonPortrait.Picture picture(GuiGraphics graphics, UUID uuid, DragonType type, int stage, CompoundTag genetics) {
      int key = Objects.hash(type, stage, genetics);
      DragonPortrait.Picture known = PICTURES.get(uuid);
      if (known != null && known.key() == key) {
         return known;
      }

      if (FAILED.contains(key)) {
         return null;
      }

      graphics.flush();
      NativeImage image = null;

      try {
         image = paint(type, stage, genetics);
      } catch (RuntimeException e) {
         LOGGER.warn("[DragonPortrait] {} cannot be drawn, showing its species: {}", uuid, e.toString());
      }

      if (image == null) {
         FAILED.add(key);
         return null;
      } else {
         Minecraft mc = Minecraft.getInstance();
         ResourceLocation texture = new ResourceLocation("bookofdragons", "portrait/" + uuid);
         mc.getTextureManager().release(texture);
         mc.getTextureManager().register(texture, new DynamicTexture(image));
         DragonPortrait.Picture picture = new DragonPortrait.Picture(texture, key);
         PICTURES.put(uuid, picture);
         return picture;
      }
   }

   private static NativeImage paint(DragonType type, int stage, CompoundTag genetics) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return null;
      }

      if (type.getEntityType().create(mc.level) instanceof DragonBase body) {
         body.setGrowthStage(stage);
         if (!genetics.isEmpty()) {
            body.getEntityData().set(FaunaBase.getGenotypeData(), genetics.copy());
         }

         AzBakedModel model = AzBakedModelCache.getInstance().getNullable(SpeciesAssets.locate("geo/" + BabyArt.modelKey(body) + ".geo.json"));
         if (model == null) {
            return null;
         }

         BoneMatrices layout = BoneMatrices.of(model);
         boolean[] head = headBones(layout, body.speciesId());
         Matrix4f[] palette = layout.newPalette();
         layout.compute(palette);
         ModelMesh mesh = ModelMesh.of(model);
         Matrix4f turn = new Matrix4f().rotateX((float)Math.toRadians(12.0)).rotateY((float)Math.toRadians(145.0));
         float[] bounds = bounds(mesh, palette, head, turn);
         if (bounds == null) {
            return null;
         }

         float span = Math.max(bounds[3] - bounds[0], bounds[4] - bounds[1]);
         if (span <= 1.0E-4F) {
            return null;
         }

         float scale = 92.16F / span;
         Matrix4f view = new Matrix4f()
            .translate(64.0F, 64.0F, 0.0F)
            .scale(scale, -scale, scale)
            .translate(-(bounds[0] + bounds[3]) / 2.0F, -(bounds[1] + bounds[4]) / 2.0F, -(bounds[2] + bounds[5]) / 2.0F)
            .mul(turn);
         ResourceLocation skin = body.hasGenotype()
            ? GeneticsTextureCache.INSTANCE.getOrCompose(body.getGenotype(), body.speciesId(), null, body.getPaletteDraws(), BabyArt.young(body))
            : BabyArt.plainTexture(body);
         return capture(mesh, palette, head, view, skin);
      } else {
         return null;
      }
   }

   private static boolean[] headBones(BoneMatrices layout, String species) {
      BodyPlan plan = BodyPlans.of(species);
      List<String> chain = plan.chain("head");
      String root = chain.isEmpty() ? "head0" : chain.get(0);
      boolean[] head = new boolean[layout.size()];

      for (String prefix : plan.prefixes()) {
         int at = layout.indexOf(prefix + root);
         if (at >= 0) {
            head[at] = true;
         }
      }

      for (int i = 0; i < head.length; i++) {
         int parent = layout.parentOf(i);
         if (parent >= 0 && head[parent]) {
            head[i] = true;
         }
      }

      return head;
   }

   private static float[] bounds(ModelMesh mesh, Matrix4f[] palette, boolean[] head, Matrix4f turn) {
      float[] pos = mesh.positions();
      int[] bones = mesh.bones();
      float[] box = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
      Matrix4f m = new Matrix4f();
      Vector3f v = new Vector3f();
      boolean any = false;

      for (int q = 0; q < mesh.quads(); q++) {
         if (head[bones[q]]) {
            turn.mul(palette[bones[q]], m);

            for (int i = 0; i < 4; i++) {
               int p = (q * 4 + i) * 3;
               m.transformPosition(pos[p], pos[p + 1], pos[p + 2], v);
               box[0] = Math.min(box[0], v.x);
               box[1] = Math.min(box[1], v.y);
               box[2] = Math.min(box[2], v.z);
               box[3] = Math.max(box[3], v.x);
               box[4] = Math.max(box[4], v.y);
               box[5] = Math.max(box[5], v.z);
               any = true;
            }
         }
      }

      return any ? box : null;
   }

   private static NativeImage capture(ModelMesh mesh, Matrix4f[] palette, boolean[] head, Matrix4f view, ResourceLocation skin) {
      Minecraft mc = Minecraft.getInstance();
      TextureTarget target = new TextureTarget(128, 128, true, Minecraft.ON_OSX);
      target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
      target.clear(Minecraft.ON_OSX);
      target.bindWrite(true);
      RenderSystem.backupProjectionMatrix();
      RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, 128.0F, 128.0F, 0.0F, -1000.0F, 1000.0F), VertexSorting.ORTHOGRAPHIC_Z);
      ModelView.pushIdentity();
      Lighting.setupForEntityInInventory();

      try {
         BufferSource buffers = mc.renderBuffers().bufferSource();
         VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(skin));
         emit(out, mesh, palette, head, view);
         buffers.endBatch();
         NativeImage image = new NativeImage(128, 128, false);
         RenderSystem.bindTexture(target.getColorTextureId());
         image.downloadTexture(0, false);
         image.flipY();
         return image;
      } finally {
         Lighting.setupFor3DItems();
         ModelView.pop();
         RenderSystem.restoreProjectionMatrix();
         target.destroyBuffers();
         mc.getMainRenderTarget().bindWrite(true);
      }
   }

   private static void emit(VertexConsumer out, ModelMesh mesh, Matrix4f[] palette, boolean[] head, Matrix4f view) {
      float[] pos = mesh.positions();
      float[] nrm = mesh.normals();
      float[] uv = mesh.uv();
      int[] bones = mesh.bones();
      byte[] flips = mesh.flips();
      Matrix3f viewNormal = new Matrix3f(view).invert().transpose();
      Matrix3f identity = new Matrix3f();
      Matrix4f m = new Matrix4f();
      Vector3f n = new Vector3f();
      int light = 15728880;
      int overlay = OverlayTexture.NO_OVERLAY;

      for (int q = 0; q < mesh.quads(); q++) {
         int b = bones[q];
         if (head[b]) {
            view.mul(palette[b], m);
            n.set(nrm[q * 3], nrm[q * 3 + 1], nrm[q * 3 + 2]);
            palette[b].transformDirection(n);
            int f = flips[q];
            if (n.x < 0.0F && (f & 1) != 0) {
               n.x = -n.x;
            }

            if (n.y < 0.0F && (f & 2) != 0) {
               n.y = -n.y;
            }

            if (n.z < 0.0F && (f & 4) != 0) {
               n.z = -n.z;
            }

            viewNormal.transform(n).normalize();

            for (int i = 0; i < 4; i++) {
               int p = (q * 4 + i) * 3;
               int t = (q * 4 + i) * 2;
               Verts.entity(out, m, identity, pos[p], pos[p + 1], pos[p + 2], 1.0F, 1.0F, 1.0F, 1.0F, uv[t], uv[t + 1], overlay, light, n.x, n.y, n.z);
            }
         }
      }
   }

   private static ResourceLocation speciesPortrait(DragonType type) {
      return new ResourceLocation("bookofdragons", "textures/gui/book/dragon_portrait_" + type.getSerializedName().replace("_", "") + ".png");
   }

   private record Picture(ResourceLocation texture, int key) {
   }
}
