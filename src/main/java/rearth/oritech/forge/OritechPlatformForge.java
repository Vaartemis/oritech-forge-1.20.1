package rearth.oritech.forge;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.level.BlockEvent;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.OritechPlatform;
import rearth.oritech.api.attachment.Attachment;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.port.CustomPacketPayload;
import rearth.oritech.port.RegistryFriendlyByteBuf;
import rearth.oritech.port.StreamCodec;

public class OritechPlatformForge implements OritechPlatform {

    @Override public void sendBlockHandle(BlockEntity blockEntity, CustomPacketPayload message) { ForgeNetworking.sendTracking(blockEntity, message); }
    @Override public void sendPlayerHandle(CustomPacketPayload message, ServerPlayer player) { ForgeNetworking.sendToPlayer(message, player); }
    @Override public void sendToServer(CustomPacketPayload message) { ForgeNetworking.sendToServer(message); }

    @Override
    public <T extends CustomPacketPayload> void registerToClient(CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> packetCodec, TriConsumer<T, Level, RegistryAccess> consumer) {
        ForgeNetworking.registerToClient(id, packetCodec, consumer);
    }

    @Override
    public <T extends CustomPacketPayload> void registerToServer(CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> packetCodec, TriConsumer<T, Player, RegistryAccess> consumer) {
        ForgeNetworking.registerToServer(id, packetCodec, consumer);
    }

    @Override public <T> void register(Attachment<T> attachment) { ForgeAttachments.register(attachment); }
    @Override public <T> boolean hasAttachment(LivingEntity entity, Attachment<T> attachment) { return ForgeAttachments.has(entity, attachment); }
    @Override public <T> T getAttachmentValue(LivingEntity entity, Attachment<T> attachment) { return ForgeAttachments.get(entity, attachment); }
    @Override public <T> void setAttachment(LivingEntity entity, Attachment<T> attachment, T value) { ForgeAttachments.set(entity, attachment, value); }
    @Override public <T> void removeAttachment(LivingEntity entity, Attachment<T> attachment) { ForgeAttachments.remove(entity, attachment); }

    @Override
    public ServerPlayer create(ServerLevel world, GameProfile profile, SimpleInventoryStorage inventory) {
        return FakeMachinePlayerImpl.create(world, profile, inventory);
    }

    @Override
    public void resetCapabilities(ServerLevel world, BlockPos pos) {
        ForgeCapabilityHub.reset(world, pos);
    }

    @Override
    public boolean canPlayerBreakBlock(Level level, BlockPos pos, BlockState state, @NotNull Player player) {
        if (level.isClientSide() || !(level instanceof ServerLevel)) return true;
        var event = new BlockEvent.BreakEvent(level, pos, state, player);
        MinecraftForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    @Override
    public boolean canAttackBeDone(Level level, LivingEntity target, float amount, DamageSource damageSource) {
        if (level.isClientSide() || !(level instanceof ServerLevel)) return true;
        return !MinecraftForge.EVENT_BUS.post(new LivingAttackEvent(target, damageSource, amount));
    }
}
