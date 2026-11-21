package rearth.oritech.compat;

import net.minecraft.world.entity.Entity;
import rearth.oritech.Oritech;
import virtuoel.pehkui.api.ScaleModifier;
import virtuoel.pehkui.api.ScaleRegistries;
import virtuoel.pehkui.api.ScaleType;
import virtuoel.pehkui.api.ScaleTypes;
import virtuoel.pehkui.api.TypedScaleModifier;

/** Own Pehkui scale type (oritech:augment_size) that multiplies the base scale, so other size mods and /scale keep working. */
final class PehkuiScale {
    
    private static ScaleType type;
    
    private PehkuiScale() {
    }
    
    static synchronized void register() {
        if (type != null) return;
        ScaleModifier modifier = ScaleRegistries.register(ScaleRegistries.SCALE_MODIFIERS, Oritech.id("augment_size_multiplier"), new TypedScaleModifier(() -> type));
        type = ScaleRegistries.register(ScaleRegistries.SCALE_TYPES, Oritech.id("augment_size"),
          ScaleType.Builder.create().affectsDimensions().addDependentModifier(modifier).defaultPersistence(true).build());
        ScaleTypes.BASE.getDefaultBaseValueModifiers().add(modifier);
    }
    
    static void setScale(Entity entity, float scale) {
        if (type == null) return;
        var data = type.getScaleData(entity);
        if (Math.abs(data.getTargetScale() - scale) > 1.0E-4f) data.setTargetScale(scale);
    }
}
