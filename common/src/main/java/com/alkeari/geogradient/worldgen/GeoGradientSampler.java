package com.alkeari.geogradient.worldgen;

import com.alkeari.geogradient.config.GeoGradientConfig;
import net.minecraft.world.level.biome.Climate;

public final class GeoGradientSampler {

    private GeoGradientSampler() {}

    /** The sampler with its temperature and humidity laid out by latitude; a sampler already wrapped is returned as is. */
    public static Climate.Sampler wrap(Climate.Sampler sampler) {
        if (isWrapped(sampler)) return sampler;
        return new Climate.Sampler(
                new GeoGradientClimateFunction(sampler.temperature(), false),
                new GeoGradientClimateFunction(sampler.humidity(), true),
                sampler.continentalness(), sampler.erosion(), sampler.depth(), sampler.weirdness(),
                sampler.spawnTarget());
    }

    public static boolean isWrapped(Climate.Sampler sampler) {
        return sampler.temperature() instanceof GeoGradientClimateFunction;
    }

    public static Climate.TargetPoint transformClimate(
            Climate.TargetPoint original, int noiseX, int noiseZ) {
        long[] climate = GeoGradientClimate.sampleClimate(noiseX, noiseZ);
        // Vanilla's own temperature and humidity noise, scaled, spread each band across the climate
        // columns its single value would otherwise never reach (Dark Forest, Pale Garden).
        long humidity = climate[1] + Math.round(GeoGradientConfig.humidityVariation * original.humidity());
        long temperature = climate[0] + Math.round(GeoGradientConfig.temperatureVariation * original.temperature());
        return new Climate.TargetPoint(
                Math.max(-10000L, Math.min(10000L, temperature)), // temperature - replaced
                Math.max(-10000L, Math.min(10000L, humidity)), // humidity - replaced
                original.continentalness(),  // preserved
                original.erosion(),          // preserved
                original.depth(),            // preserved
                original.weirdness()         // preserved
        );
    }
}
