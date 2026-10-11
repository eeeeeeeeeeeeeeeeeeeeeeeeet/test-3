package com.example.forkworld;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * A second, private integrated server running inside the client process.
 * It is built the same way vanilla builds a singleplayer world, but it is
 * never connected to the client yet (milestone 1: prove it boots and ticks
 * alongside a real multiplayer connection).
 *
 * initServer is overridden so the NeoForge "server about to start / starting"
 * lifecycle events (and the global current-server reference) are not touched.
 */
public final class SandboxServer extends IntegratedServer {

    private SandboxServer(Thread thread, Minecraft mc, LevelStorageSource.LevelStorageAccess access,
                          PackRepository packs, WorldStem stem, Services services,
                          ChunkProgressListenerFactory listeners) {
        super(thread, mc, access, packs, stem, services, listeners);
    }

    /**
     * NeoForge keeps ONE global "current server" and one set of loaded SERVER configs.
     * On a multiplayer connection nothing owns them, so the sandbox takes them over (and
     * cleans up on stop, like a normal integrated server). In singleplayer the real world
     * owns them, so the sandbox must not touch them - and must never be stopped, because
     * stopping runs the global "server stopped" cleanup and breaks the real world.
     */
    private volatile boolean ownsLifecycle;

    public boolean ownsLifecycle() {
        return ownsLifecycle;
    }

    @Override
    public boolean initServer() {
        this.ownsLifecycle = ServerLifecycleHooks.getCurrentServer() == null;
        this.setUsesAuthentication(false);
        this.setPvpAllowed(true);
        this.setFlightAllowed(true);
        if (ownsLifecycle) {
            ServerLifecycleHooks.handleServerAboutToStart(this);
        }
        this.loadLevel();
        this.setMotd("Fork sandbox");
        if (ownsLifecycle) {
            ServerLifecycleHooks.handleServerStarting(this);
        }
        return true;
    }

    /** Must be called on the client thread. */
    public static SandboxServer boot(Minecraft mc) throws Exception {
        Path root = FMLPaths.GAMEDIR.get().resolve("forkworld").resolve("sandbox");
        Files.createDirectories(root);
        LevelStorageSource source = LevelStorageSource.createDefault(root);
        LevelStorageSource.LevelStorageAccess access = source.createAccess("fork_" + System.currentTimeMillis());

        PackRepository packs = ServerPacksSource.createPackRepository(access);
        WorldDataConfiguration dataConfig = WorldDataConfiguration.DEFAULT;
        LevelSettings settings = new LevelSettings("Fork sandbox", GameType.CREATIVE, false,
                Difficulty.NORMAL, true, new GameRules(), dataConfig);
        WorldOptions options = WorldOptions.defaultWithRandomSeed();

        WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packs, dataConfig, false, false);
        WorldLoader.InitConfig initConfig = new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.INTEGRATED, 2);
        CompletableFuture<WorldStem> future = WorldLoader.load(
                initConfig,
                ctx -> {
                    WorldDimensions.Complete dims = WorldPresets.createNormalWorldDimensions(ctx.datapackWorldgen())
                            .bake(ctx.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM));
                    return new WorldLoader.DataLoadOutput<>(
                            new PrimaryLevelData(settings, options, dims.specialWorldProperty(), dims.lifecycle()),
                            dims.dimensionsRegistryAccess());
                },
                WorldStem::new,
                Util.backgroundExecutor(),
                mc);
        mc.managedBlock(future::isDone);
        WorldStem stem = future.get();
        access.saveDataTag(stem.registries().compositeAccess(), stem.worldData());

        Services services = Services.create(new YggdrasilAuthenticationService(mc.getProxy()), mc.gameDirectory);

        return MinecraftServer.spin(thread -> new SandboxServer(thread, mc, access, packs, stem, services,
                radius -> StoringChunkProgressListener.createFromGameruleRadius(radius + 0)));
    }
}
