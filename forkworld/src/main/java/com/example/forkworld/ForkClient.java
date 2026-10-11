package com.example.forkworld;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

import java.io.IOException;
import java.nio.file.Path;

@EventBusSubscriber(modid = ForkWorld.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ForkClient {
    /** Real-world recording is a debug aid only; enable with -Dforkworld.debug=true. */
    private static final boolean DEBUG = Boolean.getBoolean("forkworld.debug");
    private static final int SNAPSHOT_RADIUS_CHUNKS = 4;

    private static final InputRecorder RECORDER = new InputRecorder();
    private static final Replayer REPLAYER = new Replayer();
    private static Recording last;
    private static ForkSnapshot snapshot;

    private ForkClient() {
    }

    private static Path file() {
        return FMLPaths.GAMEDIR.get().resolve("forkworld").resolve("last.json");
    }

    private static void msg(Minecraft mc, String s, boolean overlay) {
        if (mc.player != null) mc.player.displayClientMessage(Component.literal("[Fork] " + s), overlay);
    }

    @SubscribeEvent
    public static void onTickPre(ClientTickEvent.Pre e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (REPLAYER.isActive()) {
            // Abort hotkey must work during a merge.
            while (ForkKeys.DISCARD.consumeClick()) {
                REPLAYER.abort(mc, "cancelled by player");
            }
        }
        REPLAYER.pre(mc);
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent e) {
        REPLAYER.overrideInput(e.getInput());
    }

    @SubscribeEvent
    public static void onTickPost(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        RECORDER.sample(mc);

        while (ForkKeys.FORK.consumeClick()) {
            SandboxBackend backend = ForkWorld.backend;
            snapshot = ForkSnapshot.capture(mc.level, mc.player, SNAPSHOT_RADIUS_CHUNKS);
            if (backend == null) {
                msg(mc, "Captured fork (" + snapshot.summary() + "), but no sandbox backend is installed yet.", false);
            } else {
                backend.start(snapshot);
                msg(mc, "Forked (" + snapshot.summary() + ")", true);
            }
        }

        while (ForkKeys.REWIND.consumeClick()) {
            SandboxBackend b = ForkWorld.backend;
            if (b != null && b.isActive()) {
                b.rewind();
                msg(mc, "Rewound", true);
            }
        }

        while (ForkKeys.MERGE.consumeClick()) {
            if (REPLAYER.isActive() || RECORDER.isActive()) continue;
            SandboxBackend b = ForkWorld.backend;
            Recording r = (b != null && b.isActive()) ? b.finish() : last;
            if (r == null) {
                try {
                    r = Recording.load(file());
                } catch (IOException ex) {
                    msg(mc, "Nothing to merge.", true);
                    continue;
                }
            }
            last = r;
            String err = REPLAYER.start(mc, r);
            if (err != null) {
                msg(mc, "Cannot merge: " + err, false);
            } else {
                msg(mc, "Merging " + r.frames.size() + " ticks... (" + ForkKeys.DISCARD.getTranslatedKeyMessage().getString() + " to cancel)", false);
            }
        }

        while (ForkKeys.DISCARD.consumeClick()) {
            SandboxBackend b = ForkWorld.backend;
            if (b != null && b.isActive()) {
                b.discard();
                msg(mc, "Fork discarded", true);
            }
        }

        while (ForkKeys.DEBUG_RECORD.consumeClick()) {
            if (!DEBUG) {
                msg(mc, "Debug recording is off (launch with -Dforkworld.debug=true).", true);
                continue;
            }
            if (!RECORDER.isActive()) {
                RECORDER.start(mc);
                msg(mc, "DEBUG: recording in the REAL world", true);
            } else {
                last = RECORDER.stop();
                try {
                    last.save(file());
                } catch (IOException ex) {
                    msg(mc, "Could not save: " + ex.getMessage(), false);
                }
                msg(mc, "DEBUG: saved " + last.frames.size() + " ticks. Walk back to the start and press merge.", false);
            }
        }
    }
}
