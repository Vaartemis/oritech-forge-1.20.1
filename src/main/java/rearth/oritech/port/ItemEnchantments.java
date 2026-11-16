package rearth.oritech.port;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** 1.21 ItemEnchantments emulation over the 1.20.1 enchantment map. Keys are registry holders. */
public final class ItemEnchantments {
    public static final ItemEnchantments EMPTY = new ItemEnchantments(Map.of());

    private final Map<Holder<Enchantment>, Integer> levels;

    private ItemEnchantments(Map<Holder<Enchantment>, Integer> levels) {
        this.levels = Collections.unmodifiableMap(new LinkedHashMap<>(levels));
    }

    public static Holder<Enchantment> holder(Enchantment e) {
        return BuiltInRegistries.ENCHANTMENT.wrapAsHolder(e);
    }

    public static ItemEnchantments fromMap(Map<Enchantment, Integer> map) {
        var m = new LinkedHashMap<Holder<Enchantment>, Integer>();
        map.forEach((e, l) -> m.put(holder(e), l));
        return new ItemEnchantments(m);
    }

    public Map<Enchantment, Integer> toMap() {
        var m = new LinkedHashMap<Enchantment, Integer>();
        levels.forEach((h, l) -> m.put(h.value(), l));
        return m;
    }

    public static ItemEnchantments of(net.minecraft.world.item.ItemStack stack) {
        return fromMap(net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack));
    }

    /** Sets (replaces) one enchantment level on a stack, like 1.21 ItemStack#enchant. */
    public static void enchant(net.minecraft.world.item.ItemStack stack, Holder<Enchantment> e, int level) {
        enchant(stack, e.value(), level);
    }

    public static void enchant(net.minecraft.world.item.ItemStack stack, Enchantment e, int level) {
        var map = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack);
        map.put(e, level);
        net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(map, stack);
    }

    /** 1.21 Enchantment#getAnvilCost from the 1.20.1 rarity. */
    public static int anvilCost(Enchantment e) {
        return switch (e.getRarity()) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 4;
            case VERY_RARE -> 8;
        };
    }

    public Set<Holder<Enchantment>> keySet() { return levels.keySet(); }
    public Set<Map.Entry<Holder<Enchantment>, Integer>> entrySet() { return levels.entrySet(); }
    public int getLevel(Holder<Enchantment> e) { return levels.getOrDefault(e, 0); }
    public int getLevel(Enchantment e) { return getLevel(holder(e)); }
    public boolean isEmpty() { return levels.isEmpty(); }
    public int size() { return levels.size(); }

    public static final class Mutable {
        private final LinkedHashMap<Holder<Enchantment>, Integer> levels;
        public Mutable(ItemEnchantments base) { this.levels = new LinkedHashMap<>(base.levels); }
        public void set(Holder<Enchantment> e, int level) { if (level <= 0) levels.remove(e); else levels.put(e, level); }
        public void upgrade(Holder<Enchantment> e, int level) { if (level > 0) levels.merge(e, level, Math::max); }
        public void removeIf(Predicate<Holder<Enchantment>> filter) { levels.keySet().removeIf(filter); }
        public int getLevel(Holder<Enchantment> e) { return levels.getOrDefault(e, 0); }
        public static ItemEnchantments of(net.minecraft.world.item.ItemStack stack) {
        return fromMap(net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack));
    }

    /** Sets (replaces) one enchantment level on a stack, like 1.21 ItemStack#enchant. */
    public static void enchant(net.minecraft.world.item.ItemStack stack, Holder<Enchantment> e, int level) {
        enchant(stack, e.value(), level);
    }

    public static void enchant(net.minecraft.world.item.ItemStack stack, Enchantment e, int level) {
        var map = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack);
        map.put(e, level);
        net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(map, stack);
    }

    /** 1.21 Enchantment#getAnvilCost from the 1.20.1 rarity. */
    public static int anvilCost(Enchantment e) {
        return switch (e.getRarity()) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 4;
            case VERY_RARE -> 8;
        };
    }

    public Set<Holder<Enchantment>> keySet() { return levels.keySet(); }
        public ItemEnchantments toImmutable() { return new ItemEnchantments(levels); }
    }
}
