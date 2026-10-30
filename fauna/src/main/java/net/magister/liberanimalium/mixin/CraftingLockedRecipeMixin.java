package net.magister.liberanimalium.mixin;

import net.magister.liberanimalium.progression.LockedRecipes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CraftingMenu.class)
public abstract class CraftingLockedRecipeMixin {
   @Redirect(
      method = "slotChangedCraftingGrid",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/inventory/ResultContainer;setRecipeUsed(Lnet/minecraft/world/level/Level;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/crafting/Recipe;)Z"
      )
   )
   private static boolean liberanimalium$lockedRecipe(ResultContainer result, Level level, ServerPlayer player, Recipe<?> recipe) {
      return LockedRecipes.mayCraft(player, recipe.getId()) && result.setRecipeUsed(level, player, recipe);
   }
}
