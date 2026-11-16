package rearth.oritech.port;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** The few vanilla 1.21 components Oritech touches, mapped onto 1.20.1 stack NBT. */
public final class DataComponents {
    private DataComponents() { }

    /** Per-stack max stack size override; read back by items through PortStack#maxStackSize. */
    public static final DataComponentType<Integer> MAX_STACK_SIZE = new DataComponentType<>("oritech:max_stack", Codec.INT);

    /** Vanilla "Unbreakable" flag. */
    public static final DataComponentType<Boolean> UNBREAKABLE = new DataComponentType<>("Unbreakable", Codec.BOOL) {
        @Override public void set(ItemStack stack, Boolean value) {
            if (value == null || !value) remove(stack); else stack.getOrCreateTag().putBoolean(key, true);
        }
        @Override public Boolean get(ItemStack stack) { return has(stack) ? stack.getTag().getBoolean(key) : null; }
    };

    /** Whole custom NBT of the stack. */
    public static final DataComponentType<CompoundTag> CUSTOM_DATA = new DataComponentType<>("", CompoundTag.CODEC) {
        @Override public CompoundTag get(ItemStack stack) { return stack.getTag(); }
        @Override public void set(ItemStack stack, CompoundTag value) { stack.setTag(value); }
        @Override public boolean has(ItemStack stack) { return stack.hasTag(); }
        @Override public void remove(ItemStack stack) { stack.setTag(null); }
    };

    /** Item enchantments ("Enchantments" list). */
    public static final DataComponentType<ItemEnchantments> ENCHANTMENTS = new DataComponentType<>("Enchantments", null) {
        @Override public ItemEnchantments get(ItemStack stack) { return ItemEnchantments.fromMap(net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack)); }
        @Override public void set(ItemStack stack, ItemEnchantments value) {
            if (stack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)) { var t = stack.getTag(); if (t != null) t.remove(key); }
            net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(value == null ? java.util.Map.of() : value.toMap(), stack);
        }
        @Override public boolean has(ItemStack stack) { return stack.hasTag() && stack.getTag().contains(key, 9); }
    };

    /** Enchanted book contents ("StoredEnchantments"). */
    public static final DataComponentType<ItemEnchantments> STORED_ENCHANTMENTS = new DataComponentType<>("StoredEnchantments", null) {
        @Override public ItemEnchantments get(ItemStack stack) {
            if (!has(stack)) return null;
            return ItemEnchantments.fromMap(net.minecraft.world.item.enchantment.EnchantmentHelper.deserializeEnchantments(net.minecraft.world.item.EnchantedBookItem.getEnchantments(stack)));
        }
        @Override public void set(ItemStack stack, ItemEnchantments value) {
            remove(stack);
            if (value != null) value.toMap().forEach((e, l) -> net.minecraft.world.item.EnchantedBookItem.addEnchantment(stack, new net.minecraft.world.item.enchantment.EnchantmentInstance(e, l)));
        }
        @Override public boolean has(ItemStack stack) { return stack.hasTag() && stack.getTag().contains(key, 9); }
    };

    /** Marker used by the promethium pick to remember a temporary enchant. */
    public static final DataComponentType<net.minecraft.util.Unit> INTANGIBLE_PROJECTILE = new DataComponentType<>("oritech:temp_ench", Codec.unit(net.minecraft.util.Unit.INSTANCE));
}
