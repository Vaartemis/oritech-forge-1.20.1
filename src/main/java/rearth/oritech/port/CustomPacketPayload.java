package rearth.oritech.port;

import net.minecraft.resources.ResourceLocation;

/** 1.21 CustomPacketPayload stand-in; transported through architectury networking. */
public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();

    record Type<T extends CustomPacketPayload>(ResourceLocation id) { }
}
