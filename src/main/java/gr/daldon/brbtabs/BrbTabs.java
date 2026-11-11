package gr.daldon.brbtabs;

import net.minecraftforge.fml.common.Mod;

/**
 * Forge entry point. The recipe index is deliberately not initialized at
 * mod construction time: modded creative tabs have not been populated yet.
 */
@Mod(BrbTabs.MOD_ID)
public final class BrbTabs {
    public static final String MOD_ID = "brbtabs";

    public BrbTabs() {
        // Actual UI integration will be registered after the 1.20.1 BRB
        // screen implementation is inspected and validated.
    }
}
