package net.magister.liberanimalium.platform.fabric;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.magister.liberanimalium.platform.registry.MenuFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class FabricMenus implements MenuFactory {
    public <T extends AbstractContainerMenu> MenuType<T> type(Constructor<T> constructor) {
        return new ExtendedScreenHandlerType<>(constructor::create);
    }
    public void open(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> extraData) {
        player.openMenu(new ExtendedScreenHandlerFactory() {
            public void writeScreenOpeningData(ServerPlayer user, FriendlyByteBuf buf) { extraData.accept(buf); }
            public Component getDisplayName() { return provider.getDisplayName(); }
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player user) {
                return provider.createMenu(id, inventory, user);
            }
        });
    }
}
