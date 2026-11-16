package rearth.oritech.util.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Minimal deferred register feeding {@link RegistrySink}; entries are created once, on first get(). */
public final class PortRegister<T> {
    private final String modId;
    private final ResourceKey<? extends Registry<T>> key;
    private final List<PortSupplier<? extends T>> entries = new ArrayList<>();

    private PortRegister(String modId, ResourceKey<? extends Registry<T>> key) {
        this.modId = modId;
        this.key = key;
    }

    public static <T> PortRegister<T> create(String modId, ResourceKey<? extends Registry<T>> key) {
        return new PortRegister<>(modId, key);
    }

    public <R extends T> PortSupplier<R> register(String name, Supplier<R> factory) {
        var s = new PortSupplier<R>(key.location(), new ResourceLocation(modId, name), factory);
        entries.add(s);
        return s;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void register() {
        for (var e : entries) RegistrySink.register((ResourceKey) key, e.getId(), (Supplier) e);
    }

    public static final class PortSupplier<R> implements dev.architectury.registry.registries.RegistrySupplier<R> {
        private final ResourceLocation registryId;
        private final ResourceLocation id;
        private final Supplier<R> factory;
        private R value;

        PortSupplier(ResourceLocation registryId, ResourceLocation id, Supplier<R> factory) {
            this.registryId = registryId;
            this.id = id;
            this.factory = factory;
        }

        @Override
        public synchronized R get() {
            if (value == null) value = factory.get();
            return value;
        }

        @Override public ResourceLocation getId() { return id; }
        @Override public ResourceLocation getRegistryId() { return registryId; }
        @Override public dev.architectury.registry.registries.RegistrarManager getRegistrarManager() { return dev.architectury.registry.registries.RegistrarManager.get(id.getNamespace()); }
        @Override public dev.architectury.registry.registries.Registrar<R> getRegistrar() { throw new UnsupportedOperationException("oritech port supplier " + id); }
        @Override public boolean isPresent() { return true; }
    }
}
