package com.alkeari.geogradient.forge;

import com.alkeari.geogradient.GeoGradient;
import com.alkeari.geogradient.worldgen.GeoGradientClimate;
import com.alkeari.geogradient.config.GeoGradientConfig;
import com.alkeari.geogradient.config.forge.GeoGradientForgeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(GeoGradient.MOD_ID)
public class GeoGradientForge {

    @SuppressWarnings("removal")
    public GeoGradientForge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GeoGradientForgeConfig.COMMON_SPEC);
        MinecraftForge.EVENT_BUS.register(this);
        GeoGradient.init(GeoGradientForgeConfig.CHECK_FOR_UPDATES::get);
    }

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(Level.OVERWORLD)) return;

        GeoGradientForgeConfig.apply();

        GeoGradientClimate.initialize(level.getSeed());
        applySpawnLatitude(level);
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        GeoGradientClimate.reset();
    }

    private void applySpawnLatitude(ServerLevel level) {
        double spawnLatitude = GeoGradientConfig.spawnLatitude;
        if (spawnLatitude == 0.0) return;

        Path marker = level.getServer().getWorldPath(LevelResource.ROOT)
                .resolve("geogradient_spawn_applied.flag");
        if (Files.exists(marker)) return;

        int spawnZ = (int) (-spawnLatitude * GeoGradientConfig.globeSize / 2.0);
        BlockPos target = new BlockPos(0, 0, spawnZ);
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, target);
        level.setDefaultSpawnPos(surface, 0.0f);
        GeoGradient.LOGGER.info("GeoGradient: spawn set to {} (latitude {})", surface, spawnLatitude);

        try {
            Files.createFile(marker);
        } catch (IOException e) {
            GeoGradient.LOGGER.warn("GeoGradient: could not write spawn marker file", e);
        }
    }
}
