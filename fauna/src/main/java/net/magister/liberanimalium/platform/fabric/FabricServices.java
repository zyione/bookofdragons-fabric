package net.magister.liberanimalium.platform.fabric;

import java.util.Comparator;
import net.magister.liberanimalium.platform.*;
import net.magister.liberanimalium.platform.client.ClientServices;
import net.magister.liberanimalium.platform.net.NetworkFactory;
import net.magister.liberanimalium.platform.registry.*;
import net.magister.liberanimalium.platform.world.ChunkTickets;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.ChunkPos;

public final class FabricServices implements LoaderServices {
    private final Platform platform = new FabricPlatform();
    private final NetworkFactory network = new FabricNetwork();
    private final RegistryFactory registries = new FabricRegistration();
    private final ItemFactory items = (type, background, highlight, properties) -> new SpawnEggItem(type.get(), background, highlight, properties);
    private final MenuFactory menus = new FabricMenus();
    public Platform platform() { return platform; }
    public NetworkFactory network() { return network; }
    public RegistryFactory registries() { return registries; }
    public ItemFactory items() { return items; }
    public MenuFactory menus() { return menus; }
    public ChunkTickets chunkTickets(ResourceLocation id) {
        TicketType<BlockPos> type = TicketType.create(id.toString(), Comparator.comparingLong(BlockPos::asLong));
        return (level, owner, x, z, add, ticking) -> {
            ChunkPos chunk = new ChunkPos(x, z);
            int radius = ticking ? 2 : 0;
            if (add) level.getChunkSource().addRegionTicket(type, chunk, radius, owner.immutable());
            else level.getChunkSource().removeRegionTicket(type, chunk, radius, owner.immutable());
            return true;
        };
    }
    public ClientServices client() { return Client.SERVICES; }
    public boolean despawnEventAnswered(Mob mob) {
        InteractionResult result = FabricDespawnEvent.EVENT.invoker().allowDespawn(mob);
        if (result == InteractionResult.SUCCESS) mob.discard();
        return result != InteractionResult.PASS;
    }
    private static final class Client {
        static final ClientServices SERVICES = new FabricClientServices();
    }
}
