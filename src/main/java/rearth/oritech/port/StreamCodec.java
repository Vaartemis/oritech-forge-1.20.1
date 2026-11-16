package rearth.oritech.port;

import com.mojang.datafixers.util.*;
import java.util.function.*;

/** Backport of the 1.21 StreamCodec API onto 1.20.1 ByteBufs. */
public interface StreamCodec<B, V> extends StreamDecoder<B, V>, StreamEncoder<B, V> {

    static <B, V> StreamCodec<B, V> of(StreamEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
        return new StreamCodec<>() {
            public V decode(B b) { return decoder.decode(b); }
            public void encode(B b, V v) { encoder.encode(b, v); }
        };
    }

    static <B, V> StreamCodec<B, V> ofMember(StreamMemberEncoder<B, V> encoder, StreamDecoder<B, V> decoder) {
        return new StreamCodec<>() {
            public V decode(B b) { return decoder.decode(b); }
            public void encode(B b, V v) { encoder.encode(v, b); }
        };
    }

    static <B, V> StreamCodec<B, V> unit(V value) {
        return new StreamCodec<>() {
            public V decode(B b) { return value; }
            public void encode(B b, V v) { }
        };
    }

    default <O> StreamCodec<B, O> apply(CodecOperation<B, V, O> op) { return op.apply(this); }

    default <O> StreamCodec<B, O> map(Function<? super V, ? extends O> to, Function<? super O, ? extends V> from) {
        var self = this;
        return new StreamCodec<>() {
            public O decode(B b) { return to.apply(self.decode(b)); }
            public void encode(B b, O o) { self.encode(b, from.apply(o)); }
        };
    }

    default <O extends io.netty.buffer.ByteBuf> StreamCodec<O, V> mapStream(Function<O, ? extends B> f) {
        var self = this;
        return new StreamCodec<>() {
            public V decode(O b) { return self.decode(f.apply(b)); }
            public void encode(O b, V v) { self.encode(f.apply(b), v); }
        };
    }

    @SuppressWarnings("unchecked")
    default <S extends B> StreamCodec<S, V> cast() { return (StreamCodec<S, V>) this; }

    interface CodecOperation<B, S, T> { StreamCodec<B, T> apply(StreamCodec<B, S> codec); }

    static <B, C, T1> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, Function<T1, C> f) {
        return new StreamCodec<>() {
            public C decode(B b) { return f.apply(c1.decode(b)); }
            public void encode(B b, C c) { c1.encode(b, g1.apply(c)); }
        };
    }

    static <B, C, T1, T2> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2, BiFunction<T1, T2, C> f) {
        return new StreamCodec<>() {
            public C decode(B b) { var a1 = c1.decode(b); var a2 = c2.decode(b); return f.apply(a1, a2); }
            public void encode(B b, C c) { c1.encode(b, g1.apply(c)); c2.encode(b, g2.apply(c)); }
        };
    }

    static <B, C, T1, T2, T3> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3, Function3<T1, T2, T3, C> f) {
        return new StreamCodec<>() {
            public C decode(B b) { var a1 = c1.decode(b); var a2 = c2.decode(b); var a3 = c3.decode(b); return f.apply(a1, a2, a3); }
            public void encode(B b, C c) { c1.encode(b, g1.apply(c)); c2.encode(b, g2.apply(c)); c3.encode(b, g3.apply(c)); }
        };
    }

    static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4, Function4<T1, T2, T3, T4, C> f) {
        return new StreamCodec<>() {
            public C decode(B b) { var a1 = c1.decode(b); var a2 = c2.decode(b); var a3 = c3.decode(b); var a4 = c4.decode(b); return f.apply(a1, a2, a3, a4); }
            public void encode(B b, C c) { c1.encode(b, g1.apply(c)); c2.encode(b, g2.apply(c)); c3.encode(b, g3.apply(c)); c4.encode(b, g4.apply(c)); }
        };
    }

    static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4, StreamCodec<? super B, T5> c5, Function<C, T5> g5, Function5<T1, T2, T3, T4, T5, C> f) {
        return new StreamCodec<>() {
            public C decode(B b) { var a1 = c1.decode(b); var a2 = c2.decode(b); var a3 = c3.decode(b); var a4 = c4.decode(b); var a5 = c5.decode(b); return f.apply(a1, a2, a3, a4, a5); }
            public void encode(B b, C c) { c1.encode(b, g1.apply(c)); c2.encode(b, g2.apply(c)); c3.encode(b, g3.apply(c)); c4.encode(b, g4.apply(c)); c5.encode(b, g5.apply(c)); }
        };
    }

    static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4, StreamCodec<? super B, T5> c5, Function<C, T5> g5, StreamCodec<? super B, T6> c6, Function<C, T6> g6, Function6<T1, T2, T3, T4, T5, T6, C> f) {
        return new StreamCodec<>() {
            public C decode(B b) { var a1 = c1.decode(b); var a2 = c2.decode(b); var a3 = c3.decode(b); var a4 = c4.decode(b); var a5 = c5.decode(b); var a6 = c6.decode(b); return f.apply(a1, a2, a3, a4, a5, a6); }
            public void encode(B b, C c) { c1.encode(b, g1.apply(c)); c2.encode(b, g2.apply(c)); c3.encode(b, g3.apply(c)); c4.encode(b, g4.apply(c)); c5.encode(b, g5.apply(c)); c6.encode(b, g6.apply(c)); }
        };
    }

    static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2, StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4, StreamCodec<? super B, T5> c5, Function<C, T5> g5, StreamCodec<? super B, T6> c6, Function<C, T6> g6, StreamCodec<? super B, T7> c7, Function<C, T7> g7, Function7<T1, T2, T3, T4, T5, T6, T7, C> f) {
        return new StreamCodec<>() {
            public C decode(B b) { var a1 = c1.decode(b); var a2 = c2.decode(b); var a3 = c3.decode(b); var a4 = c4.decode(b); var a5 = c5.decode(b); var a6 = c6.decode(b); var a7 = c7.decode(b); return f.apply(a1, a2, a3, a4, a5, a6, a7); }
            public void encode(B b, C c) { c1.encode(b, g1.apply(c)); c2.encode(b, g2.apply(c)); c3.encode(b, g3.apply(c)); c4.encode(b, g4.apply(c)); c5.encode(b, g5.apply(c)); c6.encode(b, g6.apply(c)); c7.encode(b, g7.apply(c)); }
        };
    }
}
