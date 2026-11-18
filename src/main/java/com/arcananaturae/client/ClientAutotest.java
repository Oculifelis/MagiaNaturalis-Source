package com.arcananaturae.client;

import com.mojang.logging.LogUtils;
import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Development-only automated check of the starfield glass core shader, inert unless the JVM runs with
 * -Dthaumicaugmentation.autotest (only the Gradle dev run passes it with -Pautotest, see build.gradle and
 * tools/run_client_test.sh). It makes a fresh creative superflat world "mntest", builds a small wall of the shader
 * starfield glass with the textured variants below it, takes one screenshot, logs "MNTEST" lines and stops the client.
 */
public final class ClientAutotest {
    private static final String MODE = System.getProperty("arcananaturae.autotest");
    private static final Logger LOG = LogUtils.getLogger();
    private static final String WORLD = "mntest";
    private static final long TIMEOUT_NANOS = 4L * 60L * 1_000_000_000L;

    private static boolean titleSeen;
    private static boolean creating;
    private static long titleAt;
    private static int idleTicks;
    private static int worldTicks;
    private static boolean finished;

    private ClientAutotest() {
    }

    public static void init() {
        if (MODE == null) {
            return;
        }
        LOG.info("MNTEST armed ({})", MODE);
        NeoForge.EVENT_BUS.addListener(ClientAutotest::tick);
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || finished) {
            return;
        }
        if (titleSeen && System.nanoTime() - titleAt > TIMEOUT_NANOS) {
            LOG.info("MNTEST timeout");
            finish(mc);
            return;
        }
        try {
            step(mc);
        } catch (Throwable t) {
            LOG.error("MNTEST step failed", t);
        }
    }

    private static void step(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            if (!creating && mc.screen != null && !(mc.screen instanceof TitleScreen) && mc.getOverlay() == null && ++idleTicks > 200) {
                LOG.info("MNTEST dismissing {}", mc.screen.getClass().getName());
                mc.setScreen(new TitleScreen());
                idleTicks = 0;
            }
            if (!creating && mc.screen instanceof TitleScreen title && mc.getOverlay() == null) {
                creating = true;
                titleSeen = true;
                titleAt = System.nanoTime();
                LOG.info("MNTEST title");
                mc.options.pauseOnLostFocus = false;
                mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.0);
                mc.options.framerateLimit().set(30);
                mc.options.renderDistance().set(4);
                mc.options.simulationDistance().set(5);
                deleteOldWorld(mc);
                LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT);
                LOG.info("MNTEST creating world {}", WORLD);
                mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(20261101L, false, false), WorldPresets::createFlatWorldDimensions, title);
            }
            return;
        }
        if (mc.screen instanceof PauseScreen) {
            mc.setScreen(null);
        }
        worldTicks++;
        if (worldTicks == 1) {
            LOG.info("MNTEST joined");
        } else if (worldTicks == 60) {
            build(mc);
        } else if (worldTicks == 300) {
            shot(mc);
        } else if (worldTicks == 340) {
            LOG.info("MNTEST done");
            finish(mc);
        }
    }

    private static void deleteOldWorld(Minecraft mc) {
        LevelStorageSource source = mc.getLevelSource();
        if (!source.levelExists(WORLD)) {
            return;
        }
        try (LevelStorageSource.LevelStorageAccess access = source.createAccess(WORLD)) {
            access.deleteLevel();
            LOG.info("MNTEST deleted old world {}", WORLD);
        } catch (Exception e) {
            LOG.warn("MNTEST could not delete old world {}", WORLD, e);
        }
    }

    /**
     * Superflat: grass at y=-61, the player stands at y=-60. Everything sits in chunk (-1, 0) and the camera in the next chunk
     * south (the section the camera stands in was not redrawn after the fill in earlier runs), facing south (z=12): a 3x3 wall at x=-15..-13, a flat L (column x=-11, foot to x=-9) and a wall at x=-7..-5 that
     * bends north along x=-5; the same three in rosettes at y=-56..-54, and a mixed gilded/rosette wall at x=-3..-1. The camera
     * stands back, looking north.
     */
    private static void build(Minecraft mc) {
        mc.options.hideGui = true;
        mc.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
        String[] commands = {
            "time set noon",
            "weather clear",
            "gamerule advance_time false",
            "gamerule advance_weather false",
            "gamerule spawn_mobs false",
            "fill -10 -60 -6 10 -52 10 minecraft:air",
            "fill -15 -60 12 -13 -58 12 arcananaturae:gilded_greatwood",
            "fill -11 -60 12 -11 -58 12 arcananaturae:gilded_greatwood",
            "fill -10 -60 12 -9 -60 12 arcananaturae:gilded_greatwood",
            "fill -7 -60 12 -5 -58 12 arcananaturae:gilded_greatwood",
            "fill -5 -60 10 -5 -58 11 arcananaturae:gilded_greatwood",
            // the same three shapes in rosettes one row up, and a mixed wall (rosettes at the corners and centre)
            "fill -15 -56 12 -13 -54 12 arcananaturae:gilded_greatwood_rosette",
            "fill -11 -56 12 -11 -54 12 arcananaturae:gilded_greatwood_rosette",
            "fill -10 -56 12 -9 -56 12 arcananaturae:gilded_greatwood_rosette",
            "fill -7 -56 12 -5 -54 12 arcananaturae:gilded_greatwood_rosette",
            "fill -5 -56 10 -5 -54 11 arcananaturae:gilded_greatwood_rosette",
            "fill -3 -60 12 -1 -58 12 arcananaturae:gilded_greatwood",
            "setblock -3 -60 12 arcananaturae:gilded_greatwood_rosette",
            "setblock -1 -60 12 arcananaturae:gilded_greatwood_rosette",
            "setblock -2 -59 12 arcananaturae:gilded_greatwood_rosette",
            "setblock -3 -58 12 arcananaturae:gilded_greatwood_rosette",
            "setblock -1 -58 12 arcananaturae:gilded_greatwood_rosette",
            "tp @a -8 -60 26.5 180 -8",
        };
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) {
            LOG.error("MNTEST no integrated server");
            return;
        }
        server.execute(() -> {
            for (String c : commands) {
                LOG.info("MNTEST /{}", c);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
            }
            LOG.info("MNTEST blocks placed");
        });
    }

    private static void shot(Minecraft mc) {
        String name = "mntest_greatwood_" + (System.currentTimeMillis() / 1000) + ".png";
        File file = new File(new File(mc.gameDirectory, "screenshots"), name);
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1, msg -> LOG.info("MNTEST screenshot message: {}", msg.getString()));
        LOG.info("MNTEST shot {}", file.getAbsolutePath());
    }

    private static void finish(Minecraft mc) {
        finished = true;
        LOG.info("MNTEST stopping client");
        mc.stop();
    }
}
