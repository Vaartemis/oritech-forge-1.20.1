package rearth.oritech.port;
@FunctionalInterface public interface StreamEncoder<O, T> { void encode(O buf, T value); }
