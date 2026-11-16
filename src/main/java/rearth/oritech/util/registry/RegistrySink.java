package rearth.oritech.util.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Where registry containers put their entries. On Forge 1.20.1 the active RegisterEvent is installed here
 * while the matching registry is open; entries for other registries are queued until theirs opens.
 */
public final class RegistrySink {
    private RegistrySink() { }

    public interface Target {
        <T> void register(ResourceKey<? extends Registry<T>> key, ResourceLocation id, Supplier<T> value);
    }

    private record Pending(ResourceKey<? extends Registry<?>> key, ResourceLocation id, Supplier<?> value) { }

    private static Target target;
    private static ResourceKey<? extends Registry<?>> openKey;
    private static final List<Pending> PENDING = new ArrayList<>();

    public static void open(ResourceKey<? extends Registry<?>> key, Target t) {
        openKey = key;
        target = t;
        var it = PENDING.iterator();
        while (it.hasNext()) {
            var p = it.next();
            if (p.key().equals(key)) {
                put(p);
                it.remove();
            }
        }
    }

    public static void close() {
        openKey = null;
        target = null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void put(Pending p) {
        target.register((ResourceKey) p.key(), p.id(), (Supplier) p.value());
    }

    public static <T> Supplier<T> register(ResourceKey<? extends Registry<T>> key, ResourceLocation id, Supplier<T> value) {
        var p = new Pending(key, id, value);
        if (target != null && key.equals(openKey)) put(p);
        else PENDING.add(p);
        return value;
    }

    public static List<String> leftovers() {
        var out = new ArrayList<String>();
        for (var p : PENDING) out.add(p.key().location() + " -> " + p.id());
        return out;
    }
}
