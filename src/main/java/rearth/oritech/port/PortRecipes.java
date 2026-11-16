package rearth.oritech.port;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** RecipeManager helpers returning {@link RecipeHolder}s like 1.21 did. */
public final class PortRecipes {
    private PortRecipes() { }

    public static <C extends Container, T extends Recipe<C>> Optional<RecipeHolder<T>> getRecipeFor(RecipeManager manager, RecipeType<T> type, C input, Level level) {
        return manager.getRecipeFor(type, input, level).map(RecipeHolder::of);
    }

    public static <C extends Container, T extends Recipe<C>> List<RecipeHolder<T>> getAllRecipesFor(RecipeManager manager, RecipeType<T> type) {
        return manager.getAllRecipesFor(type).stream().map(RecipeHolder::of).toList();
    }

    public static <C extends Container, T extends Recipe<C>> List<RecipeHolder<T>> getRecipesFor(RecipeManager manager, RecipeType<T> type, C input, Level level) {
        return manager.getRecipesFor(type, input, level).stream().map(RecipeHolder::of).toList();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional<RecipeHolder<?>> byKey(RecipeManager manager, ResourceLocation id) {
        return (Optional) manager.byKey(id).map(r -> RecipeHolder.of((Recipe) r));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Collection<RecipeHolder<?>> getRecipes(RecipeManager manager) {
        return (Collection) manager.getRecipes().stream().map(r -> RecipeHolder.of((Recipe) r)).toList();
    }
}
