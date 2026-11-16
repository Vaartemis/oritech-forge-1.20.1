package rearth.oritech.port;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

import java.util.function.Consumer;

/** GeckoLib 1.21 GeoRenderProvider emulation, bridged to Forge IClientItemExtensions. */
public interface GeoRenderProvider {

    default @Nullable BlockEntityWithoutLevelRenderer getGeoItemRenderer() { return null; }

    default @Nullable <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) { return null; }

    interface Source {
        void createGeoRenderer(Consumer<GeoRenderProvider> consumer);
    }

    /** Called from Item#initializeClient. */
    static void bridge(Source source, Consumer<IClientItemExtensions> consumer) {
        source.createGeoRenderer(provider -> consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                var r = provider.getGeoItemRenderer();
                return r != null ? r : IClientItemExtensions.super.getCustomRenderer();
            }

            @SuppressWarnings({"unchecked", "rawtypes"})
            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                var r = provider.getGeoArmorRenderer(livingEntity, itemStack, equipmentSlot, (HumanoidModel) original);
                if (r == null) return original;
                if (r instanceof GeoArmorRenderer<?> geo) geo.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return r;
            }
        }));
    }
}
