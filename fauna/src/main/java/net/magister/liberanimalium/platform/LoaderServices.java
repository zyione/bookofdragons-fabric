package net.magister.liberanimalium.platform;

import net.magister.liberanimalium.platform.client.ClientServices;
import net.magister.liberanimalium.platform.net.NetworkFactory;
import net.magister.liberanimalium.platform.registry.ItemFactory;
import net.magister.liberanimalium.platform.registry.MenuFactory;
import net.magister.liberanimalium.platform.registry.RegistryFactory;
import net.magister.liberanimalium.platform.world.ChunkTickets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public interface LoaderServices {
   Platform platform();

   NetworkFactory network();

   RegistryFactory registries();

   ItemFactory items();

   MenuFactory menus();

   ChunkTickets chunkTickets(ResourceLocation var1);

   ClientServices client();

   boolean despawnEventAnswered(Mob var1);
}
