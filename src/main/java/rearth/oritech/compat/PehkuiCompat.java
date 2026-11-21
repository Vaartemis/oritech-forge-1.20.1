package rearth.oritech.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

/** Optional Pehkui support. Pehkui classes are only touched from PehkuiScale, which is only loaded when Pehkui is present. */
public final class PehkuiCompat {
    
    private static Boolean loaded;
    
    private PehkuiCompat() {
    }
    
    public static boolean isLoaded() {
        if (loaded == null) loaded = ModList.get() != null && ModList.get().isLoaded("pehkui");
        return loaded;
    }
    
    public static void init() {
        if (isLoaded()) PehkuiScale.register();
    }
    
    public static void setAugmentScale(Player player, float scale) {
        if (isLoaded()) PehkuiScale.setScale(player, scale);
    }
}
