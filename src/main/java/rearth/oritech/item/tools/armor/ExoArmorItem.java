package rearth.oritech.item.tools.armor;

import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.client.renderers.ExosuitArmorRenderer;
import rearth.oritech.item.tools.util.ArmorEventHandler;
import software.bernie.geckolib.animatable.GeoItem;
import rearth.oritech.port.GeoRenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import rearth.oritech.port.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import rearth.oritech.port.ItemAttributeModifiers;

public class ExoArmorItem extends ArmorItem implements GeoRenderProvider.Source, GeoItem, ArmorEventHandler {
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    public ExoArmorItem(ArmorMaterial material, Type type, Properties settings) {
        super(material, type, settings);
    }
    
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return false;
    }
    
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }
    
    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        var base = super.getDefaultAttributeModifiers(slot);
        if (slot != EquipmentSlot.LEGS || this.getEquipmentSlot() != EquipmentSlot.LEGS) return base;
        
        var result = com.google.common.collect.HashMultimap.create(base);
        result.put(Attributes.MOVEMENT_SPEED, ItemAttributeModifiers.mod(Oritech.id("exo_move_speed"), 0.2, AttributeModifier.Operation.MULTIPLY_BASE));
        result.put(Attributes.FLYING_SPEED, ItemAttributeModifiers.mod(Oritech.id("exo_fly_speed"), 0.2, AttributeModifier.Operation.MULTIPLY_BASE));
        return result;
    }
    
    @Override
    public void onEquipped(Player playerEntity, ItemStack stack) {
        
        if (this.getEquipmentSlot() == EquipmentSlot.HEAD)
            playerEntity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
    }
    
    @Override
    public void onUnequipped(Player playerEntity, ItemStack stack) {
        
        if (this.getEquipmentSlot() == EquipmentSlot.HEAD)
            playerEntity.removeEffect(MobEffects.NIGHT_VISION);
    }
    
    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        GeoRenderProvider.bridge(this, consumer);
    }
    
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<?> renderer;
            
            @Override
            public @Nullable <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
                
                if (this.renderer == null)
                    this.renderer = new ExosuitArmorRenderer(getModel(), Oritech.id("armor/exo_armor"));
                
                return this.renderer;
            }
        });
    }
    
    public ResourceLocation getModel() {
        return Oritech.id("armor/exo_armor");
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, 20, state -> PlayState.STOP));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level context, List<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, context, tooltip, type);
        tooltip.add(Component.translatable("tooltip.oritech." + BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()).withStyle(ChatFormatting.GRAY));
    }
}
