package rearth.oritech.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.energy.BlockEnergyApi;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.ItemEnergyApi;
import rearth.oritech.api.energy.containers.SimpleEnergyItemStorage;
import rearth.oritech.api.lookup.BlockLookupCache;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.port.DataComponentType;
import rearth.oritech.util.StackContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Oritech energy API on Forge Energy (int based, values clamped). */
public class ForgeEnergyApiImpl implements BlockEnergyApi, ItemEnergyApi {

    private final List<Supplier<BlockEntityType<?>>> registeredBlockEntities = new ArrayList<>();
    private final List<Supplier<Item>> registeredItems = new ArrayList<>();
    private boolean hooked;

    private void hook() {
        if (hooked) return;
        hooked = true;
        ForgeCapabilityHub.registerBlock(ForgeCapabilityHub.ENERGY, registeredBlockEntities,
          (entity, direction) -> ContainerStorageWrapper.of(((EnergyApi.BlockProvider) entity).getEnergyStorage(direction)));
        ForgeCapabilityHub.registerItem(ForgeCapabilityHub.ENERGY, registeredItems,
          (stack, ignored) -> ContainerStorageWrapper.of(((EnergyApi.ItemProvider) stack.getItem()).getEnergyStorage(stack)));
    }

    @Override
    public void registerBlockEntity(Supplier<BlockEntityType<?>> typeSupplier) {
        registeredBlockEntities.add(typeSupplier);
        hook();
    }

    @Override
    public void registerForItem(Supplier<Item> itemSupplier) {
        registeredItems.add(itemSupplier);
        hook();
    }

    @Override
    public DataComponentType<Long> getEnergyComponent() {
        return ComponentContent.ENERGY;
    }

    @Override
    public EnergyApi.EnergyStorage find(StackContext stack) {
        if (stack.getValue().getCount() > 1) return null;
        var candidate = ForgeCapabilityHub.lookup(stack.getValue(), ForgeCapabilityHub.ENERGY);
        if (candidate == null) return null;
        if (candidate instanceof ContainerStorageWrapper wrapper && wrapper.container instanceof SimpleEnergyItemStorage itemStorage)
            return itemStorage.withCallback(ignored -> stack.sync());
        return new ForgeStorageWrapper(candidate);
    }

    @Override
    public EnergyApi.EnergyStorage find(Level world, BlockPos pos, @Nullable BlockState state, @Nullable BlockEntity entity, @Nullable Direction direction) {
        return wrapStorage(ForgeCapabilityHub.lookup(world, pos, entity, ForgeCapabilityHub.ENERGY, direction));
    }

    @Override
    public EnergyApi.EnergyStorage find(Level world, BlockPos pos, @Nullable Direction direction) {
        return find(world, pos, null, null, direction);
    }

    @Override
    public BlockLookupCache<EnergyApi.EnergyStorage> createCache(Level world, BlockPos pos, @Nullable Direction direction) {
        return BlockLookupCache.of(() -> find(world, pos, direction));
    }

    private static @Nullable EnergyApi.EnergyStorage wrapStorage(@Nullable IEnergyStorage candidate) {
        if (candidate == null) return null;
        if (candidate instanceof ContainerStorageWrapper wrapper) return wrapper.container;
        return new ForgeStorageWrapper(candidate);
    }

    private static int clamp(long v) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, v));
    }

    /** Other mods' energy storages, seen through the Oritech API. */
    public static class ForgeStorageWrapper extends EnergyApi.EnergyStorage {
        public final IEnergyStorage storage;

        public ForgeStorageWrapper(IEnergyStorage storage) { this.storage = storage; }

        @Override public boolean supportsInsertion() { return storage.canReceive(); }
        @Override public boolean supportsExtraction() { return storage.canExtract(); }
        @Override public long insert(long maxAmount, boolean simulate) { return storage.receiveEnergy(clamp(maxAmount), simulate); }
        @Override public long extract(long maxAmount, boolean simulate) { return storage.extractEnergy(clamp(maxAmount), simulate); }
        @Override public long getAmount() { return storage.getEnergyStored(); }
        @Override public long getCapacity() { return storage.getMaxEnergyStored(); }
        @Override public void setAmount(long amount) { }
        @Override public void update() { }
    }

    /** Oritech storages exposed to other mods as Forge Energy. */
    public static class ContainerStorageWrapper implements IEnergyStorage {
        public final EnergyApi.EnergyStorage container;

        public static ContainerStorageWrapper of(EnergyApi.EnergyStorage container) {
            return container == null ? null : new ContainerStorageWrapper(container);
        }

        public ContainerStorageWrapper(EnergyApi.EnergyStorage container) { this.container = container; }

        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            if (!container.supportsInsertion()) return 0;
            long inserted = container.insert(amount, simulate);
            if (!simulate && inserted > 0) container.update();
            return clamp(inserted);
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            if (!container.supportsExtraction()) return 0;
            long extracted = container.extract(amount, simulate);
            if (!simulate && extracted > 0) container.update();
            return clamp(extracted);
        }

        @Override public int getEnergyStored() { return clamp(container.getAmount()); }
        @Override public int getMaxEnergyStored() { return clamp(container.getCapacity()); }
        @Override public boolean canExtract() { return container.supportsExtraction(); }
        @Override public boolean canReceive() { return container.supportsInsertion(); }
    }
}
