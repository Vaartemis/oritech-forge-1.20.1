package rearth.oritech.item.tools.harvesting;

import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import rearth.oritech.port.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.interaction.TreefellerBlockEntity;
import rearth.oritech.client.renderers.PromethiumToolRenderer;
import net.minecraft.core.particles.ParticleTypes;
import software.bernie.geckolib.animatable.GeoItem;
import rearth.oritech.port.GeoRenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

public class PromethiumAxeItem extends AxeItem implements GeoRenderProvider.Source, GeoItem {
    
    public record PendingBlock(Level level, BlockPos pos, ItemStack tool) {};
    public static final Deque<PendingBlock> pendingBlocks = new ArrayDeque<>();
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    public PromethiumAxeItem(Tier toolMaterial, float attackDamage, float attackSpeed, Properties settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
        // 1.20.1: AxeItem already mines axe blocks; sword-efficient blocks and cobweb handled in getDestroySpeed
    }
    
    @Override
    public float getDestroySpeed(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        if (state.is(Blocks.COBWEB)) return 15.0F;
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) return getTier().getSpeed();
        if (state.is(BlockTags.SWORD_EFFICIENT)) return 1.5F;
        return 1.0F;
    }
    
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        return state.is(Blocks.COBWEB) || super.isCorrectToolForDrops(stack, state);
    }
    
    @Override
    public boolean mineBlock(ItemStack stack, Level world, BlockState state, BlockPos pos, LivingEntity miner) {
        
        if (!world.isClientSide && miner.isShiftKeyDown()) {
            var startPos = pos.above();
            var startState = world.getBlockState(startPos);
            if (startState.is(BlockTags.LOGS)) {
                var treeBlocks = TreefellerBlockEntity.getTreeBlocks(startPos, world);
                pendingBlocks.addAll(treeBlocks.stream().map(elem -> new PendingBlock(world, elem, stack)).toList());
            }
        }
        
        return true;
    }
    
    public static void processPendingBlocks(Level world) {
        if (pendingBlocks.isEmpty()) return;
        
        var topWorld = pendingBlocks.getFirst().level();
        if (topWorld != world) return;
        
        for (int i = 0; i < 8 && !pendingBlocks.isEmpty(); i++) {
            var candidate = pendingBlocks.pollFirst();
            var candidatePos = candidate.pos();
            var candidateState = world.getBlockState(candidatePos);
            if (!candidateState.is(BlockTags.LOGS) && !candidateState.is(BlockTags.LEAVES)) return;
            
            var dropped = Block.getDrops(candidateState, (ServerLevel) world, candidatePos, null, null, candidate.tool());
            world.setBlockAndUpdate(candidatePos, Blocks.AIR.defaultBlockState());
            
            dropped.forEach(elem -> world.addFreshEntity(new ItemEntity(world, candidatePos.getX(), candidatePos.getY(), candidatePos.getZ(), elem)));
            
            world.playSound(null, candidatePos, candidateState.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.5f, 1f);
            world.addDestroyBlockEffect(candidatePos, candidateState);
            
            if (world instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, candidatePos.getX() + 0.5, candidatePos.getY() + 0.5, candidatePos.getZ() + 0.5, 4, 0.6, 0.6, 0.6, 0);
            
            if (candidateState.is(BlockTags.LOGS)) break;
        }
        
    }
    
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return false;
    }
    
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }
    
    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        GeoRenderProvider.bridge(this, consumer);
    }
    
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private PromethiumToolRenderer renderer;
            
            @Override
            public @Nullable BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new PromethiumToolRenderer("promethium_axe");
                return renderer;
            }
        });
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    
    public static void onTick(ServerLevel serverWorld) {
        processPendingBlocks(serverWorld);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level context, List<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, context, tooltip, type);
        tooltip.add(Component.translatable("tooltip.oritech.promethium_axe").withStyle(ChatFormatting.DARK_GRAY));
    }
}
