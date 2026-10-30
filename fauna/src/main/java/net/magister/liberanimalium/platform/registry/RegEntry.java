package net.magister.liberanimalium.platform.registry;

import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

public interface RegEntry<T> extends Supplier<T> {
   @Override
   T get();

   ResourceLocation id();

   Holder<T> holder();
}
