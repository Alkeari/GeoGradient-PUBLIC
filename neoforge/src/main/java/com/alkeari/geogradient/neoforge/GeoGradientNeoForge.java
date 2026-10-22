package com.alkeari.geogradient.neoforge;

import com.alkeari.geogradient.GeoGradient;
import com.alkeari.geogradient.worldgen.GeoGradientClimate;
import com.alkeari.geogradient.config.GeoGradientConfig;
import com.alkeari.geogradient.config.neoforge.GeoGradientNeoForgeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import com.alkeari.geogradient.neoforge.client.GeoGradientNeoForgeClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
//? if <1.20.5
/*import net.neoforged.fml.ModLoadingContext;*/

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(GeoGradient.MOD_ID)
public class GeoGradientNeoForge {

    public GeoGradientNeoForge(ModContainer modContainer) {
        // ModContainer.registerConfig arrived with NeoForge 20.5; earlier builds register through ModLoadingContext.
        //? if >=1.20.5 {
        modContainer.registerConfig(ModConfig.Type.COMMON, GeoGradientNeoForgeConfig.COMMON_SPEC);
        //?} else
        /*ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GeoGradientNeoForgeConfig.COMMON_SPEC);*/
        if (FMLEnvironment.dist == Dist.CLIENT) GeoGradientNeoForgeClient.registerConfigScreen(modContainer);
        NeoForge.EVENT_BUS.register(this);
        GeoGradient.init(GeoGradientNeoForgeConfig.CHECK_FOR_UPDATES::get);
    }

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(Level.OVERWORLD)) return;

        GeoGradientNeoForgeConfig.apply();

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
