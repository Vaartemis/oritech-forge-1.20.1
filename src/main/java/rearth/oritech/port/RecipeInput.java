package rearth.oritech.port;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1.21 RecipeInput stand-in: a read-only Container so it fits 1.20.1's Recipe<C extends Container>. */
public interface RecipeInput extends Container {
    ItemStack getItem(int index);

    int size();

    @Override default int getContainerSize() { return size(); }
    @Override default boolean isEmpty() {
        for (int i = 0; i < size(); i++) if (!getItem(i).isEmpty()) return false;
        return true;
    }
    @Override default ItemStack removeItem(int slot, int amount) { return ItemStack.EMPTY; }
    @Override default ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
    @Override default void setItem(int slot, ItemStack stack) { }
    @Override default void setChanged() { }
    @Override default boolean stillValid(Player player) { return true; }
    @Override default void clearContent() { }
}
