package rearth.oritech.port;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * 1.21 data component stand-in. Values live in the stack NBT under {@link #key}, encoded with {@link #codec}.
 * Vanilla components the port needs override get/set/has/remove (see {@link DataComponents}).
 */
public class DataComponentType<T> implements Supplier<DataComponentType<T>> {
    public final String key;
    public final Codec<T> codec;

    public DataComponentType(String key, Codec<T> codec) {
        this.key = key;
        this.codec = codec;
    }

    @Override
    public DataComponentType<T> get() { return this; }

    public T get(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(key)) return null;
        return codec.parse(NbtOps.INSTANCE, tag.get(key)).result().orElse(null);
    }

    public void set(ItemStack stack, T value) {
        if (value == null) { remove(stack); return; }
        codec.encodeStart(NbtOps.INSTANCE, value).result().ifPresent(t -> stack.getOrCreateTag().put(key, t));
    }

    public boolean has(ItemStack stack) {
        var tag = stack.getTag();
        return tag != null && tag.contains(key);
    }

    public void remove(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null) return;
        tag.remove(key);
        if (tag.isEmpty()) stack.setTag(null);
    }

    public static <T> Builder<T> builder() { return new Builder<>(); }

    public static class Builder<T> {
        private Codec<T> codec;
        public Builder<T> persistent(Codec<T> codec) { this.codec = codec; return this; }
        public Builder<T> networkSynchronized(Object ignored) { return this; }
        public Builder<T> cacheEncoding() { return this; }
        public DataComponentType<T> build(String key) { return new DataComponentType<>(key, codec); }
    }
}
