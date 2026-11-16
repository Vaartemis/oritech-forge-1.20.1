package rearth.oritech.forge;

import com.google.common.collect.MapMaker;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;
import rearth.oritech.util.FakePlayerMarker;

import java.util.Map;

/** Machine fake player (Forge FakePlayer that drops picked-up items into the machine inventory). */
public class FakeMachinePlayerImpl extends FakePlayer implements FakePlayerMarker {
    private static final Map<Key, FakePlayer> PLAYERS = new MapMaker().weakValues().makeMap();
    private record Key(ServerLevel world, GameProfile profile) { }

    private final SimpleInventoryStorage inventory;

    private FakeMachinePlayerImpl(ServerLevel world, GameProfile profile, SimpleInventoryStorage inventory) {
        super(world, profile);
        this.inventory = inventory;
    }

    public static ServerPlayer create(ServerLevel world, GameProfile profile, SimpleInventoryStorage inventory) {
        return PLAYERS.computeIfAbsent(new Key(world, profile), k -> new FakeMachinePlayerImpl(k.world(), k.profile(), inventory));
    }

    @Override
    public boolean addItem(ItemStack itemStack) {
        this.inventory.insert(itemStack, false);
        return true;
    }
}
