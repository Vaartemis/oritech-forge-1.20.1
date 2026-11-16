package rearth.oritech.port;

import net.minecraft.resources.ResourceLocation;

/** Recipes whose id is assigned after codec decoding. */
public interface IdentifiedRecipe {
    void setId(ResourceLocation id);
}
