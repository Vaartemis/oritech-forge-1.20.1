package rearth.oritech.port;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Small helpers that replace 1.21-only vanilla API. */
public final class PortUtil {
    private PortUtil() { }

    private static RegistryAccess builtin;

    /** 1.21 ItemStack#consume: shrink unless the player is in creative. */
    public static void consume(net.minecraft.world.item.ItemStack stack, int amount, net.minecraft.world.entity.player.Player player) {
        if (player == null || !player.getAbilities().instabuild) stack.shrink(amount);
    }

    /** Copy of a container's stacks as a list (1.20.1 SimpleContainer hides its list). */
    public static net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> items(net.minecraft.world.Container c) {
        var list = net.minecraft.core.NonNullList.withSize(c.getContainerSize(), net.minecraft.world.item.ItemStack.EMPTY);
        for (int i = 0; i < list.size(); i++) list.set(i, c.getItem(i));
        return list;
    }

    /** 1.21 ItemStack#limitSize. */
    public static void limitSize(net.minecraft.world.item.ItemStack stack, int max) {
        if (!stack.isEmpty() && stack.getCount() > max) stack.setCount(max);
    }

    /** Best available registry access (server, then client, then built-in registries). */
    public static RegistryAccess registries() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.registryAccess();
        var client = ClientHooks.registries();
        if (client != null) return client;
        if (builtin == null) builtin = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        return builtin;
    }

    /** Isolated so dedicated servers never load client classes. */
    static final class ClientHooks {
        static RegistryAccess registries() {
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) return Client.get();
            return null;
        }
    }

    static final class Client {
        static RegistryAccess get() {
            var mc = net.minecraft.client.Minecraft.getInstance();
            return mc.level != null ? mc.level.registryAccess() : null;
        }
    }
}
