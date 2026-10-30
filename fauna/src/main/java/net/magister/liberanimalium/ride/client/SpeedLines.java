package net.magister.liberanimalium.ride.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.List;
import net.magister.liberanimalium.anim.rig.Ease;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.compat.Meshes;
import net.magister.liberanimalium.compat.Verts;
import net.magister.liberanimalium.config.RideSettings;
import net.magister.liberanimalium.platform.event.StageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class SpeedLines {
   private static final double FROM = 0.7;
   private static final double FULL = 1.7;
   private static final float RATE = 70.0F;
   private static final int MAX = 120;
   private static final double AHEAD_MIN = 0.5;
   private static final double AHEAD_MAX = 1.1;
   private static final double OFF_MIN = 1.4;
   private static final double OFF_MAX = 7.0;
   private static final double STRETCH = 0.12;
   private static final float HALF_WIDTH = 0.025F;
   private static final float ALPHA = 0.32F;
   private static final float RISE = 0.3F;
   private static final float FALL = 0.6F;
   private static final List<SpeedLines.Streak> STREAKS = new ArrayList<>();
   private static final RandomSource RANDOM = RandomSource.create();
   private static float strength;
   private static float owed;

   private SpeedLines() {
   }

   public static void update(FaunaBase body, float frameDelta) {
      float target = RideSettings.isSpeedLinesEnabled() ? want(body) : 0.0F;
      float tau = target > strength ? 0.3F : 0.6F;
      strength = strength + (target - strength) * Ease.lag(frameDelta, tau);
      Minecraft mc = Minecraft.getInstance();
      Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
      Vec3 v = body.getDeltaMovement();
      double speed = v.length();
      Vec3 dir = speed > 1.0E-4 ? v.scale(1.0 / speed) : Vec3.ZERO;
      STREAKS.removeIf(s -> {
         s.age += frameDelta;
         return s.age >= s.life || dir != Vec3.ZERO && s.head.subtract(camera).dot(dir) < -s.length - 2.0;
      });
      if (!(strength < 0.02F) && dir != Vec3.ZERO) {
         owed = owed + strength * 70.0F * frameDelta;
         double perSecond = speed * 20.0;
         Vec3 side = Math.abs(dir.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : dir.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
         Vec3 up = side.cross(dir).normalize();

         while (owed >= 1.0F && STREAKS.size() < 120) {
            owed--;
            double ahead = perSecond * Mth.lerp(RANDOM.nextDouble(), 0.5, 1.1);
            double angle = RANDOM.nextDouble() * (float) (Math.PI * 2);
            double off = Mth.lerp(Math.sqrt(RANDOM.nextDouble()), 1.4, 7.0);
            Vec3 at = camera.add(dir.scale(ahead)).add(side.scale(Math.cos(angle) * off)).add(up.scale(Math.sin(angle) * off));
            double length = Mth.clamp(perSecond * 0.12, 1.5, 10.0) * (0.7 + RANDOM.nextDouble() * 0.6);
            float life = (float)(1.5 + RANDOM.nextDouble() * 0.4);
            STREAKS.add(new SpeedLines.Streak(at, dir.reverse(), length, life));
         }

         if (owed > 1.0F) {
            owed = 1.0F;
         }
      } else {
         owed = 0.0F;
      }
   }

   public static void reset() {
      STREAKS.clear();
      strength = 0.0F;
      owed = 0.0F;
   }

   static float want(FaunaBase body) {
      if (body.isFlying() && !body.isAquatic()) {
         double reference = Math.max(0.05, PhysicsSheet.of(body).flight().rollReferenceSpeed());
         float t = (float)Mth.clamp((body.getDeltaMovement().length() / reference - 0.7) / 1.0, 0.0, 1.0);
         return t * t * (3.0F - 2.0F * t);
      } else {
         return 0.0F;
      }
   }

   public static void render(StageContext event) {
      if (!STREAKS.isEmpty()) {
         Vec3 camera = event.getCamera().getPosition();
         Matrix4f matrix = event.getPoseStack().last().pose();
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         BufferBuilder buffer = Meshes.immediate(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

         for (SpeedLines.Streak s : STREAKS) {
            float fade = Mth.sin((float) Math.PI * Mth.clamp(s.age / s.life, 0.0F, 1.0F));
            Vec3 tail = s.head.add(s.along.scale(s.length));
            Vec3 mid = s.head.add(s.along.scale(s.length * 0.5));
            double near = mid.distanceTo(camera);
            float alpha = 0.32F * fade * strength * (float)Mth.clamp((near - 1.0) / 2.0, 0.0, 1.0);
            if (!(alpha <= 0.004F)) {
               Vec3 toCamera = camera.subtract(mid);
               Vec3 width = s.along.cross(toCamera);
               if (!(width.lengthSqr() < 1.0E-8)) {
                  width = width.normalize().scale(0.025F);
                  ribbon(buffer, matrix, camera, s.head, mid, width, 0.0F, alpha);
                  ribbon(buffer, matrix, camera, mid, tail, width, alpha, 0.0F);
               }
            }
         }

         Meshes.draw(buffer);
         RenderSystem.enableCull();
         RenderSystem.depthMask(true);
         RenderSystem.disableBlend();
      }
   }

   private static void ribbon(BufferBuilder buffer, Matrix4f matrix, Vec3 camera, Vec3 from, Vec3 to, Vec3 width, float alphaFrom, float alphaTo) {
      Vec3 a = from.subtract(camera);
      Vec3 b = to.subtract(camera);
      Verts.posColor(
         buffer,
         matrix,
         (float)(a.x - width.x),
         (float)(a.y - width.y),
         (float)(a.z - width.z),
         1.0F,
         1.0F,
         1.0F,
         alphaFrom
      );
      Verts.posColor(
         buffer,
         matrix,
         (float)(a.x + width.x),
         (float)(a.y + width.y),
         (float)(a.z + width.z),
         1.0F,
         1.0F,
         1.0F,
         alphaFrom
      );
      Verts.posColor(
         buffer,
         matrix,
         (float)(b.x + width.x),
         (float)(b.y + width.y),
         (float)(b.z + width.z),
         1.0F,
         1.0F,
         1.0F,
         alphaTo
      );
      Verts.posColor(
         buffer,
         matrix,
         (float)(b.x - width.x),
         (float)(b.y - width.y),
         (float)(b.z - width.z),
         1.0F,
         1.0F,
         1.0F,
         alphaTo
      );
   }

   private static final class Streak {
      final Vec3 head;
      final Vec3 along;
      final double length;
      final float life;
      float age;

      Streak(Vec3 head, Vec3 along, double length, float life) {
         this.head = head;
         this.along = along;
         this.length = length;
         this.life = life;
      }
   }
}
