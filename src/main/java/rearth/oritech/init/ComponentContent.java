package rearth.oritech.init;

import com.mojang.serialization.Codec;
import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import rearth.oritech.block.entity.interaction.ShrinkerBlockEntity;
import rearth.oritech.port.DataComponentType;
import rearth.oritech.port.PortCodecs;

/** Item data stored in stack NBT (1.20.1 has no data components). */
public class ComponentContent {

    public static final DataComponentType<Boolean> IS_AOE_ACTIVE = new DataComponentType<>("oritech:is_aoe_active", Codec.BOOL);

    public static final DataComponentType<BlockPos> TARGET_POSITION = new DataComponentType<>("oritech:target_position", BlockPos.CODEC);

    public static final DataComponentType<FluidStack> STORED_FLUID = new DataComponentType<>("oritech:stored_fluid", PortCodecs.FLUID_STACK_CODEC);

    public static final DataComponentType<ShrinkerBlockEntity.ShrunkAddonData> ADDON_DATA = new DataComponentType<>("oritech:addon_data", ShrinkerBlockEntity.ShrunkAddonData.CODEC);

    /** Energy stored in Oritech items (was a NeoForge-registered component). */
    public static final DataComponentType<Long> ENERGY = new DataComponentType<>("oritech:energy", Codec.LONG);
}
