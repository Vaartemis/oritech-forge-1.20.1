package rearth.oritech.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.client.init.OritechClientConfig;
import rearth.oritech.init.FluidContent;
import rearth.oritech.init.OritechConfig;
import rearth.oritech.init.OritechStartupConfig;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.item.tools.util.ArmorEventHandler;
import rearth.oritech.util.registry.RegistrySink;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;

@Mod(Oritech.MOD_ID)
public final class OritechModForge {

    /** Items queued for each Oritech creative tab (filled by ItemGroups). */
    public static final Map<CreativeModeTab, List<Supplier<ItemStack>>> TAB_CONTENTS = new IdentityHashMap<>();

    public OritechModForge() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(Oritech.MOD_ID, modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, OritechConfig.COMMON_SPEC, "oritech-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, OritechClientConfig.CLIENT_SPEC, "oritech-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, OritechStartupConfig.STARTUP_SPEC, "oritech-startup.toml");

        modBus.addListener(EventPriority.HIGH, OritechModForge::onRegister);
        modBus.addListener(OritechModForge::onBuildTabs);
        rearth.oritech.port.MachineOwnerHooks.init(modBus);
        modBus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) -> e.enqueueWork(OritechModForge::registerCompostables));

        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, (AttachCapabilitiesEvent<BlockEntity> e) -> ForgeCapabilityHub.attachBlock(e));
        MinecraftForge.EVENT_BUS.addGenericListener(ItemStack.class, (AttachCapabilitiesEvent<ItemStack> e) -> ForgeCapabilityHub.attachItem(e));
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.ItemAttributeModifierEvent e) -> {
            var mods = rearth.oritech.port.ItemAttributeModifiers.BY_ITEM.get(e.getItemStack().getItem());
            if (mods != null) mods.forSlot(e.getSlotType()).forEach(e::addModifier);
        });
        MinecraftForge.EVENT_BUS.addListener((LivingEquipmentChangeEvent e) -> ArmorEventHandler.processEvent(e.getEntity(), e.getSlot(), e.getFrom(), e.getTo()));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (PlayerEvent.Clone e) -> {
            ForgeAttachments.copy(e.getOriginal(), e.getEntity());
            if (e.getEntity() instanceof ServerPlayer player) PlayerAugments.refreshActiveAugments(player);
        });

        // fluids, their blocks and buckets: lazily built, handed to the registry events
        FluidContent.registerFluids();
        FluidContent.registerBlocks();
        FluidContent.registerItems();

        Oritech.initialize();
        rearth.oritech.compat.PehkuiCompat.init();
        // the neoforge:attachment_types registry event never fires on Forge
        rearth.oritech.block.entity.augmenter.api.Augment.registerAttachmentTypes();
        rearth.oritech.util.ServerZiplineHandler.registerAttachments();
        ForgeAttachments.initClientSyncIfClient();

        if (FMLEnvironment.dist.isClient()) rearth.oritech.forge.client.OritechClientForge.init(modBus);
    }

    // NeoForge data map data/neoforge/data_maps/item/compostables.json, applied directly on 1.20.1
    private static void registerCompostables() {
        var map = java.util.Map.of("banana", 0.65f, "biomass", 0.3f, "biomass_block", 1.0f, "packed_wheat", 1.0f);
        map.forEach((name, chance) -> {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(Oritech.id(name));
            if (item != net.minecraft.world.item.Items.AIR) net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(item, (float) chance);
        });
    }

    private static final Set<String> FLUID_CONTENT_HOOKS = Set.of();

    private static void onRegister(RegisterEvent event) {
        var key = event.getRegistryKey();
        RegistrySink.open(key, new RegistrySink.Target() {
            @Override
            public <T> void register(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> k, net.minecraft.resources.ResourceLocation id, Supplier<T> value) {
                event.register(k, id, value);
            }
        });
        try {
            if (Oritech.EVENT_MAP.containsKey(key.location())) {
                for (var hook : Oritech.EVENT_MAP.get(key.location())) hook.run();
            }

            if (key.equals(Registries.RECIPE_SERIALIZER)) registerSerializers(event);

            if (key.equals(ForgeRegistries.Keys.FLUID_TYPES)) {
                for (var attribute : FluidContent.FLUID_ATTRIBUTES) {
                    var fluid = attribute.getSourceFluid();
                    var fluidId = attribute.getSourceFluid() != null ? idOf(attribute.getSourceFluid()) : null;
                    if (fluidId == null) continue;
                    event.register(ForgeRegistries.Keys.FLUID_TYPES, fluidId, fluid::getFluidType);
                }
            }
        } finally {
            RegistrySink.close();
        }
    }

    /** Fluid ids are known before the fluid registry fires: look them up on the lazy suppliers. */
    private static net.minecraft.resources.ResourceLocation idOf(net.minecraft.world.level.material.Fluid fluid) {
        for (var f : FluidContent.class.getFields()) {
            try {
                if (f.get(null) instanceof rearth.oritech.util.registry.PortRegister.PortSupplier<?> s && s.get() == fluid) return s.getId();
            } catch (IllegalAccessException ignored) { }
        }
        return null;
    }

    /** Oritech recipe types double as their own serializers; the serializer registry fires first on Forge. */
    private static void registerSerializers(RegisterEvent event) {
        for (var field : RecipeContent.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !RecipeType.class.isAssignableFrom(field.getType())) continue;
            try {
                field.setAccessible(true);
                var value = field.get(null);
                if (value instanceof RecipeSerializer<?> serializer) {
                    var id = Oritech.id(field.getName().toLowerCase(Locale.ROOT));
                    event.register(Registries.RECIPE_SERIALIZER, id, () -> serializer);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void onBuildTabs(BuildCreativeModeTabContentsEvent event) {
        var list = TAB_CONTENTS.get(event.getTab());
        if (list == null) return;
        for (var s : list) {
            var stack = s.get();
            if (!stack.isEmpty()) event.accept(stack);
        }
    }
}
