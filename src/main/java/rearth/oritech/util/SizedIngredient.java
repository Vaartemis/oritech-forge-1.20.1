package rearth.oritech.util;

import rearth.oritech.port.PortCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Predicate;
import rearth.oritech.port.RegistryFriendlyByteBuf;
import rearth.oritech.port.ByteBufCodecs;
import rearth.oritech.port.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

// this is very much inspired by TR: https://github.com/TechReborn/TechReborn/blob/1.21/RebornCore/src/main/java/reborncore/common/crafting/SizedIngredient.java#L50
public record SizedIngredient(int count, Ingredient ingredient) implements Predicate<ItemStack> {
    
    public static MapCodec<SizedIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      Codec.INT.optionalFieldOf("count", 1).forGetter(SizedIngredient::count),
      PortCodecs.INGREDIENT_CODEC.fieldOf("ingredient").forGetter(SizedIngredient::ingredient)
    ).apply(instance, SizedIngredient::new));
    
    public static StreamCodec<RegistryFriendlyByteBuf, SizedIngredient> PACKET_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT, SizedIngredient::count,
      PortCodecs.INGREDIENT, SizedIngredient::ingredient,
      SizedIngredient::new
    );
    
    @Override
    public boolean test(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            // Never match empty stacks.
            return false;
        }
        
        if (itemStack.getCount() < count) {
            return false;
        }
        
        return ingredient.test(itemStack);
    }
}
