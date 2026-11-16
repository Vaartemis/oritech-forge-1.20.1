package rearth.oritech.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Forge 1.20.1 replacement for NeoForge's RegisterCapabilitiesEvent: Oritech block entities and items get
 * their energy / item / fluid capabilities attached here, built from the Oritech API providers.
 */
public final class ForgeCapabilityHub {
    private ForgeCapabilityHub() { }

    private static final ResourceLocation KEY = Oritech.id("capabilities");

    record BlockCap<T>(Capability<T> cap, List<Supplier<BlockEntityType<?>>> types, BiFunction<BlockEntity, Direction, T> factory) {
        boolean matches(BlockEntityType<?> type, Set<BlockEntityType<?>> resolved) { return resolved.contains(type); }
    }

    record ItemCap<T>(Capability<T> cap, List<Supplier<Item>> items, BiFunction<ItemStack, Void, T> factory) { }

    private static final List<BlockCap<?>> BLOCK_CAPS = new ArrayList<>();
    private static final List<ItemCap<?>> ITEM_CAPS = new ArrayList<>();
    private static List<Set<BlockEntityType<?>>> resolvedBlocks;
    private static List<Set<Item>> resolvedItems;
    private static final Map<BlockEntity, BlockProvider> PROVIDERS = Collections.synchronizedMap(new WeakHashMap<>());

    public static <T> void registerBlock(Capability<T> cap, List<Supplier<BlockEntityType<?>>> types, BiFunction<BlockEntity, Direction, T> factory) {
        BLOCK_CAPS.add(new BlockCap<>(cap, types, factory));
        resolvedBlocks = null;
    }

    public static <T> void registerItem(Capability<T> cap, List<Supplier<Item>> items, BiFunction<ItemStack, Void, T> factory) {
        ITEM_CAPS.add(new ItemCap<>(cap, items, factory));
        resolvedItems = null;
    }

    private static List<Set<BlockEntityType<?>>> blocks() {
        if (resolvedBlocks == null) {
            var list = new ArrayList<Set<BlockEntityType<?>>>();
            for (var c : BLOCK_CAPS) {
                var set = new HashSet<BlockEntityType<?>>();
                for (var s : c.types()) set.add(s.get());
                list.add(set);
            }
            resolvedBlocks = list;
        }
        return resolvedBlocks;
    }

    private static List<Set<Item>> items() {
        if (resolvedItems == null) {
            var list = new ArrayList<Set<Item>>();
            for (var c : ITEM_CAPS) {
                var set = new HashSet<Item>();
                for (var s : c.items()) set.add(s.get());
                list.add(set);
            }
            resolvedItems = list;
        }
        return resolvedItems;
    }

    public static void attachBlock(AttachCapabilitiesEvent<BlockEntity> event) {
        var be = event.getObject();
        var sets = blocks();
        List<BlockCap<?>> caps = null;
        for (int i = 0; i < BLOCK_CAPS.size(); i++) {
            if (sets.get(i).contains(be.getType())) {
                if (caps == null) caps = new ArrayList<>(3);
                caps.add(BLOCK_CAPS.get(i));
            }
        }
        if (caps == null) return;
        var provider = new BlockProvider(be, caps);
        event.addCapability(KEY, provider);
        event.addListener(provider::invalidate);
        PROVIDERS.put(be, provider);
    }

    public static void attachItem(AttachCapabilitiesEvent<ItemStack> event) {
        var stack = event.getObject();
        var sets = items();
        List<ItemCap<?>> caps = null;
        for (int i = 0; i < ITEM_CAPS.size(); i++) {
            if (sets.get(i).contains(stack.getItem())) {
                if (caps == null) caps = new ArrayList<>(2);
                caps.add(ITEM_CAPS.get(i));
            }
        }
        if (caps == null) return;
        event.addCapability(KEY, new ItemProvider(stack, caps));
    }

    /** Drops cached capability instances of the block entity at pos (Oritech calls this when a machine's IO layout changes). */
    public static void reset(Level level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (be == null) return;
        var p = PROVIDERS.get(be);
        if (p != null) p.invalidate();
    }

    @Nullable
    public static <T> T lookup(Level level, BlockPos pos, @Nullable BlockEntity entity, Capability<T> cap, @Nullable Direction side) {
        var be = entity != null ? entity : level.getBlockEntity(pos);
        if (be == null) return null;
        return be.getCapability(cap, side).resolve().orElse(null);
    }

    @Nullable
    public static <T> T lookup(ItemStack stack, Capability<T> cap) {
        if (stack.isEmpty()) return null;
        return stack.getCapability(cap).resolve().orElse(null);
    }

    static final class BlockProvider implements ICapabilityProvider {
        private final BlockEntity be;
        private final List<BlockCap<?>> caps;
        private final Map<Capability<?>, LazyOptional<?>[]> cache = new IdentityHashMap<>();

        BlockProvider(BlockEntity be, List<BlockCap<?>> caps) {
            this.be = be;
            this.caps = caps;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            for (var c : caps) {
                if (c.cap() != cap) continue;
                var arr = cache.computeIfAbsent(cap, k -> new LazyOptional[7]);
                int idx = side == null ? 6 : side.ordinal();
                var cached = arr[idx];
                if (cached != null && cached.isPresent()) return (LazyOptional<T>) cached;
                var value = ((BiFunction) c.factory()).apply(be, side);
                if (value == null) return LazyOptional.empty();
                var lo = LazyOptional.of(() -> value);
                arr[idx] = lo;
                return (LazyOptional<T>) lo;
            }
            return LazyOptional.empty();
        }

        void invalidate() {
            for (var arr : cache.values())
                for (var lo : arr) if (lo != null) lo.invalidate();
            cache.clear();
        }
    }

    static final class ItemProvider implements ICapabilityProvider {
        private final ItemStack stack;
        private final List<ItemCap<?>> caps;

        ItemProvider(ItemStack stack, List<ItemCap<?>> caps) {
            this.stack = stack;
            this.caps = caps;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            for (var c : caps) {
                if (c.cap() != cap) continue;
                var value = ((BiFunction) c.factory()).apply(stack, null);
                return value == null ? LazyOptional.empty() : LazyOptional.of(() -> (T) value);
            }
            return LazyOptional.empty();
        }
    }

    // keep the Forge capability constants reachable for the API impls
    public static final Capability<net.minecraftforge.energy.IEnergyStorage> ENERGY = ForgeCapabilities.ENERGY;
    public static final Capability<net.minecraftforge.items.IItemHandler> ITEMS = ForgeCapabilities.ITEM_HANDLER;
    public static final Capability<net.minecraftforge.fluids.capability.IFluidHandler> FLUIDS = ForgeCapabilities.FLUID_HANDLER;
    public static final Capability<net.minecraftforge.fluids.capability.IFluidHandlerItem> FLUID_ITEMS = ForgeCapabilities.FLUID_HANDLER_ITEM;
}
