package net.magister.bookofdragons.client.gui;

import java.util.Arrays;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.liberanimalium.chassis.component.command.Modifier;
import net.magister.liberanimalium.chassis.component.command.Stance;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.magister.liberanimalium.network.SetCommandMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CommandWheelScreen extends Screen {
   private static final int CARD_WIDTH = 84;
   private static final int CARD_HEIGHT = 100;
   private static final int CARD_GAP = 14;
   private static final int BACK_OFFSET_X = 6;
   private static final int BACK_OFFSET_Y = 6;
   private static final int TOP_MARGIN = 20;
   private static final float FLIP_DURATION = 12.0F;
   private static final float HOVER_DURATION = 7.0F;
   private static final float STANCE_FADE_DURATION = 8.0F;
   private static final int GLOW_RGB = 16766054;
   private static final int TEXT_PRIMARY = -1;
   private static final int TEXT_SECONDARY = -5592372;
   private static final int TEXT_DIM = -10461068;
   private static final int TEXT_GOLD = -11162;
   private final int dragonEntityId;
   private DragonBase dragon;
   private Stance lastSeenStance;
   private final Modifier[] lastSeenModifierForStance = new Modifier[Stance.values().length];
   private final Modifier[] outgoingModifierForStance = new Modifier[Stance.values().length];
   private float stanceTransitionStart = -1.0F;
   private final float[] flipStart = new float[Stance.values().length];
   private final float[] hoverProgress = new float[Stance.values().length];
   private float lastFrameTime = -1.0F;
   private int hoverIndex = -1;

   private static ResourceLocation getCardTexture(Modifier modifier) {
      return new ResourceLocation("bookofdragons", "textures/gui/command/card_" + modifier.name().toLowerCase() + ".png");
   }

   public CommandWheelScreen(int dragonEntityId) {
      super(Component.translatable("dragon.command.wheel.title"));
      this.dragonEntityId = dragonEntityId;
      Arrays.fill(this.flipStart, -1.0F);
   }

   public boolean isPauseScreen() {
      return false;
   }

   protected void init() {
      super.init();
      if (this.minecraft != null && this.minecraft.level != null && this.minecraft.level.getEntity(this.dragonEntityId) instanceof DragonBase d) {
         this.dragon = d;
         Stance s = this.readStance();
         Modifier m = this.readModifier();
         this.lastSeenStance = s;
         Arrays.fill(this.lastSeenModifierForStance, null);
         Arrays.fill(this.outgoingModifierForStance, null);
         this.lastSeenModifierForStance[s.ordinal()] = m;
      }
   }

   private Modifier[] getSavedModifiers() {
      int o = (Integer)this.dragon.getEntityData().get(DragonBase.getCommandModifierData());
      Modifier[] mods = new Modifier[Stance.values().length];
      mods[Stance.STAY.ordinal()] = (o & 8) != 0 ? Stance.STAY.secondaryModifier() : Stance.STAY.defaultModifier();
      mods[Stance.FOLLOW.ordinal()] = (o & 16) != 0 ? Stance.FOLLOW.secondaryModifier() : Stance.FOLLOW.defaultModifier();
      mods[Stance.FREE.ordinal()] = (o & 32) != 0 ? Stance.FREE.secondaryModifier() : Stance.FREE.defaultModifier();
      return mods;
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      if (this.dragon != null && this.dragon.isAlive()) {
         float now = this.currentTime(partialTick);
         float frameDt = this.lastFrameTime < 0.0F ? 0.05F : Math.max(0.0F, now - this.lastFrameTime);
         this.lastFrameTime = now;
         Stance currentStance = this.readStance();
         Modifier currentModifier = this.readModifier();
         boolean stanceChanged = false;
         if (this.lastSeenStance != currentStance) {
            this.stanceTransitionStart = now;
            this.lastSeenStance = currentStance;
            stanceChanged = true;
         }

         Modifier lastForActive = this.lastSeenModifierForStance[currentStance.ordinal()];
         if (!stanceChanged && lastForActive != null && lastForActive != currentModifier) {
            this.flipStart[currentStance.ordinal()] = now;
            this.outgoingModifierForStance[currentStance.ordinal()] = lastForActive;
         }

         this.lastSeenModifierForStance[currentStance.ordinal()] = currentModifier;
         Stance[] stances = Stance.values();
         int totalWidth = stances.length * 84 + (stances.length - 1) * 14;
         int startX = (this.width - totalWidth) / 2;
         int startY = 34;
         this.hoverIndex = this.computeHoverIndex(mouseX, mouseY, startX, startY, stances.length);
         String title = this.dragon.getDisplayName().getString();
         CardRenderer.drawCenteredText(g, this.font, title, 0, startY - 12, this.width, -1, true);

         for (int i = 0; i < stances.length; i++) {
            float target = i == this.hoverIndex ? 1.0F : 0.0F;
            this.hoverProgress[i] = approach(this.hoverProgress[i], target, frameDt / 7.0F);
         }

         for (int i = 0; i < stances.length; i++) {
            int x = startX + i * 98;
            this.renderColumn(g, stances[i], x, startY, now, currentStance, currentModifier);
         }
      } else {
         this.onClose();
      }
   }

   private void renderColumn(GuiGraphics g, Stance stance, int x, int y, float now, Stance activeStance, Modifier activeModifier) {
      int idx = stance.ordinal();
      boolean isActive = stance == activeStance;
      Modifier frontModifier = isActive ? activeModifier : this.getSavedModifiers()[idx];
      int liftY = -Math.round(3.0F * easeOutCubic(this.hoverProgress[idx]));
      if (isActive) {
         float fade = this.stanceTransitionStart < 0.0F ? 1.0F : Math.min(1.0F, (now - this.stanceTransitionStart) / 8.0F);
         fade = easeOutCubic(fade);
         int baseAlpha = (int)(64.0F * fade);

         for (int i = 1; i <= 4; i++) {
            int e = i * 3;
            int a = Math.max(0, baseAlpha - i * 8);
            int c = a << 24 | 16766054;
            g.fill(x - e, y + liftY - e, x + 84 + e, y + liftY + 100 + e, c);
         }
      }

      float fp = this.flipProgress(idx, now);
      boolean flipping = fp >= 0.0F && fp < 1.0F;
      if (flipping) {
         Modifier incomingModifier = frontModifier;
         Modifier outgoingModifier = this.outgoingModifierForStance[idx];
         if (outgoingModifier == null) {
            outgoingModifier = frontModifier;
         }

         float t = easeInOutCubic(fp);
         float startX = x;
         float startY = y + liftY;
         float backX = x + 6;
         float backY = y + 6 + liftY;
         float ctrlX = startX + 3.0F;
         float ctrlY = startY - 24.0F;
         float outX = (1.0F - t) * (1.0F - t) * startX + 2.0F * (1.0F - t) * t * ctrlX + t * t * backX;
         float outY = (1.0F - t) * (1.0F - t) * startY + 2.0F * (1.0F - t) * t * ctrlY + t * t * backY;
         float inX = backX + (startX - backX) * t;
         float inY = backY + (startY - backY) * t;
         if (t < 0.5F) {
            this.renderFullCard(g, inX, inY, stance, incomingModifier, isActive, t, false);
            this.renderFullCard(g, outX, outY, stance, outgoingModifier, isActive, 1.0F - t, true);
         } else {
            this.renderFullCard(g, outX, outY, stance, outgoingModifier, isActive, 1.0F - t, false);
            this.renderFullCard(g, inX, inY, stance, incomingModifier, isActive, t, true);
         }
      } else {
         int bx = x + 6;
         int by = y + 6 + liftY;
         Modifier backModifier = otherModifier(stance, frontModifier);
         g.setColor(0.5F, 0.5F, 0.5F, 1.0F);
         g.blit(getCardTexture(backModifier), bx, by, 0.0F, 0.0F, 84, 100, 84, 100);
         g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         this.renderFrontCard(g, x, y + liftY, stance, frontModifier, isActive);
      }
   }

   private void renderFrontCard(GuiGraphics g, int x, int y, Stance stance, Modifier modifier, boolean isActive) {
      int w = 84;
      int h = 100;
      g.blit(getCardTexture(modifier), x, y, 0.0F, 0.0F, w, h, w, h);
      int textPrimary = -1;
      int accent = isActive ? -11162 : -5592372;
      CardRenderer.drawCenteredText(g, this.font, stance.name(), x, y + 4, w, isActive ? -15066592 : textPrimary, true);
      CardRenderer.drawCenteredText(g, this.font, modifier.name(), x, y + 70, w, accent, true);
      String desc = describeModifier(stance, modifier);
      if (desc != null && !desc.isEmpty()) {
         int maxW = w - 8;
         String shown = this.trimToWidth(desc, maxW);
         CardRenderer.drawCenteredText(g, this.font, shown, x, y + h - 14, w, -10461068, false);
      }
   }

   private void renderFullCard(GuiGraphics g, float x, float y, Stance stance, Modifier modifier, boolean isActiveColumn, float frontness, boolean drawText) {
      int w = 84;
      int h = 100;
      int ix = Math.round(x);
      int iy = Math.round(y);
      float brightness = 0.5F + 0.5F * frontness;
      g.setColor(brightness, brightness, brightness, 1.0F);
      g.blit(getCardTexture(modifier), ix, iy, 0.0F, 0.0F, w, h, w, h);
      g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (drawText) {
         int textPrimary = -1;
         int accent = isActiveColumn ? -11162 : -5592372;
         CardRenderer.drawCenteredText(g, this.font, stance.name(), ix, iy + 4, w, isActiveColumn ? -15066592 : textPrimary, true);
         CardRenderer.drawCenteredText(g, this.font, modifier.name(), ix, iy + 70, w, accent, true);
         String desc = describeModifier(stance, modifier);
         if (desc != null && !desc.isEmpty()) {
            int maxW = w - 8;
            String shown = this.trimToWidth(desc, maxW);
            CardRenderer.drawCenteredText(g, this.font, shown, ix, iy + h - 14, w, -10461068, false);
         }
      }
   }

   private static float easeInOutCubic(float t) {
      return t < 0.5F ? 4.0F * t * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 3.0) / 2.0F;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0 && button != 1) {
         return super.mouseClicked(mouseX, mouseY, button);
      }

      if (this.hoverIndex < 0) {
         return false;
      }

      Stance clicked = Stance.values()[this.hoverIndex];
      Stance active = this.readStance();
      Modifier modifierToSet;
      if (clicked == active) {
         Modifier current = this.readModifier();
         modifierToSet = otherModifier(clicked, current);
      } else {
         modifierToSet = this.getSavedModifiers()[clicked.ordinal()];
      }

      FaunaNetwork.sendToServer(new SetCommandMessage(this.dragon.getUUID(), clicked, modifierToSet));
      return true;
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 1) {
         if (this.hoverIndex >= 0) {
            Stance clicked = Stance.values()[this.hoverIndex];
            Stance active = this.readStance();
            if (clicked != active) {
               Modifier mod = this.getSavedModifiers()[clicked.ordinal()];
               FaunaNetwork.sendToServer(new SetCommandMessage(this.dragon.getUUID(), clicked, mod));
            }
         }

         this.onClose();
         return true;
      } else {
         return super.mouseReleased(mouseX, mouseY, button);
      }
   }

   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(null);
      }
   }

   private int computeHoverIndex(int mouseX, int mouseY, int startX, int startY, int cardCount) {
      for (int i = 0; i < cardCount; i++) {
         int x = startX + i * 98;
         int liftY = -Math.round(3.0F * easeOutCubic(this.hoverProgress[i]));
         int y = startY + liftY;
         if (mouseX >= x && mouseX < x + 84 && mouseY >= y && mouseY < y + 100) {
            return i;
         }
      }

      return -1;
   }

   private float flipProgress(int idx, float now) {
      if (this.flipStart[idx] < 0.0F) {
         return 1.0F;
      } else {
         float p = (now - this.flipStart[idx]) / 12.0F;
         if (p >= 1.0F) {
            this.flipStart[idx] = -1.0F;
            return 1.0F;
         } else {
            return Math.max(0.0F, p);
         }
      }
   }

   private Stance readStance() {
      int o = (Integer)this.dragon.getEntityData().get(DragonBase.getCommandData());
      return Stance.fromOrdinal(o);
   }

   private Modifier readModifier() {
      int o = (Integer)this.dragon.getEntityData().get(DragonBase.getCommandModifierData());
      Modifier m = Modifier.fromOrdinal(o & 7);
      Stance s = this.readStance();
      return s.accepts(m) ? m : s.defaultModifier();
   }

   private static Modifier otherModifier(Stance s, Modifier current) {
      return current == s.defaultModifier() ? s.secondaryModifier() : s.defaultModifier();
   }

   private float currentTime(float partialTick) {
      long t = this.minecraft != null && this.minecraft.level != null ? this.minecraft.level.getGameTime() : 0L;
      return (float)t + partialTick;
   }

   private static float approach(float current, float target, float maxStep) {
      return target > current ? Math.min(target, current + maxStep) : Math.max(target, current - maxStep);
   }

   private static float easeOutCubic(float t) {
      float u = 1.0F - t;
      return 1.0F - u * u * u;
   }

   private String trimToWidth(String text, int maxWidth) {
      if (this.font.width(text) <= maxWidth) {
         return text;
      }

      String ellipsis = "…";
      int eW = this.font.width(ellipsis);
      StringBuilder sb = new StringBuilder();

      for (char c : text.toCharArray()) {
         sb.append(c);
         if (this.font.width(sb.toString()) + eW > maxWidth) {
            sb.deleteCharAt(sb.length() - 1);
            sb.append(ellipsis);
            return sb.toString();
         }
      }

      return text;
   }

   private static String describeModifier(Stance stance, Modifier mod) {
      return switch (stance) {
         case STAY -> mod == Modifier.REST ? "sit and rest" : "patrol home";
         case FOLLOW -> mod == Modifier.TRAIL ? "loose follow" : "tight heel";
         case FREE -> mod == Modifier.GROUND ? "ground only" : "near roost";
         default -> throw new IncompatibleClassChangeError();
      };
   }
}
