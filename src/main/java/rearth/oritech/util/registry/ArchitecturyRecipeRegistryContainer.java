package rearth.oritech.util.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import rearth.oritech.Oritech;

import java.lang.reflect.Field;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public interface ArchitecturyRecipeRegistryContainer extends ArchitecturyRegistryContainer<RecipeType<?>> {
    
    
    
    @Override
    default ResourceKey<Registry<RecipeType<?>>> getRegistryType() {
        return Registries.RECIPE_TYPE;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    default Class<RecipeType<?>> getTargetFieldType() {
        return (Class<RecipeType<?>>) (Object) RecipeType.class;
    }
    
    @Override
    default void postProcessField(String namespace, RecipeType<?> value, String identifier, Field field, java.util.function.Supplier<RecipeType<?>> supplier) {
        ArchitecturyRegistryContainer.super.postProcessField(namespace, value, identifier, field, supplier);
        // serializers are registered by OritechModForge.registerSerializers
    }
    
    static void finishSerializerRegister() {
    }
    
}
