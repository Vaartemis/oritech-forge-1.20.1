package rearth.oritech.api.fluid.containers;

import rearth.oritech.port.PortComponents;
import dev.architectury.fluid.FluidStack;
import rearth.oritech.api.fluid.FluidApi;

import java.util.function.Consumer;
import rearth.oritech.port.DataComponents;
import net.minecraft.world.item.ItemStack;

public class SimpleItemFluidStorage extends SimpleFluidStorage {
    
    private final ItemStack itemStack;
    public Consumer<ItemStack> contextCallback;
    
    public SimpleItemFluidStorage(Long capacity, ItemStack itemStack) {
        super(capacity);
        this.itemStack = itemStack;
        this.setStack(PortComponents.getOrDefault(itemStack, FluidApi.ITEM.getFluidComponent(), FluidStack.empty()));
        
        if (!this.getStack().isEmpty())
            PortComponents.set(itemStack, DataComponents.MAX_STACK_SIZE, 1);
    }
    
    @Override
    public void update() {
        super.update();
        
        if (this.getStack().isEmpty()) {
            PortComponents.remove(itemStack, FluidApi.ITEM.getFluidComponent());
            PortComponents.set(itemStack, DataComponents.MAX_STACK_SIZE, itemStack.getItem().getMaxStackSize());
            return;
        }
        
        PortComponents.set(itemStack, FluidApi.ITEM.getFluidComponent(), this.getStack());
        PortComponents.set(itemStack, DataComponents.MAX_STACK_SIZE, 1);
        
        if (contextCallback != null) contextCallback.accept(itemStack);
    }
    
    public SimpleItemFluidStorage withCallback(Consumer<ItemStack> contextCallback) {
        this.contextCallback = contextCallback;
        return this;
    }
}
