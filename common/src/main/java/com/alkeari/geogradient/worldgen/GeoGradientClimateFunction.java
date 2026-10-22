package com.alkeari.geogradient.worldgen;

import com.alkeari.geogradient.config.GeoGradientConfig;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Temperature or humidity as GeoGradient lays it out: the latitude band's value plus the original noise, scaled by
 * the variation setting. Wrapping the sampler's own density functions, rather than answering the biome lookup
 * itself, keeps every other mod's hook on that lookup (Biolith replacements, TerraBlender regions) running against
 * GeoGradient's climate.
 */
public record GeoGradientClimateFunction(DensityFunction original, boolean humidity) implements DensityFunction {

    @Override
    public double compute(FunctionContext context) {
        long[] climate = GeoGradientClimate.sampleClimate(context.blockX() >> 2, context.blockZ() >> 2);
        double variation = humidity ? GeoGradientConfig.humidityVariation : GeoGradientConfig.temperatureVariation;
        double value = climate[humidity ? 1 : 0] / 10000.0 + variation * original.compute(context);
        return Math.max(-1.0, Math.min(1.0, value));
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new GeoGradientClimateFunction(original.mapAll(visitor), humidity));
    }

    @Override
    public double minValue() {
        return -1.0;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return original.codec();
    }
}
