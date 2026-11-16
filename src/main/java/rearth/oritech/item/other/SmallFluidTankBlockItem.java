package rearth.oritech.item.other;

import rearth.oritech.port.PortComponents;
import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import rearth.oritech.init.OritechConfig;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi;
import rearth.oritech.api.fluid.containers.SimpleItemFluidStorage;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;

public class SmallFluidTankBlockItem extends BlockItem implements FluidApi.ItemProvider {
    
    public SmallFluidTankBlockItem(Block block, Properties settings) {
        super(block, settings);
    }
    
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        var model = getBlock() == rearth.oritech.init.BlockContent.CREATIVE_TANK_BLOCK ? "creative_tank_item_model" : "tank_item_model";
        rearth.oritech.forge.client.OritechClientForge.tankItemExtensions(consumer, rearth.oritech.Oritech.id(model));
    }
    
    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level context, List<Component> tooltip, TooltipFlag type) {
        var data = PortComponents.getOrDefault(stack, FluidApi.ITEM.getFluidComponent(), FluidStack.empty());
        
        if (data.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.oritech.fluid_empty"));
        } else {
            var amount = data.getAmount() / (float) FluidStackHooks.bucketAmount();
            tooltip.add(Component.translatable("tooltip.oritech.fluid_content_tank_tooltip", amount, FluidStackHooks.getName(data).getString()).withStyle(ChatFormatting.GRAY));
        }
        
        super.appendHoverText(stack, context, tooltip, type);
        
    }
    
    @Override
    public Component getName(ItemStack stack) {
        var content = PortComponents.getOrDefault(stack, FluidApi.ITEM.getFluidComponent(), FluidStack.empty());
        if (content.isEmpty()) {
            return super.getName(stack);
        } else {
            return FluidStackHooks.getName(content).copy().append(Component.literal(" ")).append(super.getName(stack));
        }
    }
    
    @Override
    public FluidApi.SingleSlotStorage getFluidStorage(ItemStack stack) {
        return new SimpleItemFluidStorage(OritechConfig.portableTankCapacityBuckets.get() * FluidStackHooks.bucketAmount(), stack);
    }
    
    @Override
    public boolean isBarVisible(ItemStack stack) {
        var contentEmpty = PortComponents.getOrDefault(stack, FluidApi.ITEM.getFluidComponent(), FluidStack.empty()).isEmpty();
        return !contentEmpty;
    }
    
    @Override
    public int getBarColor(ItemStack stack) {
        var content = PortComponents.getOrDefault(stack, FluidApi.ITEM.getFluidComponent(), FluidStack.empty());
        if (content.isEmpty())
            return 0x07bdff;
        
        if (content.getFluid().equals(Fluids.LAVA))
            return 0xff8000;
        
        return FluidStackHooks.getColor(content);
    }
    
    @Override
    public int getBarWidth(ItemStack stack) {
        
        var capacity = OritechConfig.portableTankCapacityBuckets.get() * FluidStackHooks.bucketAmount();
        var fillAmount = PortComponents.getOrDefault(stack, FluidApi.ITEM.getFluidComponent(), FluidStack.empty()).getAmount();
        
        return Math.round((fillAmount * 100f / capacity) * MAX_BAR_WIDTH) / 100;
    }
}
