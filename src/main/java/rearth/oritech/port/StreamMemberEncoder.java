package rearth.oritech.port;
@FunctionalInterface public interface StreamMemberEncoder<O, T> { void encode(T value, O buf); }
