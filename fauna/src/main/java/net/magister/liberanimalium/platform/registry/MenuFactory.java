package net.magister.liberanimalium.platform.registry;

import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public interface MenuFactory {
   <T extends AbstractContainerMenu> MenuType<T> type(MenuFactory.Constructor<T> var1);

   void open(ServerPlayer var1, MenuProvider var2, Consumer<FriendlyByteBuf> var3);

   @FunctionalInterface
   interface Constructor<T extends AbstractContainerMenu> {
      T create(int var1, Inventory var2, FriendlyByteBuf var3);
   }
}
