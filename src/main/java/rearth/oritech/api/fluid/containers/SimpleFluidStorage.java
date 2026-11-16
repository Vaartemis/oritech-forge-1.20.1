package rearth.oritech.api.fluid.containers;

import rearth.oritech.port.PortCodecs;
import dev.architectury.fluid.FluidStack;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import rearth.oritech.port.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.networking.UpdatableField;

import java.util.List;

public class SimpleFluidStorage extends FluidApi.SingleSlotStorage implements UpdatableField<Void, FluidStack> {
    
    public static Long transfer(SimpleFluidStorage from, SimpleFluidStorage to, long maxAmount, boolean simulate) {
        
        var extracted = from.extract(FluidStack.create(from.getFluid(), maxAmount, from.getChanges()), true);   // check how much we could extract at most
        var inserted = to.insert(FluidStack.create(from.getFluid(), extracted, from.getChanges()), simulate);   // insert max extraction amount
        extracted = from.extract(FluidStack.create(from.getFluid(), inserted, from.getChanges()), simulate);    // extract only how much was actually inserted
        
        if (extracted > 0 && !simulate) {
            from.update();
            to.update();
        }
        
        return extracted;
    }
    
    private FluidStack content;
    private final Long capacity;
    private final Runnable onUpdate;
    
    public SimpleFluidStorage(Long capacity, Runnable onUpdate) {
        this.capacity = capacity;
        this.onUpdate = onUpdate;
        this.content = FluidStack.create(getEmptyVariant(), 0);
    }
    
    public Fluid getEmptyVariant() {
        return Fluids.EMPTY;
    }
    
    public SimpleFluidStorage(Long capacity) {
        this(capacity, () -> {});
    }
    
    
    @Override
    public long insert(FluidStack toInsert, boolean simulate) {
        return SimpleInOutFluidStorage.insertTo(toInsert, simulate, capacity, content, stack -> this.content = stack);
    }
    
    @Override
    public long extract(FluidStack toExtract, boolean simulate) {
        return SimpleInOutFluidStorage.extractFrom(toExtract, simulate, content);
    }
    
    @Override
    public List<FluidStack> getContent() {
        return List.of(content);
    }
    
    public void writeNbt(CompoundTag nbt, String suffix) {
        PortCodecs.FLUID_STACK_CODEC.encodeStart(NbtOps.INSTANCE, content).result().ifPresent(tag -> nbt.put("fluid" + suffix, tag));
    }
    
    public void readNbt(CompoundTag nbt, String suffix) {
        content = PortCodecs.FLUID_STACK_CODEC.parse(NbtOps.INSTANCE, nbt.get("fluid" + suffix)).result().orElse(FluidStack.empty());
    }
    
    // Forge FluidStacks forget their fluid at amount 0 and the shared empty stack cannot be modified:
    // remember the fluid set via setFluid so setFluid(f) + setAmount(n) works like on 1.21
    private Fluid pendingFluid = Fluids.EMPTY;
    
    public void setAmount(long amount) {
        if (amount <= 0) {
            if (!content.isEmpty()) pendingFluid = content.getFluid();
            content = FluidStack.empty();
        } else if (content.isEmpty()) {
            if (pendingFluid != Fluids.EMPTY) content = FluidStack.create(pendingFluid, amount);
        } else {
            content.setAmount(amount);
        }
    }
    
    public long getAmount() {
        return content.getAmount();
    }
    
    public void setFluid(Fluid fluid) {
        pendingFluid = fluid;
        if (fluid == Fluids.EMPTY || getAmount() <= 0) { content = FluidStack.empty(); return; }
        content = FluidStack.create(fluid, getAmount(), getChanges());
    }
    
    public Fluid getFluid() {
        return content.getFluid();
    }
    
    public void setChanges(CompoundTag data) {
        content = FluidStack.create(getFluid(), getAmount(), data);
    }
    
    public CompoundTag getChanges() {
        return content.getTag();
    }
    
    @Override
    public long getCapacity() {
        return capacity;
    }
    
    @Override
    public void update() {
        onUpdate.run();
    }
    
    @Override
    public void setStack(FluidStack stack) {
        content = stack.copy();
    }
    
    @Override
    public FluidStack getStack() {
        return content;
    }
    
    @Override
    public FluidStack getDeltaData() {
        return this.content;
    }
    
    @Override
    public Void getFullData() {
        return null;
    }
    
    @Override
    public boolean useDeltaOnly(SyncType type) {
        return true;
    }
    
    @Override
    public StreamCodec<? extends ByteBuf, FluidStack> getDeltaCodec() {
        return NetworkManager.FLUID_STACK_STREAM_CODEC;
    }
    
    @Override
    public StreamCodec<? extends ByteBuf, Void> getFullCodec() {
        return null;
    }
    
    @Override
    public void handleFullUpdate(Void updatedData) { }
    
    @Override
    public void handleDeltaUpdate(FluidStack updatedData) {
        this.setStack(updatedData);
    }
}
