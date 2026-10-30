package net.magister.liberanimalium.platform.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public interface RegistryFactory {
   <R> Registrar<R> create(ResourceKey<? extends Registry<R>> var1, String var2);
}
