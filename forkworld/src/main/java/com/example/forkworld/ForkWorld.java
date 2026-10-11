package com.example.forkworld;

import net.neoforged.fml.common.Mod;

/**
 * Fork World: fork the area around you into a private local simulation,
 * rehearse freely, then merge the winning input sequence back into the real
 * world by replaying it through the real client (ordinary packets).
 */
@Mod(ForkWorld.MODID)
public class ForkWorld {
    public static final String MODID = "forkworld";

    /** Set by the sandbox implementation once it exists. Null means "no sandbox backend yet". */
    public static volatile SandboxBackend backend = null;
}
