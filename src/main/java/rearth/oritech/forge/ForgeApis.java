package rearth.oritech.forge;

/** Single instances of the platform API implementations (ServiceLoader replacement). */
public final class ForgeApis {
    private ForgeApis() { }
    public static final ForgeEnergyApiImpl ENERGY = new ForgeEnergyApiImpl();
    public static final ForgeFluidApiImpl FLUID = new ForgeFluidApiImpl();
    public static final ForgeItemApiImpl ITEM = new ForgeItemApiImpl();
    public static final OritechPlatformForge PLATFORM = new OritechPlatformForge();
}
