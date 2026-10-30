package net.magister.liberanimalium.platform.registry;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Item.Properties;

public interface ItemFactory {
   SpawnEggItem spawnEgg(Supplier<? extends EntityType<? extends Mob>> var1, int var2, int var3, Properties var4);
}
