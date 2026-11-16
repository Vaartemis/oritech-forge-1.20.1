package rearth.oritech.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.Oritech;
import rearth.oritech.OritechClient;
import rearth.oritech.client.cablesurfer.ActiveCableRenderer;
import rearth.oritech.client.init.ModRenderers;
import rearth.oritech.client.other.OreFinderRenderer;
import rearth.oritech.client.renderers.BlockOutlineRenderer;
import rearth.oritech.client.renderers.PortalEntityRenderer;
import rearth.oritech.client.renderers.SmallTankItemRenderer;
import rearth.oritech.init.EntitiesContent;

import java.util.function.Consumer;

/** Client side of the Forge 1.20.1 platform layer (replaces OritechClientNeoForge). */
public final class OritechClientForge {
    private OritechClientForge() { }

    public static void init(IEventBus modBus) {
        modBus.addListener(OritechClientForge::registerRenderers);
        modBus.addListener(OritechClientForge::addLayers);
        MinecraftForge.EVENT_BUS.addListener(OritechClientForge::onWorldRender);
        MinecraftForge.EVENT_BUS.addListener(OritechClientForge::onOutlineRender);
        MinecraftForge.EVENT_BUS.addListener(OritechClientForge::onMouse);
        OritechClient.initialize();
    }

    private static void onWorldRender(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
            OreFinderRenderer.doRender(event.getPoseStack(), event.getCamera(), buffers);
            ActiveCableRenderer.render(event.getPoseStack(), buffers);
        }
    }

    private static void onOutlineRender(RenderHighlightEvent.Block event) {
        BlockOutlineRenderer.render(Minecraft.getInstance().level, event.getCamera(), event.getPoseStack(), event.getMultiBufferSource());
    }

    private static void onMouse(InputEvent.MouseButton.Pre event) {
        if (OritechClient.handleMouseClicked(Minecraft.getInstance(), event.getButton(), event.getAction(), event.getModifiers()))
            event.setCanceled(true);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        OritechClient.registerRenderers();
        event.registerEntityRenderer(EntitiesContent.PORTAL_ENTITY, PortalEntityRenderer::new);
        for (var entry : ModRenderers.RENDER_LAYERS.entrySet())
            ItemBlockRenderTypes.setRenderLayer(entry.getKey(), entry.getValue());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins())
            if (event.getSkin(skin) instanceof PlayerRenderer pr)
                pr.addLayer(new OritechElytraLayer<>(pr, event.getEntityModels()));
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            try {
                if (event.getRenderer((EntityType) type) instanceof HumanoidMobRenderer<?, ?> hmr)
                    hmr.addLayer(new OritechElytraLayer(hmr, event.getEntityModels()));
            } catch (ClassCastException ignored) { }
        }
        LivingEntityRenderer ler = event.getRenderer(EntityType.ARMOR_STAND);
        if (ler != null) ler.addLayer(new OritechElytraLayer(ler, event.getEntityModels()));
    }

    /** Called from SmallFluidTankBlockItem#initializeClient. */
    public static void tankItemExtensions(Consumer<IClientItemExtensions> consumer, ResourceLocation modelId) {
        consumer.accept(new IClientItemExtensions() {
            private TankItemRenderer renderer;
            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new TankItemRenderer(modelId);
                return renderer;
            }
        });
    }

    private static class TankItemRenderer extends BlockEntityWithoutLevelRenderer {
        private final SmallTankItemRenderer itemRenderer;

        TankItemRenderer(ResourceLocation modelId) {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
            this.itemRenderer = new SmallTankItemRenderer(modelId);
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext mode, PoseStack matrices, MultiBufferSource buffers, int light, int overlay) {
            itemRenderer.render(stack, mode, matrices, buffers, light, overlay);
        }
    }
}
