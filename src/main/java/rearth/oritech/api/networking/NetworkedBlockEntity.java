package rearth.oritech.api.networking;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import rearth.oritech.port.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.Oritech;

// important: when implementing this class and the block has a GUI, make sure to call `this.sendUpdate(SyncType.GUI_OPEN);` in the `saveExtraData()` method.
// also ensure the `gui_tick` event type is sent from the screenhandler `sendContentUpdates` call, e.g. `blockEntity.sendUpdate(SyncType.GUI_TICK);`
public abstract class NetworkedBlockEntity extends BlockEntity implements BlockEntityTicker<NetworkedBlockEntity> {
    
    private boolean networkDirty = false;
    private boolean needsInitialUpdate = false;
    private long lastSentTickUpdate = 0;
    // Machine owner + owner attributes (rearth.oritech.port.MachineOwnerHooks)
    public java.util.UUID engineerOwner;
    @SyncField({SyncType.GUI_OPEN, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public float engineerSpeed;
    @SyncField({SyncType.GUI_OPEN, SyncType.SPARSE_TICK, SyncType.INITIAL})
    public float engineerEfficiency;
    public long engineerRefreshedAt = -100000;

    public void writeEngineer(CompoundTag nbt) {
        if (engineerOwner == null) return;
        nbt.putUUID("oritech.owner", engineerOwner);
        nbt.putFloat("oritech.owner_speed", engineerSpeed);
        nbt.putFloat("oritech.owner_efficiency", engineerEfficiency);
    }

    public void readEngineer(CompoundTag nbt) {
        if (!nbt.hasUUID("oritech.owner")) return;
        engineerOwner = nbt.getUUID("oritech.owner");
        engineerSpeed = nbt.getFloat("oritech.owner_speed");
        engineerEfficiency = nbt.getFloat("oritech.owner_efficiency");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        writeEngineer(nbt);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        readEngineer(nbt);
    }
    
    public NetworkedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    // this should never be used in child classes, always use serverTick / clientTick
    @Override
    public void tick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        if (world.isClientSide) {
            clientTick(world, pos, state, blockEntity);
            return;
        }
        
        serverTick(world, pos, state, blockEntity);
        
        var time = world.getGameTime();
        
        if ((time + this.worldPosition.asLong()) % getSparseUpdateInterval() == 0)
            sendUpdate(SyncType.SPARSE_TICK);
        
        if (networkDirty && time >= lastSentTickUpdate + getTickUpdateInterval()) {
            networkDirty = false;
            sendUpdate(SyncType.TICK);
            lastSentTickUpdate = time;
        }
        if (needsInitialUpdate) {
            needsInitialUpdate = false;
            sendUpdate(SyncType.INITIAL);
        }
    }
    
    public abstract void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity);
    public void clientTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {};
    
    public int getSparseUpdateInterval() {return 100;}
    
    public int getTickUpdateInterval() {return 4;}
    
    @Override
    public void setChanged() {
        setChanged(false);
    }
    
    public void setChanged(boolean updateComparator) {
        if (this.level != null) {
            setChanged(this.level, this.worldPosition, this.getBlockState());
            if (updateComparator)
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        
        networkDirty = true;
    }
    
    public void preNetworkUpdate(SyncType type) {}
    
    public void sendUpdate(SyncType type) {
        if (level == null) {
            Oritech.LOGGER.warn("unable to send update: World is null.");
            return;
        }
        
        preNetworkUpdate(type);
        
        var usedBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        var fieldCount = NetworkManager.encodeFields(this, type, usedBuf, level);
        if (fieldCount == 0) return;
        
        NetworkManager.sendBlockHandle(this, new NetworkManager.MessagePayload(worldPosition, BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(getType()), type, usedBuf.array()));
    }
    
    public void sendUpdate(SyncType type, ServerPlayer player) {
        if (level == null) {
            Oritech.LOGGER.warn("unable to send player update: World is null.");
            return;
        }
        
        preNetworkUpdate(type);
        
        var usedBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        var fieldCount = NetworkManager.encodeFields(this, type, usedBuf, level);
        if (fieldCount == 0) return;
        
        NetworkManager.sendPlayerHandle(new NetworkManager.MessagePayload(worldPosition, BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(getType()), type, usedBuf.array()), player);
    }
    
    @Override
    public CompoundTag getUpdateTag() {
        HolderLookup.Provider registryLookup = rearth.oritech.port.PortUtil.registries();
        needsInitialUpdate = true;
        return super.getUpdateTag();
    }
}
