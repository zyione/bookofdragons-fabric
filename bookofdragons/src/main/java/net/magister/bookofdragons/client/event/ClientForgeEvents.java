package net.magister.bookofdragons.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.magister.bookofdragons.client.KeyBindings;
import net.magister.bookofdragons.client.gui.book.ClientDragonPages;
import net.magister.bookofdragons.client.gui.book.DragonPortrait;
import net.magister.bookofdragons.client.sound.DragonSoundClientHelper;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.custom.dragon.NightfuryEntity;
import net.magister.bookofdragons.network.ControlNetwork;
import net.magister.bookofdragons.network.Strip;
import net.magister.bookofdragons.network.message.OpenDragonGUIMessage;
import net.magister.liberanimalium.chassis.FaunaTick;
import net.magister.liberanimalium.compat.Meshes;
import net.magister.liberanimalium.compat.Verts;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.magister.liberanimalium.platform.event.RenderStage;
import net.magister.liberanimalium.platform.event.StageContext;
import net.magister.liberanimalium.qte.client.ClientQteState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ClientForgeEvents {
   private static final Map<UUID, Integer> playerAnimationTimers = new ConcurrentHashMap<>();
   private static final Map<UUID, float[]> playerAnimationData = new ConcurrentHashMap<>();
   private static boolean roostDebugEnabled = false;

   public static void clientInit() {
      net.magister.liberanimalium.platform.event.ClientEvents.KEY.register(ClientForgeEvents::onKeyInput);
      net.magister.liberanimalium.platform.event.ClientEvents.CLIENT_TICK_END.register(ClientForgeEvents::onClientTick);
      FaunaTick.LIVING.register(ClientForgeEvents::onLivingTick);
      net.magister.liberanimalium.platform.event.ClientEvents.RENDER_STAGE.register(ClientForgeEvents::onRenderLevelStage);
      CommonEvents.LEVEL_UNLOAD.register(ClientForgeEvents::onLevelUnload);
   }

   public static void onLevelUnload(LevelAccessor level) {
      if (level.isClientSide()) {
         ClientDragonPages.clear();
         DragonPortrait.clear();
      }
   }

   public static void onKeyInput(int key, int scanCode, int action, int modifiers) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         boolean opens = KeyBindings.OPEN_DRAGON_INVENTORY_KEY.consumeClick();
         if (opens && player.getVehicle() instanceof DragonBase dragon && !ClientQteState.tameCircleActiveFor(dragon.getId())) {
            ControlNetwork.sendToServer(new OpenDragonGUIMessage(dragon.getId()));
         }
      }
   }

   public static void onClientTick() {
      while (KeyBindings.ROOST_LINES_KEY.consumeClick()) {
         if (!Services.platform().isProduction()) {
            roostDebugEnabled = !roostDebugEnabled;
            Player player = Minecraft.getInstance().player;
            if (player != null) {
               Strip.say(player, Component.translatable(roostDebugEnabled ? "message.bookofdragons.roost_lines.on" : "message.bookofdragons.roost_lines.off"));
            }
         }
      }

      playerAnimationTimers.forEach((uuid, ticks) -> {
         if (ticks <= 1) {
            playerAnimationTimers.remove(uuid);
            playerAnimationData.remove(uuid);
         } else {
            playerAnimationTimers.put(uuid, ticks - 1);
            float animProgress = (20 - ticks) / 20.0F;
            float shakeIntensity = 1.0F - animProgress;
            float shakeAngle = (float)Math.sin(ticks.intValue() * 1.5) * 0.3F * shakeIntensity;
            float xRot = -1.2F;
            float yRot = shakeAngle;
            float zRot = shakeAngle * 0.5F;
            playerAnimationData.put(uuid, new float[]{xRot, yRot, zRot});
         }
      });
   }

   public static void onLivingTick(LivingEntity entity) {
      if (entity.level().isClientSide()) {
         if (entity instanceof NightfuryEntity nightfury) {
            DragonSoundClientHelper.tickNightfuryDiveSounds(nightfury);
         }
      }
   }

   public static void startRattlestaffAnimation(UUID uuid) {
      playerAnimationTimers.put(uuid, 20);
   }

   public static boolean isAnimationActive(UUID uuid) {
      return playerAnimationTimers.containsKey(uuid);
   }

   public static int getAnimationTicks(UUID uuid) {
      return playerAnimationTimers.getOrDefault(uuid, 0);
   }

   public static float[] getAnimationData(UUID uuid) {
      return playerAnimationData.get(uuid);
   }

   public static void onRenderLevelStage(StageContext event) {
      if (event.getStage() == RenderStage.AFTER_PARTICLES) {
         Minecraft mc = Minecraft.getInstance();
         if (roostDebugEnabled && mc.level != null) {
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.lineWidth(2.0F);
            RenderSystem.disableDepthTest();
            Camera camera = event.getCamera();
            Vec3 camPos = camera.getPosition();
            PoseStack poseStack = event.getPoseStack();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder buffer = Meshes.immediate(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

            for (Entity entity : mc.level.entitiesForRendering()) {
               if (entity instanceof DragonBase dragon && dragon.holdsRoost()) {
                  BlockPos roost = dragon.getRoostPosition();
                  Vec3 dragonPos = dragon.position().add(0.0, dragon.getBbHeight() * 0.5, 0.0);
                  Vec3 roostPos = Vec3.atCenterOf(roost);
                  Matrix4f matrix = poseStack.last().pose();
                  Verts.posColor(
                     buffer,
                     matrix,
                     (float)(dragonPos.x - camPos.x),
                     (float)(dragonPos.y - camPos.y),
                     (float)(dragonPos.z - camPos.z),
                     0.0F,
                     1.0F,
                     0.0F,
                     1.0F
                  );
                  Verts.posColor(
                     buffer,
                     matrix,
                     (float)(roostPos.x - camPos.x),
                     (float)(roostPos.y - camPos.y),
                     (float)(roostPos.z - camPos.z),
                     0.0F,
                     1.0F,
                     0.0F,
                     1.0F
                  );
               }
            }

            Meshes.draw(buffer);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
         }
      }
   }
}
