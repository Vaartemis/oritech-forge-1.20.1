package rearth.oritech.port;

import net.minecraft.world.item.ItemStack;

public record SingleRecipeInput(ItemStack item) implements RecipeInput {
    @Override public ItemStack getItem(int index) { return index == 0 ? item : ItemStack.EMPTY; }
    @Override public int size() { return 1; }
}
