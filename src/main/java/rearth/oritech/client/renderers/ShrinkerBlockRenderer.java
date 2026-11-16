package rearth.oritech.client.renderers;

import rearth.oritech.block.entity.interaction.ShrinkerBlockEntity;

// 1.20.1 GeckoLib has no DynamicGeoBlockRenderer: the shrinker uses the regular machine renderer
// (moving parts keep their own texture instead of the animated strange-mixture overlay)
public class ShrinkerBlockRenderer extends MachineRenderer<ShrinkerBlockEntity> {
    
    public ShrinkerBlockRenderer(String modelPath) {
        super(modelPath);
    }
}
