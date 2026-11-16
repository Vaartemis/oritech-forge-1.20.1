package rearth.oritech.port;

import net.minecraft.world.item.ItemStack;

import java.util.function.UnaryOperator;

/** Call-site helpers replacing ItemStack#get/set/has/remove(DataComponentType) from 1.21. */
public final class PortComponents {
    private PortComponents() { }

    public static <T> T get(ItemStack stack, DataComponentType<T> type) { return type.get(stack); }

    public static <T> T getOrDefault(ItemStack stack, DataComponentType<T> type, T fallback) {
        var v = type.get(stack);
        return v == null ? fallback : v;
    }

    public static <T> T set(ItemStack stack, DataComponentType<T> type, T value) {
        var old = type.get(stack);
        type.set(stack, value);
        return old;
    }

    public static <T> boolean has(ItemStack stack, DataComponentType<T> type) { return type.has(stack); }

    public static <T> T remove(ItemStack stack, DataComponentType<T> type) {
        var old = type.get(stack);
        type.remove(stack);
        return old;
    }

    public static <T> T update(ItemStack stack, DataComponentType<T> type, T fallback, UnaryOperator<T> op) {
        var v = op.apply(getOrDefault(stack, type, fallback));
        type.set(stack, v);
        return v;
    }
}
