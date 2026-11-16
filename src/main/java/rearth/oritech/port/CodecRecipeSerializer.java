package rearth.oritech.port;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Bridges 1.21 codec based serializers onto 1.20.1's JSON / network serializer API. */
public interface CodecRecipeSerializer<T extends Recipe<?> & IdentifiedRecipe> extends RecipeSerializer<T> {
    MapCodec<T> codec();

    StreamCodec<RegistryFriendlyByteBuf, T> streamCodec();

    @Override
    default T fromJson(ResourceLocation id, JsonObject json) {
        var recipe = codec().codec().parse(JsonOps.INSTANCE, json).getOrThrow(false, msg -> { });
        recipe.setId(id);
        return recipe;
    }

    @Override
    default T fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        var recipe = streamCodec().decode(new RegistryFriendlyByteBuf(buf, PortUtil.registries()));
        recipe.setId(id);
        return recipe;
    }

    @Override
    default void toNetwork(FriendlyByteBuf buf, T recipe) {
        streamCodec().encode(new RegistryFriendlyByteBuf(buf, PortUtil.registries()), recipe);
    }
}
