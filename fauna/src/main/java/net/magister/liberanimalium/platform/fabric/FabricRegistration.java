package net.magister.liberanimalium.platform.fabric;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.magister.liberanimalium.platform.registry.*;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/** Keeps reference lazy registrations until both modules have declared content. */
public final class FabricRegistration implements RegistryFactory {
    private static final List<Entry<?>> PENDING = new ArrayList<>();
    public static void flush() {
        for (int i = 0; i < PENDING.size(); i++) PENDING.get(i).get();
        PENDING.clear();
    }
    @Override
    public <R> Registrar<R> create(ResourceKey<? extends Registry<R>> key, String modId) {
        @SuppressWarnings("unchecked")
        Registry<R> registry = (Registry<R>) BuiltInRegistries.REGISTRY.get(key.location());
        if (registry == null) throw new IllegalArgumentException("Unknown registry " + key.location());
        return new Registrar<R>() {
            @Override public <T extends R> RegEntry<T> register(String name, Supplier<? extends T> factory) {
                Entry<T> entry = new Entry<>((Registry<T>) registry, new ResourceLocation(modId, name), factory);
                PENDING.add(entry);
                return entry;
            }
        };
    }
    private static final class Entry<T> implements RegEntry<T> {
        private final Registry<T> registry;
        private final ResourceLocation id;
        private final Supplier<? extends T> factory;
        private T value;
        private boolean constructing;
        Entry(Registry<T> registry, ResourceLocation id, Supplier<? extends T> factory) {
            this.registry = registry; this.id = id; this.factory = factory;
        }
        @Override public T get() {
            if (value == null) {
                if (constructing) throw new IllegalStateException("Cyclic registration " + id);
                constructing = true;
                try { value = Registry.register(registry, id, factory.get()); }
                finally { constructing = false; }
            }
            return value;
        }
        @Override public ResourceLocation id() { return id; }
        @Override public Holder<T> holder() {
            get();
            return registry.getHolder(ResourceKey.create(registry.key(), id)).orElseThrow();
        }
    }
}
