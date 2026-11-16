package rearth.oritech.port;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** 1.21 SavedData.Factory emulation. */
public record SavedDataFactory<T extends SavedData>(Supplier<T> constructor, BiFunction<CompoundTag, HolderLookup.Provider, T> deserializer, Object type) {

    public T get(DimensionDataStorage storage, String id) {
        return storage.computeIfAbsent(tag -> deserializer.apply(tag, PortUtil.registries()), constructor, id);
    }
}
