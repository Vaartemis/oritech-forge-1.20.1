package rearth.oritech.port;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import rearth.oritech.api.networking.NetworkedBlockEntity;

/** Port addition: machines remember the player who placed them and use that player's attributes.
 *  oritech:machine_speed and oritech:machine_efficiency (player attributes, base 0 = no change): factor = 1 / (1 + value).
 *  Modpacks raise them with any attribute source (items, effects, skill mods). Stat oritech:machine_operations counts
 *  finished machine recipes for the owner. Nothing here depends on another mod. */
public final class MachineOwnerHooks {
    private MachineOwnerHooks() {}
    public static final DeferredRegister<Attribute> ATTRS = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, "oritech");
    public static final RegistryObject<Attribute> MACHINE_SPEED = ATTRS.register("machine_speed",
        () -> new RangedAttribute("attribute.oritech.machine_speed", 0, 0, 1024).setSyncable(true));
    public static final RegistryObject<Attribute> MACHINE_EFFICIENCY = ATTRS.register("machine_efficiency",
        () -> new RangedAttribute("attribute.oritech.machine_efficiency", 0, 0, 1024).setSyncable(true));
    public static final ResourceLocation MACHINE_OPERATIONS = new ResourceLocation("oritech", "machine_operations");
    private static final int REFRESH_TICKS = 200;

    public static void init(IEventBus modBus) {
        ATTRS.register(modBus);
        modBus.addListener((EntityAttributeModificationEvent e) -> {
            e.add(EntityType.PLAYER, MACHINE_SPEED.get());
            e.add(EntityType.PLAYER, MACHINE_EFFICIENCY.get());
        });
        modBus.addListener((RegisterEvent e) -> e.register(Registries.CUSTOM_STAT, h -> h.register(MACHINE_OPERATIONS, MACHINE_OPERATIONS)));
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(() -> Stats.CUSTOM.get(MACHINE_OPERATIONS, StatFormatter.DEFAULT)));
        MinecraftForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p)) return;
            if (e.getLevel().getBlockEntity(e.getPos()) instanceof NetworkedBlockEntity be) {
                be.engineerOwner = p.getUUID();
                copy(be, p);
                be.setChanged();
            }
        });
    }

    private static void copy(NetworkedBlockEntity be, ServerPlayer p) {
        be.engineerSpeed = (float) p.getAttributeValue(MACHINE_SPEED.get());
        be.engineerEfficiency = (float) p.getAttributeValue(MACHINE_EFFICIENCY.get());
    }

    private static ServerPlayer owner(NetworkedBlockEntity be) {
        if (be.engineerOwner == null || !(be.getLevel() instanceof ServerLevel sl)) return null;
        return sl.getServer().getPlayerList().getPlayer(be.engineerOwner);
    }

    /** cached owner values, refreshed every 10 s while the owner is online (offline keeps the last values) */
    private static void refresh(NetworkedBlockEntity be) {
        if (be.engineerOwner == null || be.getLevel() == null || be.getLevel().isClientSide) return;
        long t = be.getLevel().getGameTime();
        if (t - be.engineerRefreshedAt < REFRESH_TICKS) return;
        be.engineerRefreshedAt = t;
        ServerPlayer p = owner(be);
        if (p != null) copy(be, p);
    }

    public static float speedFactor(NetworkedBlockEntity be) {
        refresh(be);
        return 1f / (1f + Math.max(0f, be.engineerSpeed));
    }

    public static float efficiencyFactor(NetworkedBlockEntity be) {
        refresh(be);
        return 1f / (1f + Math.max(0f, be.engineerEfficiency));
    }

    public static void onCraft(NetworkedBlockEntity be) {
        ServerPlayer p = owner(be);
        if (p != null) p.awardStat(Stats.CUSTOM.get(MACHINE_OPERATIONS));
    }
}
