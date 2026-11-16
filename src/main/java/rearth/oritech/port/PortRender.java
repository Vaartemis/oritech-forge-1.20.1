package rearth.oritech.port;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** 1.21 bulk addVertex(x, y, z, argb, u, v, overlay, light, nx, ny, nz) on 1.20.1. */
public final class PortRender {
    private PortRender() { }

    public static void vertex(VertexConsumer c, float x, float y, float z, int argb, float u, float v, int overlay, int light, float nx, float ny, float nz) {
        float a = (argb >>> 24 & 255) / 255f, r = (argb >> 16 & 255) / 255f, g = (argb >> 8 & 255) / 255f, b = (argb & 255) / 255f;
        c.vertex(x, y, z, r, g, b, a, u, v, overlay, light, nx, ny, nz);
    }
}
