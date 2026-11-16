package rearth.oritech.port;

import com.mojang.datafixers.util.Either;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.*;
import java.util.function.*;

import static rearth.oritech.port.RegistryFriendlyByteBuf.friendly;

/** Backport of the 1.21 ByteBufCodecs helpers. */
public final class ByteBufCodecs {
    private ByteBufCodecs() { }

    public static final StreamCodec<ByteBuf, Boolean> BOOL = StreamCodec.of(ByteBuf::writeBoolean, ByteBuf::readBoolean);
    public static final StreamCodec<ByteBuf, Byte> BYTE = StreamCodec.of((b, v) -> b.writeByte(v), ByteBuf::readByte);
    public static final StreamCodec<ByteBuf, Short> SHORT = StreamCodec.of((b, v) -> b.writeShort(v), ByteBuf::readShort);
    public static final StreamCodec<ByteBuf, Integer> INT = StreamCodec.of(ByteBuf::writeInt, ByteBuf::readInt);
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of((b, v) -> friendly(b).writeVarInt(v), b -> friendly(b).readVarInt());
    public static final StreamCodec<ByteBuf, Long> LONG = StreamCodec.of(ByteBuf::writeLong, ByteBuf::readLong);
    public static final StreamCodec<ByteBuf, Long> VAR_LONG = StreamCodec.of((b, v) -> friendly(b).writeVarLong(v), b -> friendly(b).readVarLong());
    public static final StreamCodec<ByteBuf, Float> FLOAT = StreamCodec.of(ByteBuf::writeFloat, ByteBuf::readFloat);
    public static final StreamCodec<ByteBuf, Double> DOUBLE = StreamCodec.of(ByteBuf::writeDouble, ByteBuf::readDouble);
    public static final StreamCodec<ByteBuf, byte[]> BYTE_ARRAY = StreamCodec.of((b, v) -> friendly(b).writeByteArray(v), b -> friendly(b).readByteArray());
    public static final StreamCodec<ByteBuf, String> STRING_UTF8 = StreamCodec.of((b, v) -> friendly(b).writeUtf(v), b -> friendly(b).readUtf());
    public static final StreamCodec<ByteBuf, CompoundTag> COMPOUND_TAG = StreamCodec.of((b, v) -> friendly(b).writeNbt(v), b -> friendly(b).readNbt());
    public static final StreamCodec<ByteBuf, Optional<CompoundTag>> OPTIONAL_COMPOUND_TAG = StreamCodec.of((b, v) -> friendly(b).writeNbt(v.orElse(null)), b -> Optional.ofNullable(friendly(b).readNbt()));

    public static StreamCodec<ByteBuf, String> stringUtf8(int max) {
        return StreamCodec.of((b, v) -> friendly(b).writeUtf(v, max), b -> friendly(b).readUtf(max));
    }

    public static <B extends ByteBuf, V> StreamCodec<B, Optional<V>> optional(StreamCodec<? super B, V> codec) {
        return new StreamCodec<>() {
            public Optional<V> decode(B b) { return b.readBoolean() ? Optional.of(codec.decode(b)) : Optional.empty(); }
            public void encode(B b, Optional<V> v) {
                b.writeBoolean(v.isPresent());
                v.ifPresent(x -> codec.encode(b, x));
            }
        };
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> factory, StreamCodec<? super B, V> codec) {
        return collection(factory, codec, Integer.MAX_VALUE);
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> factory, StreamCodec<? super B, V> codec, int max) {
        return new StreamCodec<>() {
            public C decode(B b) {
                int n = friendly(b).readVarInt();
                if (n > max) throw new IllegalStateException("collection too big: " + n);
                C c = factory.apply(Math.min(n, 65536));
                for (int i = 0; i < n; i++) c.add(codec.decode(b));
                return c;
            }
            public void encode(B b, C c) {
                friendly(b).writeVarInt(c.size());
                for (V v : c) codec.encode(b, v);
            }
        };
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec.CodecOperation<B, V, C> collection(IntFunction<C> factory) {
        return codec -> collection(factory, codec);
    }

    public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
        return codec -> collection(ArrayList::new, codec);
    }

    public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list(int max) {
        return codec -> collection(ArrayList::new, codec, max);
    }

    public static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(IntFunction<? extends M> factory, StreamCodec<? super B, K> keys, StreamCodec<? super B, V> values) {
        return new StreamCodec<>() {
            public M decode(B b) {
                int n = friendly(b).readVarInt();
                M m = factory.apply(Math.min(n, 65536));
                for (int i = 0; i < n; i++) { K k = keys.decode(b); V v = values.decode(b); m.put(k, v); }
                return m;
            }
            public void encode(B b, M m) {
                friendly(b).writeVarInt(m.size());
                m.forEach((k, v) -> { keys.encode(b, k); values.encode(b, v); });
            }
        };
    }


    public static <B extends ByteBuf, L, R> StreamCodec<B, Either<L, R>> either(StreamCodec<? super B, L> left, StreamCodec<? super B, R> right) {
        return new StreamCodec<>() {
            public Either<L, R> decode(B b) { return b.readBoolean() ? Either.left(left.decode(b)) : Either.right(right.decode(b)); }
            public void encode(B b, Either<L, R> e) {
                e.ifLeft(l -> { b.writeBoolean(true); left.encode(b, l); }).ifRight(r -> { b.writeBoolean(false); right.encode(b, r); });
            }
        };
    }

    public static <T> StreamCodec<ByteBuf, T> idMapper(IntFunction<T> byId, ToIntFunction<T> toId) {
        return StreamCodec.of((b, v) -> friendly(b).writeVarInt(toId.applyAsInt(v)), b -> byId.apply(friendly(b).readVarInt()));
    }

    /** Encodes any value through a DFU codec as NBT (used where 1.21 had fromCodec). */
    public static <T> StreamCodec<ByteBuf, T> fromCodec(com.mojang.serialization.Codec<T> codec) {
        return StreamCodec.of(
          (b, v) -> friendly(b).writeNbt((CompoundTag) wrap(codec.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, v).getOrThrow(false, e -> { }))),
          b -> codec.parse(net.minecraft.nbt.NbtOps.INSTANCE, unwrap(friendly(b).readNbt())).getOrThrow(false, e -> { }));
    }

    public static <T> StreamCodec<ByteBuf, T> fromCodecWithRegistries(com.mojang.serialization.Codec<T> codec) {
        return fromCodec(codec);
    }

    private static net.minecraft.nbt.Tag wrap(net.minecraft.nbt.Tag t) {
        var c = new CompoundTag();
        c.put("v", t);
        return c;
    }

    private static net.minecraft.nbt.Tag unwrap(CompoundTag t) {
        return t == null ? new CompoundTag() : t.get("v");
    }
}
