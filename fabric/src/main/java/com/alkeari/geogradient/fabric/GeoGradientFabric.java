package com.alkeari.geogradient.fabric;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import com.alkeari.geogradient.GeoGradient;
import com.alkeari.geogradient.worldgen.GeoGradientClimate;
import com.alkeari.geogradient.config.GeoGradientConfig;
import com.alkeari.geogradient.config.fabric.GeoGradientFabricConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GeoGradientFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        AutoConfig.register(GeoGradientFabricConfig.class, GsonConfigSerializer::new);

        ServerWorldEvents.LOAD.register((server, level) -> {
            if (!level.dimension().equals(Level.OVERWORLD)) return;

            AutoConfig.getConfigHolder(GeoGradientFabricConfig.class).getConfig().apply();

            GeoGradientClimate.initialize(level.getSeed());
            applySpawnLatitude(level);
        });

        ServerWorldEvents.UNLOAD.register((server, level) -> {
            if (!level.dimension().equals(Level.OVERWORLD)) return;
            GeoGradientClimate.reset();
        });

        GeoGradient.init(() -> AutoConfig.getConfigHolder(GeoGradientFabricConfig.class).getConfig().features.check_for_updates);
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
