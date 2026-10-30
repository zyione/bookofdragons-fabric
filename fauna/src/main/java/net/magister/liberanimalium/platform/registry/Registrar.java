package net.magister.liberanimalium.platform.registry;

import java.util.function.Supplier;

public interface Registrar<R> {
   <T extends R> RegEntry<T> register(String var1, Supplier<? extends T> var2);
}
