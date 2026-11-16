package rearth.oritech.port;

import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 1.21 CraftingInput emulation: a menu-less CraftingContainer for recipe lookups. */
public class CraftingInput implements CraftingContainer {
    private final int width, height;
    private final NonNullList<ItemStack> items;

    private CraftingInput(int width, int height, List<ItemStack> stacks) {
        this.width = width;
        this.height = height;
        this.items = NonNullList.withSize(width * height, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(stacks.size(), items.size()); i++) items.set(i, stacks.get(i));
    }

    public static CraftingInput of(int width, int height, List<ItemStack> stacks) { return new CraftingInput(width, height, stacks); }

    @Override public int getWidth() { return width; }
    @Override public int getHeight() { return height; }
    @Override public List<ItemStack> getItems() { return List.copyOf(items); }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return slot >= 0 && slot < items.size() ? items.get(slot) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) { return ContainerHelper.removeItem(items, slot, amount); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); }
    @Override public void setChanged() { }
    @Override public boolean stillValid(Player player) { return true; }
    @Override public void clearContent() { items.clear(); }
    @Override public void fillStackedContents(StackedContents contents) { for (var s : items) contents.accountSimpleStack(s); }
    public int size() { return items.size(); }
}
