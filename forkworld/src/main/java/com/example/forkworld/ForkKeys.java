package com.example.forkworld;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = ForkWorld.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForkKeys {
    private static final String CAT = "key.categories.forkworld";

    public static final KeyMapping FORK = new KeyMapping("key.forkworld.fork", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CAT);
    public static final KeyMapping MERGE = new KeyMapping("key.forkworld.merge", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CAT);
    public static final KeyMapping REWIND = new KeyMapping("key.forkworld.rewind", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CAT);
    public static final KeyMapping DISCARD = new KeyMapping("key.forkworld.discard", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CAT);
    /** Debug only: records in the REAL world so the replayer can be tested without a sandbox. */
    public static final KeyMapping DEBUG_RECORD = new KeyMapping("key.forkworld.debug_record", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CAT);

    private ForkKeys() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent e) {
        e.register(FORK);
        e.register(MERGE);
        e.register(REWIND);
        e.register(DISCARD);
        e.register(DEBUG_RECORD);
    }
}
