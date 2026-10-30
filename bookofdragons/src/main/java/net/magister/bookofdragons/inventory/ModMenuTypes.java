package net.magister.bookofdragons.inventory;

import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.registry.RegEntry;
import net.magister.liberanimalium.platform.registry.Registrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public class ModMenuTypes {
   public static final Registrar<MenuType<?>> MENU_TYPES = Services.registries().create(Registries.MENU, "bookofdragons");
   public static final RegEntry<MenuType<DragonInventoryMenu>> DRAGON_INVENTORY_MENU = MENU_TYPES.register(
      "dragon_inventory", () -> Services.menus().type(DragonInventoryMenu::new)
   );

   public static void register() {
   }
}
