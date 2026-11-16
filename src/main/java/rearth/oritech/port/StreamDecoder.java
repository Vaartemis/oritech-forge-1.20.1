package rearth.oritech.port;
@FunctionalInterface public interface StreamDecoder<I, T> { T decode(I buf); }
