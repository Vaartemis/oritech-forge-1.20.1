package rearth.oritech.forge;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.util.TriConsumer;
import rearth.oritech.Oritech;
import rearth.oritech.port.CustomPacketPayload;
import rearth.oritech.port.PortUtil;
import rearth.oritech.port.RegistryFriendlyByteBuf;
import rearth.oritech.port.StreamCodec;

import java.util.HashMap;
import java.util.Map;

/** Payload transport over architectury networking (one architectury channel per payload id). */
public final class ForgeNetworking {
    private ForgeNetworking() { }

    private static final Map<ResourceLocation, StreamCodec<RegistryFriendlyByteBuf, ?>> CODECS = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> RegistryFriendlyByteBuf encode(T payload, RegistryAccess access) {
        var id = payload.type().id();
        var codec = (StreamCodec<RegistryFriendlyByteBuf, T>) CODECS.get(id);
        if (codec == null) throw new IllegalStateException("No codec for Oritech payload " + id);
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), access);
        codec.encode(buf, payload);
        return buf;
    }

    public static <T extends CustomPacketPayload> void registerToClient(CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> codec, TriConsumer<T, Level, RegistryAccess> consumer) {
        CODECS.put(id.id(), codec);
        // S2C receivers only exist on the client (architectury strips them on dedicated servers)
        if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) return;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, id.id(), (buf, ctx) -> {
            var access = rearth.oritech.port.PortUtil.registries();
            T payload;
            try {
                payload = codec.decode(new RegistryFriendlyByteBuf(buf, access));
            } catch (Exception e) {
                Oritech.LOGGER.error("Failed to decode Oritech payload {}", id.id(), e);
                return;
            }
            ctx.queue(() -> {
                var player = ctx.getPlayer();
                if (player != null) consumer.accept(payload, player.level(), access);
            });
        });
    }

    public static <T extends CustomPacketPayload> void registerToServer(CustomPacketPayload.Type<T> id, StreamCodec<RegistryFriendlyByteBuf, T> codec, TriConsumer<T, Player, RegistryAccess> consumer) {
        CODECS.put(id.id(), codec);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, id.id(), (buf, ctx) -> {
            var access = rearth.oritech.port.PortUtil.registries();
            T payload;
            try {
                payload = codec.decode(new RegistryFriendlyByteBuf(buf, access));
            } catch (Exception e) {
                Oritech.LOGGER.error("Failed to decode Oritech payload {}", id.id(), e);
                return;
            }
            ctx.queue(() -> consumer.accept(payload, ctx.getPlayer(), access));
        });
    }

    public static void sendToPlayer(CustomPacketPayload message, ServerPlayer player) {
        NetworkManager.sendToPlayer(player, message.type().id(), encode(message, player.level().registryAccess()));
    }

    public static void sendTracking(BlockEntity blockEntity, CustomPacketPayload message) {
        if (!(blockEntity.getLevel() instanceof ServerLevel level)) return;
        var players = level.getChunkSource().chunkMap.getPlayers(new ChunkPos(blockEntity.getBlockPos()), false);
        if (players.isEmpty()) return;
        var id = message.type().id();
        var bytes = encode(message, level.registryAccess());
        for (var player : players) {
            NetworkManager.sendToPlayer(player, id, new RegistryFriendlyByteBuf(bytes.copy(), level.registryAccess()));
        }
        bytes.release();
    }

    public static void sendToServer(CustomPacketPayload message) {
        NetworkManager.sendToServer(message.type().id(), encode(message, PortUtil.registries()));
    }
}
