package rearth.oritech.port;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

/** 1.21 RecipeHolder stand-in (1.20.1 recipes carry their own id). */
public record RecipeHolder<T extends Recipe<?>>(ResourceLocation id, T value) {
    public static <T extends Recipe<?>> RecipeHolder<T> of(T recipe) { return new RecipeHolder<>(recipe.getId(), recipe); }
}
