package net.magister.liberanimalium.platform;

import net.magister.liberanimalium.platform.client.ClientServices;
import net.magister.liberanimalium.platform.fabric.FabricServices;
import net.magister.liberanimalium.platform.net.NetworkFactory;
import net.magister.liberanimalium.platform.registry.ItemFactory;
import net.magister.liberanimalium.platform.registry.MenuFactory;
import net.magister.liberanimalium.platform.registry.RegistryFactory;
import net.magister.liberanimalium.platform.world.ChunkTickets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public final class Services {
   public static final LoaderServices LOADER = new FabricServices();

   private Services() {
   }

   public static Platform platform() {
      return LOADER.platform();
   }

   public static NetworkFactory network() {
      return LOADER.network();
   }

   public static RegistryFactory registries() {
      return LOADER.registries();
   }

   public static ItemFactory items() {
      return LOADER.items();
   }

   public static MenuFactory menus() {
      return LOADER.menus();
   }

   public static ChunkTickets chunkTickets(ResourceLocation id) {
      return LOADER.chunkTickets(id);
   }

   public static ClientServices client() {
      return LOADER.client();
   }

   public static boolean despawnEventAnswered(Mob mob) {
      return LOADER.despawnEventAnswered(mob);
   }
}
