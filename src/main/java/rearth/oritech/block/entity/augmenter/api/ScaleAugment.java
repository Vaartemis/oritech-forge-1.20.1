package rearth.oritech.block.entity.augmenter.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import rearth.oritech.api.attachment.AttachmentApi;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.compat.PehkuiCompat;

/**
 * Player size augments (Miniaturization / Gigantism). Upstream uses the vanilla generic.scale attribute, which only
 * exists from 1.20.5 on. On 1.20.1 the size is applied through Pehkui when it is installed; without Pehkui the
 * recipes are not loaded (forge:mod_loaded condition) and this class never touches Pehkui classes.
 */
public class ScaleAugment extends Augment {
    
    public static final ResourceLocation SCALE_ATTRIBUTE = new ResourceLocation("minecraft", "generic.scale");
    
    private final AttributeModifier.Operation operation;
    private final float amount;
    
    public ScaleAugment(ResourceLocation id, AttributeModifier.Operation operation, float amount, boolean toggleable) {
        super(id, toggleable);
        this.operation = operation;
        this.amount = amount;
    }
    
    @Override
    public void activate(Player player) {
        apply(player, this, true);
    }
    
    @Override
    public void deactivate(Player player) {
        apply(player, this, false);
    }
    
    @Override
    public void refreshServer(Player player) {
        apply(player, null, false);
    }
    
    @Override
    public int refreshInterval() {
        return 3000;
    }
    
    // same math as a vanilla attribute with base value 1 and every enabled size augment as a modifier.
    // 'changed' overrides the stored state, since toggle() calls activate/deactivate before saving it.
    private static void apply(Player player, ScaleAugment changed, boolean changedEnabled) {
        if (player.level().isClientSide || !PehkuiCompat.isLoaded()) return;
        var data = AttachmentApi.getAttachmentValue(player, ACTIVE_AUGMENTS_DATA);
        double add = 0, multBase = 0, multTotal = 1;
        for (var augment : PlayerAugments.allAugments.values()) {
            if (!(augment instanceof ScaleAugment scale)) continue;
            var enabled = augment == changed ? changedEnabled : augment.isEnabled(data);
            if (!enabled) continue;
            switch (scale.operation) {
                case ADDITION -> add += scale.amount;
                case MULTIPLY_BASE -> multBase += scale.amount;
                case MULTIPLY_TOTAL -> multTotal *= 1 + scale.amount;
            }
        }
        var base = 1 + add;
        var value = (base + base * multBase) * multTotal;
        PehkuiCompat.setAugmentScale(player, (float) Math.max(0.0625, Math.min(16, value)));
    }
}
