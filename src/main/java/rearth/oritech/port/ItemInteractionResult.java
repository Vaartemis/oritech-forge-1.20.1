package rearth.oritech.port;

import net.minecraft.world.InteractionResult;

/** 1.21 ItemInteractionResult stand-in; bridged into Block#use by the port. */
public enum ItemInteractionResult {
    SUCCESS, CONSUME, CONSUME_PARTIAL, PASS_TO_DEFAULT_BLOCK_INTERACTION, SKIP_DEFAULT_BLOCK_INTERACTION, FAIL;

    public InteractionResult result() {
        return switch (this) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case CONSUME -> InteractionResult.CONSUME;
            case CONSUME_PARTIAL -> InteractionResult.CONSUME_PARTIAL;
            case PASS_TO_DEFAULT_BLOCK_INTERACTION, SKIP_DEFAULT_BLOCK_INTERACTION -> InteractionResult.PASS;
            case FAIL -> InteractionResult.FAIL;
        };
    }

    public boolean consumesAction() { return result().consumesAction(); }

    public static ItemInteractionResult sidedSuccess(boolean client) { return client ? SUCCESS : CONSUME; }
}
