package net.magister.bookofdragons.platform.fabric;
import net.fabricmc.api.ClientModInitializer;
import net.magister.bookofdragons.BookOfDragons;
public final class BookOfDragonsClient implements ClientModInitializer {
    public void onInitializeClient() { SeparateItemModels.register(); BookOfDragons.clientInit(); }
}
