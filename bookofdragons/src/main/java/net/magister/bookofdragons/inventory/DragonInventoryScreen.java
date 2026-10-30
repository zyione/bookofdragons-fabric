package net.magister.bookofdragons.inventory;

import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DragonInventoryScreen extends AbstractContainerScreen<DragonInventoryMenu> {
   private static final ResourceLocation DRAGON_INVENTORY_LOCATION = new ResourceLocation("bookofdragons", "textures/gui/dragon_inventory.png");
   private final DragonBase dragon;
   private float xMouse;
   private float yMouse;

   public DragonInventoryScreen(DragonInventoryMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.dragon = menu.getDragon();
      this.imageWidth = 176;
      this.imageHeight = 166;
   }

   protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
      int x = (this.width - this.imageWidth) / 2;
      int y = (this.height - this.imageHeight) / 2;
      guiGraphics.blit(DRAGON_INVENTORY_LOCATION, x, y, 0, 0, this.imageWidth, this.imageHeight);
      if (this.dragon != null) {
         int entityX = x + 51;
         int entityY = y + 60;
         int entitySize = 17;
         InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, entityX, entityY, entitySize, entityX - this.xMouse, entityY - 50 - this.yMouse, this.dragon);
      }

      if (this.dragon != null && this.dragon.hasChest()) {
         guiGraphics.blit(DRAGON_INVENTORY_LOCATION, x + 79, y + 17, 0, 166, 90, 54);
      }

      guiGraphics.blit(DRAGON_INVENTORY_LOCATION, x + 4, y + 36, 18, 220, 18, 18);
      guiGraphics.blit(DRAGON_INVENTORY_LOCATION, x + 4, y + 18, 0, 220, 18, 18);
   }

   protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      Component dragonName = this.dragon != null ? this.dragon.getDisplayName() : this.title;
      guiGraphics.drawString(this.font, dragonName, 8, 6, 4210752, false);
      guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(guiGraphics);
      this.xMouse = mouseX;
      this.yMouse = mouseY;
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      this.renderTooltip(guiGraphics, mouseX, mouseY);
   }
}
