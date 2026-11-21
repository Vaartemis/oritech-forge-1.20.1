package rearth.oritech.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import rearth.oritech.Oritech;

public class TagUtils {
    // upstream uses the 1.21 "c" convention tags; on Forge 1.20.1 the same tags live under "forge" (data was
    // converted the same way by tools/data/convert.py, so both sides must use this mapping)
    public static String C_TAG_NAMESPACE = "forge";
    
    private static final java.util.Map<String, String> C_TO_FORGE = java.util.Map.ofEntries(
      java.util.Map.entry("glass_blocks", "glass"), java.util.Map.entry("obsidians", "obsidian"), java.util.Map.entry("stones", "stone"),
      java.util.Map.entry("cobblestones", "cobblestone"), java.util.Map.entry("slime_balls", "slimeballs"), java.util.Map.entry("strings", "string"),
      java.util.Map.entry("leathers", "leather"), java.util.Map.entry("gunpowders", "gunpowder"), java.util.Map.entry("sands", "sand"),
      java.util.Map.entry("gravels", "gravel"), java.util.Map.entry("netherracks", "netherrack"),
      java.util.Map.entry("player_workstations/crafting_tables", "workbench"), java.util.Map.entry("player_workstations/furnaces", "furnaces"),
      java.util.Map.entry("tools/shears", "shears"), java.util.Map.entry("tools/bow", "tools/bows"), java.util.Map.entry("tools/shield", "tools/shields"),
      java.util.Map.entry("tools/fishing_rod", "tools/fishing_rods"));
    
    public static ResourceLocation conventionalId(String path) {
        return new ResourceLocation(C_TAG_NAMESPACE, C_TO_FORGE.getOrDefault(path, path));
    }
    
    public static TagKey<Item> getStorageBlockTag(String path) {
        return cItemTag("storage_blocks/" + path);
    }
    
    public static TagKey<Block> getStorageBlockyTag(String path) {
        return cBlockTag("storage_blocks/" + path);
    }
    
    public static TagKey<Item> getIngotTag(String path) {
        return cItemTag("ingots/" + path);
    }

    public static TagKey<Item> getClumpTag(String path) {
        return cItemTag("clumps/" + path);
    }
    
    public static TagKey<Item> getDustTag(String path) {
        return cItemTag("dusts/" + path);
    }

    public static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(namespace, path));
    }

    public static TagKey<Item> cItemTag(String path) {
        return TagKey.create(Registries.ITEM, conventionalId(path));
    }

    public static TagKey<Block> cBlockTag(String path) {
        return TagKey.create(Registries.BLOCK, conventionalId(path));
    }

    public static TagKey<Item> oritechItemTag(String path) {
        return TagKey.create(Registries.ITEM, Oritech.id(path));
    }

    public static TagKey<Block> oritechBlockTag(String path) {
        return TagKey.create(Registries.BLOCK, Oritech.id(path));
    }

    public static TagKey<Fluid> cFluidTag(String path) {
        return TagKey.create(Registries.FLUID, conventionalId(path));
    }

    public static TagKey<Fluid> fluidTag(String namespace, String path) {        
        return TagKey.create(Registries.FLUID, new ResourceLocation(namespace, path));
    }

    public static TagKey<Fluid> oritechFluidTag(String path) {
        return fluidTag(Oritech.MOD_ID, path);
    }
}
