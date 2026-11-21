package rearth.oritech.forge;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import rearth.oritech.Oritech;
import rearth.oritech.api.attachment.Attachment;
import rearth.oritech.port.CustomPacketPayload;
import rearth.oritech.port.PortCodecs;
import rearth.oritech.port.RegistryFriendlyByteBuf;
import rearth.oritech.port.StreamCodec;

import java.util.*;

/**
 * Entity attachments on 1.20.1: values cached per entity, persisted in the entity's persistent NBT
 * (key "oritech_attachments"), synced to the owning player, copied on respawn.
 */
public final class ForgeAttachments {
    private ForgeAttachments() { }

    static final String ROOT = "oritech_attachments";
    private static final Map<ResourceLocation, Attachment<?>> TYPES = new HashMap<>();
    private static final Map<LivingEntity, Map<ResourceLocation, Object>> VALUES = Collections.synchronizedMap(new WeakHashMap<>());

    public static <T> void register(Attachment<T> attachment) {
        TYPES.put(attachment.identifier(), attachment);
    }

    private static CompoundTag root(LivingEntity entity) {
        var data = entity.getPersistentData();
        if (!data.contains(ROOT)) data.put(ROOT, new CompoundTag());
        return data.getCompound(ROOT);
    }

    public static <T> boolean has(LivingEntity entity, Attachment<T> a) {
        var map = VALUES.get(entity);
        if (map != null && map.containsKey(a.identifier())) return true;
        return entity.getPersistentData().getCompound(ROOT).contains(a.identifier().toString());
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(LivingEntity entity, Attachment<T> a) {
        var map = VALUES.computeIfAbsent(entity, e -> new HashMap<>());
        var v = map.get(a.identifier());
        if (v != null) return (T) v;
        var tag = entity.getPersistentData().getCompound(ROOT).get(a.identifier().toString());
        T value = null;
        if (tag != null) value = a.persistenceCodec().parse(NbtOps.INSTANCE, tag).result().orElse(null);
        if (value == null) value = a.initializer().get();
        map.put(a.identifier(), value);
        return value;
    }

    public static <T> void set(LivingEntity entity, Attachment<T> a, T value) {
        VALUES.computeIfAbsent(entity, e -> new HashMap<>()).put(a.identifier(), value);
        a.persistenceCodec().encodeStart(NbtOps.INSTANCE, value).result().ifPresent(t -> root(entity).put(a.identifier().toString(), t));
        if (entity instanceof ServerPlayer player) sync(player, a, value);
    }

    public static <T> void remove(LivingEntity entity, Attachment<T> a) {
        var map = VALUES.get(entity);
        if (map != null) map.remove(a.identifier());
        root(entity).remove(a.identifier().toString());
    }

    /** Respawn / dimension change: carry attachments over to the new player entity. */
    public static void copy(LivingEntity from, LivingEntity to) {
        var data = from.getPersistentData();
        if (data.contains(ROOT)) to.getPersistentData().put(ROOT, data.getCompound(ROOT).copy());
        var map = VALUES.get(from);
        if (map != null) VALUES.put(to, new HashMap<>(map));
    }

    // ---- client sync ----
    public record SyncPayload(ResourceLocation id, byte[] data) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(Oritech.id("attachment_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> CODEC = StreamCodec.composite(
          PortCodecs.RESOURCE_LOCATION, SyncPayload::id,
          rearth.oritech.port.ByteBufCodecs.BYTE_ARRAY, SyncPayload::data,
          SyncPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> void sync(ServerPlayer player, Attachment<T> a, T value) {
        if (a.networkCodec() == null) return;
        // the augment codecs read/write a RegistryFriendlyByteBuf; a bare netty buffer threw ClassCastException
        var buf = new rearth.oritech.port.RegistryFriendlyByteBuf(Unpooled.buffer(), player.serverLevel().registryAccess());
        ((StreamCodec) a.networkCodec()).encode(buf, value);
        var bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        buf.release();
        ForgeNetworking.sendToPlayer(new SyncPayload(a.identifier(), bytes), player);
    }

    public static void initClientSyncIfClient() {
        initClientSync();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void initClientSync() {
        ForgeNetworking.registerToClient(SyncPayload.TYPE, SyncPayload.CODEC, (payload, level, access) -> {
            var a = TYPES.get(payload.id());
            var player = ClientAccess.player();
            if (a == null || player == null) return;
            var value = ((StreamCodec) a.networkCodec()).decode(new rearth.oritech.port.RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.data()), player.level().registryAccess()));
            VALUES.computeIfAbsent(player, e -> new HashMap<>()).put(a.identifier(), value);
        });
    }

    static final class ClientAccess {
        static LivingEntity player() {
            return net.minecraft.client.Minecraft.getInstance().player;
        }
    }
}
