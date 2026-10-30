package net.magister.bookofdragons.client.gui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Set;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.liberanimalium.attack.data.delivery.DeliveryConfig;
import net.magister.liberanimalium.attack.data.delivery.StreamDelivery;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.component.ranged.OmniAttackHandler;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.magister.liberanimalium.platform.event.HudLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public final class RiderHud {
   private static final ResourceLocation GUI_ICONS = new ResourceLocation("bookofdragons", "textures/gui/rider_hud.png");
   private static final ResourceLocation PRIMARY_ICON = new ResourceLocation("bookofdragons", "textures/gui/rider_primary_icon.png");
   private static final ResourceLocation SECONDARY_ICON = new ResourceLocation("bookofdragons", "textures/gui/rider_secondary_icon.png");
   private static final ResourceLocation ABILITY_BACKING = new ResourceLocation("bookofdragons", "textures/gui/ability_backing.png");
   private static final ResourceLocation STAMINA_BAR = new ResourceLocation("bookofdragons", "textures/gui/staminabar.png");
   private static final int ICON_SIZE = 9;
   private static final int ICONS_PER_ROW = 10;
   private static final int HEALTH_PER_ICON = 10;
   private static final int HUNGER_PER_ICON = 10;
   private static final int Y_OFFSET = 10;
   private static float lastHealth = -1.0F;
   private static long lastDamageTime = 0L;
   private static long lastRegenTime = 0L;
   static final Set<HudLayer> HIDDEN = Set.of(
      HudLayer.MOUNT_HEALTH, HudLayer.PLAYER_HEALTH, HudLayer.ARMOR_LEVEL, HudLayer.FOOD_LEVEL, HudLayer.EXPERIENCE_BAR, HudLayer.EXPERIENCE_LEVEL
   );

   private RiderHud() {
   }

   public static void clientInit() {
      ClientEvents.HUD_LAYER_PRE.register(RiderHud::hides);
      ClientEvents.HUD_AFTER_HOTBAR.register(RiderHud::render);
   }

   public static boolean hides(HudLayer type) {
      Minecraft mc = Minecraft.getInstance();
      return mc.player != null && mc.player.getVehicle() instanceof DragonBase ? HIDDEN.contains(type) : false;
   }

   static int fill(float value, int icon, int perIcon) {
      float in = value - icon * perIcon;
      return in <= 0.0F ? 0 : (in <= perIcon / 2 ? 1 : 2);
   }

   public static void render(GuiGraphics guiGraphics, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player != null && !mc.options.hideGui && player.getVehicle() instanceof DragonBase dragon) {
         int var13 = mc.getWindow().getGuiScaledWidth();
         int screenHeight = mc.getWindow().getGuiScaledHeight();
         float currentHealth = dragon.getHealth();
         long gameTime = dragon.level().getGameTime();
         if (lastHealth > 0.0F && currentHealth < lastHealth) {
            lastDamageTime = gameTime;
         }

         if (lastHealth > 0.0F && currentHealth > lastHealth) {
            lastRegenTime = gameTime;
         }

         lastHealth = currentHealth;
         boolean damageFlash = gameTime - lastDamageTime < 10L;
         boolean regenBounce = gameTime - lastRegenTime < 10L;
         boolean lowHealth = currentHealth <= dragon.getMaxHealth() * 0.2F;
         RenderSystem.enableBlend();
         renderHealthBar(guiGraphics, dragon, var13, screenHeight, damageFlash, regenBounce, lowHealth, gameTime);
         renderHungerBar(guiGraphics, dragon, var13, screenHeight, gameTime);
         renderPrimaryAttack(guiGraphics, dragon, var13, screenHeight);
         renderSecondaryAbility(guiGraphics, dragon, var13, screenHeight);
         renderStaminaBar(guiGraphics, dragon, var13, screenHeight);
         RenderSystem.disableBlend();
      } else {
         lastHealth = -1.0F;
      }
   }

   private static void renderHealthBar(
      GuiGraphics guiGraphics, FaunaBase dragon, int screenWidth, int screenHeight, boolean damageFlash, boolean regenBounce, boolean lowHealth, long gameTime
   ) {
      float health = dragon.getHealth();
      float maxHealth = dragon.getMaxHealth();
      int totalIcons = (int)Math.ceil(maxHealth / 10.0F);
      int startX = screenWidth / 2 - 91;
      int baseY = screenHeight - 39 - 10 - 9;
      int healthY = 0;
      int containerIndex = damageFlash ? 1 : 0;
      int containerU = 16 + containerIndex * 9;
      int heartTypeIndex;
      if (dragon.hasEffect(MobEffects.POISON)) {
         heartTypeIndex = 1;
      } else if (dragon.hasEffect(MobEffects.WITHER)) {
         heartTypeIndex = 2;
      } else if (dragon.isFullyFrozen()) {
         heartTypeIndex = 4;
      } else {
         heartTypeIndex = 0;
      }

      for (int i = 0; i < totalIcons; i++) {
         int row = i / 10;
         int col = i % 10;
         int x = startX + col * 8;
         int y = baseY - row * 10;
         if (lowHealth) {
            y += (int)(Math.sin(gameTime * 0.5 + i) * 2.0);
         }

         if (regenBounce && i == (int)(health / 10.0F)) {
            y -= 2;
         }

         guiGraphics.blit(GUI_ICONS, x, y, containerU, healthY, 9, 9);

         int baseHeartU = switch (heartTypeIndex) {
            case 1 -> 88;
            case 2 -> 124;
            case 3 -> 160;
            case 4 -> 178;
            default -> 52;
         };
         if (damageFlash && heartTypeIndex <= 2) {
            baseHeartU += 18;
         }

         int fill = fill(health, i, 10);
         if (fill > 0) {
            guiGraphics.blit(GUI_ICONS, x, y, baseHeartU + (fill == 1 ? 9 : 0), healthY, 9, 9);
         }
      }
   }

   private static void renderHungerBar(GuiGraphics guiGraphics, FaunaBase dragon, int screenWidth, int screenHeight, long gameTime) {
      int hunger = (Integer)dragon.getEntityData().get(FaunaBase.getHungerLevelData());
      int maxHunger = 100;
      int totalIcons = maxHunger / 10;
      int startX = screenWidth / 2 + 91;
      int baseY = screenHeight - 39 - 10 - 9;
      boolean hungerEffect = dragon.hasEffect(MobEffects.HUNGER);
      boolean lowHunger = hunger <= 0 && dragon.tickCount % 20 < 10;
      int hungerY = 27;

      for (int i = 0; i < totalIcons; i++) {
         int row = i / 10;
         int col = i % 10;
         int x = startX - (col + 1) * 8;
         int y = baseY - row * 10;
         if (hungerEffect) {
            if (dragon.getRandom().nextInt(10) == 0) {
               y += dragon.getRandom().nextInt(3) - 1;
            }
         } else if (lowHunger) {
            y += (int)(Math.sin(gameTime * 0.3 + i * 0.5) * 1.0);
         }

         int containerU = hungerEffect ? 25 : 16;
         guiGraphics.blit(GUI_ICONS, x, y, containerU, hungerY, 9, 9);
         int baseFoodU = hungerEffect ? 88 : 52;
         int fill = fill(hunger, i, 10);
         if (fill > 0) {
            guiGraphics.blit(GUI_ICONS, x, y, baseFoodU + (fill == 1 ? 9 : 0), hungerY, 9, 9);
         }
      }
   }

   private static void renderPrimaryAttack(GuiGraphics guiGraphics, FaunaBase dragon, int screenWidth, int screenHeight) {
      int cooldownTicks = 0;
      int maxCooldownTicks = 1;
      boolean isActive = false;
      OmniAttackHandler attackHandler = (OmniAttackHandler)dragon.componentRegistry.get(OmniAttackHandler.class);
      if (attackHandler != null && attackHandler.hasAttack()) {
         DeliveryConfig delivery = attackHandler.definition().delivery();
         if (delivery instanceof StreamDelivery stream) {
            cooldownTicks = attackHandler.getFullRechargeCooldownTicks();
            maxCooldownTicks = Math.max(1, stream.fuel().rechargeTicks());
            isActive = attackHandler.isBreathing() || attackHandler.isWindingUp();
         } else {
            int fireCooldown = Math.max(1, delivery.cooldownTicks());
            cooldownTicks = Math.max(0, fireCooldown - attackHandler.getTicksSinceLastFire());
            maxCooldownTicks = fireCooldown;
            isActive = attackHandler.isCharging() || attackHandler.isFiring();
         }
      }

      int x = screenWidth / 2 - 91 - 29 - 29;
      int y = screenHeight - 23;
      renderAbilitySlot(guiGraphics, x, y, cooldownTicks, maxCooldownTicks, isActive, PRIMARY_ICON);
   }

   private static void renderSecondaryAbility(GuiGraphics guiGraphics, FaunaBase dragon, int screenWidth, int screenHeight) {
      int cooldownTicks = (Integer)dragon.getEntityData().get(FaunaBase.getSecondaryCooldownData());
      int maxCooldownTicks = Math.max(1, (Integer)dragon.getEntityData().get(FaunaBase.getSecondaryCooldownMaxData()));
      int x = screenWidth / 2 + 91 + 7;
      int y = screenHeight - 23;
      renderAbilitySlot(guiGraphics, x, y, cooldownTicks, maxCooldownTicks, false, SECONDARY_ICON);
   }

   private static void renderAbilitySlot(
      GuiGraphics guiGraphics, int x, int y, int cooldownTicks, int maxCooldownTicks, boolean isSelected, ResourceLocation icon
   ) {
      int size = 22;
      guiGraphics.blit(ABILITY_BACKING, x, y, 0.0F, 0.0F, size, size, size, size);
      if (icon != null) {
         guiGraphics.blit(icon, x + 3, y + 3, 0.0F, 0.0F, 16, 16, 16, 16);
      }

      if (isSelected) {
         guiGraphics.fill(x - 1, y - 1, x + size + 1, y, -1);
         guiGraphics.fill(x - 1, y + size, x + size + 1, y + size + 1, -1);
         guiGraphics.fill(x - 1, y, x, y + size, -1);
         guiGraphics.fill(x + size, y, x + size + 1, y + size, -1);
      }

      if (cooldownTicks > 0 && maxCooldownTicks > 0) {
         float progress = (float)cooldownTicks / maxCooldownTicks;
         int overlayHeight = (int)((size - 4) * progress);
         int overlayY = y + size - 2 - overlayHeight;
         guiGraphics.fill(x + 2, overlayY, x + size - 2, y + size - 2, -1426063361);
      }
   }

   private static void renderStaminaBar(GuiGraphics guiGraphics, FaunaBase dragon, int screenWidth, int screenHeight) {
      float staminaPct = (Float)dragon.getEntityData().get(FaunaBase.getStaminaData())
         / Math.max(1.0F, (Float)dragon.getEntityData().get(FaunaBase.getMaxStaminaData()));
      int textureWidth = 112;
      int textureHeight = 25;
      int fillWidthMax = 83;
      int fillHeight = 10;
      int baseX = screenWidth / 2 - textureWidth / 2;
      int baseY = screenHeight - 22 - textureHeight;
      int fillX = baseX + 14;
      int fillY = baseY + 9;
      int color;
      if (staminaPct <= 0.2F) {
         float pulse = (float)(Math.sin(dragon.tickCount * 0.3) * 0.3 + 0.7);
         int r = (int)(255.0F * pulse);
         color = 0xFF000000 | r << 16 | 8704;
      } else if (staminaPct <= 0.5F) {
         color = -22016;
      } else {
         color = -16720572;
      }

      int currentFillWidth = (int)(fillWidthMax * staminaPct);
      if (currentFillWidth > 0) {
         guiGraphics.fill(fillX, fillY, fillX + currentFillWidth, fillY + fillHeight, color);
      }

      guiGraphics.blit(STAMINA_BAR, baseX, baseY, 0.0F, 0.0F, textureWidth, textureHeight, textureWidth, textureHeight);
   }
}
