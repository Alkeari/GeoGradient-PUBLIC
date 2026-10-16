package com.alkeari.geogradient.worldgen;

import com.alkeari.geogradient.compat.terrablender.GeoGradientTerraBlenderBridge;
import com.alkeari.geogradient.config.GeoGradientConfig;
import com.alkeari.geogradient.mixin.MultiNoiseBiomeSourceAccessor;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class GeoGradientClimate {

    // ── 7-band climate definitions ────────────────────────────────────────────
    // Each band's extent, temperature and humidity come from GeoGradientConfig. Distance d runs
    // linearly from the nearest equator (0.0) to the pole (1.0), so a band edge at latitude L
    // degrees sits at d = L / 90. Temperature peaks at Tropical Dry, placing deserts between the
    // jungle belt at the equator and the temperate forests, as Earth's subtropical highs do.
    static final String[] BAND_NAME = {"Tropical Wet", "Tropical Dry", "Subtropical",
                                       "Temperate", "Subarctic", "Tundra", "Polar"};

    static double[] bandUpper() {
        double[] edges = GeoGradientConfig.bandEdges;
        double[] upper = new double[BAND_NAME.length];
        for (int i = 0; i < edges.length; i++) upper[i] = edges[i] / 90.0;
        upper[BAND_NAME.length - 1] = 1.0;
        return upper;
    }

    // ── LRU climate cache ─────────────────────────────────────────────────────
    // Key: packed biome-section coords (x, z); value: float[]{temperature, humidity}.
    // Eliminates repeated SimplexNoise + trig calls for the same biome column.
    // reset() clears this so it does not straddle world-loads.
    private static final int CACHE_SIZE = 1024;
    private static final Map<Long, float[]> CLIMATE_CACHE =
        Collections.synchronizedMap(new LinkedHashMap<Long, float[]>(CACHE_SIZE + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, float[]> eldest) {
                return size() > CACHE_SIZE;
            }
        });

    private static long packKey(int x, int z) {
        return (x & 0xFFFFFFFFL) | ((long) z << 32);
    }

    // ── World state ───────────────────────────────────────────────────────────
    private static volatile SimplexNoise noise;
    private static volatile boolean initialized = false;
    private static volatile BiomeSource storedBiomeSource;

    public static synchronized void initialize(long seed) {
        // No early-return guard: if an external mod (e.g. Genesis) called the two-arg overload
        // during a preview with a different seed, we must overwrite here so the real world seed
        // wins.  reset() is called on world-unload, so double-init within one session is not a
        // realistic concern.
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(seed ^ 0x47656F47L));
        noise = new SimplexNoise(random);
        initialized = true;
    }

    @SuppressWarnings("unused")
    public static synchronized void initialize(long seed, BiomeSource biomeSource) {
        storedBiomeSource = biomeSource;
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(seed ^ 0x47656F47L));
        noise = new SimplexNoise(random);
        initialized = true;
    }

    public static synchronized void reset() {
        initialized = false;
        noise = null;
        storedBiomeSource = null;
        CLIMATE_CACHE.clear();
    }

    @SuppressWarnings("unused")
    public static Holder<Biome> findBiomeForPreview(int x, int y, int z, Climate.Sampler sampler) {
        if (noise == null) return null;
        Climate.TargetPoint original = sampler.sample(x, y, z);
        Climate.TargetPoint adjusted = GeoGradientSampler.transformClimate(original, x, z);
        if (storedBiomeSource instanceof MultiNoiseBiomeSourceAccessor acc) {
            return GeoGradientTerraBlenderBridge.findBiome(acc.geogradient$parameters(), adjusted, x, y, z);
        }
        return null;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isInitialized() {
        return initialized;
    }

    // ── Core climate computation ──────────────────────────────────────────────

    /**
     * Computes raw (un-quantized) {temperature, humidity} for biome-section coords.
     * x and z are biome-section coordinates (block >> 2). Results are cached at
     * biome-section resolution; call reset() between world loads to invalidate.
     */
    static float[] computeClimate(int x, int z) {
        long key = packKey(x, z);
        float[] cached = CLIMATE_CACHE.get(key);
        if (cached != null) return cached;

        // Read config volatiles once into locals - eliminates memory barriers in the hot path.
        int    gs   = GeoGradientConfig.globeSize;
        int    amp  = GeoGradientConfig.borderAmplitude;
        double freq = GeoGradientConfig.borderFrequency;
        double bf   = GeoGradientConfig.blendFraction;

        // Apply border noise warp. x, z are in biome-section units; amp is in blocks,
        // so divide by 4.0 to convert to biome-section units before adding to z.
        // Multiply effectiveZ by 4.0 below to convert back to blocks for the globeSize comparison.
        double effectiveZ = z + noise.getValue(x * freq, z * freq) * (amp / 4.0);
        double angle      = (effectiveZ * 4.0 / (gs * 2.0)) * 2.0 * Math.PI;

        // Normalized distance d from the nearest equator: 0.0 = equator, 1.0 = pole.
        // Fold the full sine-wave angle into distance from the nearest π/2 peak.
        double modAngle  = ((angle - Math.PI / 2) % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);
        double distAngle = modAngle <= Math.PI ? modAngle : 2 * Math.PI - modAngle;
        double d         = Math.min(distAngle / Math.PI, 1.0);

        float[] result = bandLookup(d, bf);
        CLIMATE_CACHE.put(key, result);
        return result;
    }

    /**
     * Looks up the {temperature, humidity} pair for normalized equatorial distance d.
     * Each boundary gets one cosine blend centered on it, reaching bf/2 of the narrower
     * neighbor's width to either side, so both bands agree on the value at the boundary.
     * bf in [0.0, 0.5] - guaranteed not to overlap the two blend zones within one band.
     * Package-private for unit testing.
     */
    static float[] bandLookup(double d, double bf) {
        double[] upper = bandUpper();
        double[] temp = GeoGradientConfig.bandTemperature;
        double[] hum = GeoGradientConfig.bandHumidity;
        int last = upper.length - 1;
        for (int i = 0; i <= last; i++) {
            if (d <= upper[i]) {
                double lower = i == 0 ? 0.0 : upper[i - 1];

                // Blend toward next band (poleward boundary): first half of the cosine, w from 0 to 0.5
                if (i < last) {
                    double halfBlend = bf * 0.5 * Math.min(width(upper, i), width(upper, i + 1));
                    if (d > upper[i] - halfBlend) {
                        double t = (d - (upper[i] - halfBlend)) / (halfBlend * 2);
                        double w = 0.5 * (1.0 - Math.cos(Math.PI * t));
                        return new float[]{lerp(temp[i], temp[i + 1], w), lerp(hum[i], hum[i + 1], w)};
                    }
                }
                // Blend from previous band (equatorial boundary): second half of the cosine, w from 0.5 to 1
                double halfBlend = i > 0 ? bf * 0.5 * Math.min(width(upper, i - 1), width(upper, i)) : 0.0;
                if (i > 0 && d < lower + halfBlend) {
                    double t = 0.5 + (d - lower) / (halfBlend * 2);
                    double w = 0.5 * (1.0 - Math.cos(Math.PI * t));
                    return new float[]{lerp(temp[i - 1], temp[i], w), lerp(hum[i - 1], hum[i], w)};
                }
                return new float[]{(float) temp[i], (float) hum[i]};
            }
        }
        // d > 1.0: clamp to Polar
        return new float[]{(float) temp[last], (float) hum[last]};
    }

    private static double width(double[] upper, int i) {
        return upper[i] - (i == 0 ? 0.0 : upper[i - 1]);
    }

    private static float lerp(double a, double b, double t) {
        return (float) (a + (b - a) * t);
    }

    /**
     * Returns quantized {temperature, humidity} for biome-section coords.
     * Called by {@link GeoGradientSampler#transformClimate} on every biome lookup.
     * Both values are in Minecraft's quantized range [−10000, +10000].
     */
    public static long[] sampleClimate(int x, int z) {
        if (noise == null) {
            throw new IllegalStateException("GeoGradientClimate.initialize() must be called before sampleClimate()");
        }
        float[] raw = computeClimate(x, z);
        return new long[]{
            Climate.quantizeCoord(raw[0]),
            Climate.quantizeCoord(raw[1])
        };
    }

    /**
     * Backward-compatibility shim. External mods (e.g. Genesis) that call
     * sampleTemperature() via reflection continue to work; returns only the
     * temperature component of sampleClimate().
     */
    public static long sampleTemperature(int x, int z) {
        return sampleClimate(x, z)[0];
    }

    // ── Display / debug helpers ───────────────────────────────────────────────

    public record ClimateInfo(float temp, float humidity, String zone,
                              int distNP, int distEQ, int distSP) {}

    public static ClimateInfo sampleAt(int blockX, int blockZ) {
        int noiseX = blockX >> 2;
        int noiseZ = blockZ >> 2;
        float temp = 0f, humidity = 0f;
        String zone = "Unknown";
        if (noise != null) {
            float[] raw = computeClimate(noiseX, noiseZ);
            temp     = raw[0];
            humidity = raw[1];
            // Recompute d for zone name (display-only; noise is cheap after cache warms).
            int    gs   = GeoGradientConfig.globeSize;
            int    amp  = GeoGradientConfig.borderAmplitude;
            double freq = GeoGradientConfig.borderFrequency;
            double effectiveZ = noiseZ + noise.getValue(noiseX * freq, noiseZ * freq) * (amp / 4.0);
            double angle      = (effectiveZ * 4.0 / (gs * 2.0)) * 2.0 * Math.PI;
            double modAngle   = ((angle - Math.PI / 2) % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);
            double distAngle  = modAngle <= Math.PI ? modAngle : 2 * Math.PI - modAngle;
            double d          = Math.min(distAngle / Math.PI, 1.0);
            double[] upper = bandUpper();
            for (int i = 0; i < upper.length; i++) {
                if (d <= upper[i]) { zone = BAND_NAME[i]; break; }
            }
        }
        int gs         = GeoGradientConfig.globeSize;
        int period     = gs * 2;
        int coldOffset = -(gs / 2);
        int hotOffset  =   gs / 2;
        int npBlock = nearestLandmarkNorth(blockZ, coldOffset, period);
        int eqBlock = nearestLandmark(blockZ, hotOffset, period);
        int spBlock = nearestLandmarkSouth(blockZ, coldOffset, period);
        return new ClimateInfo(temp, humidity, zone,
                               blockZ - npBlock, blockZ - eqBlock, blockZ - spBlock);
    }

    public static float getTemperatureAt(int x, int z) {
        if (noise == null) return 0.0f;
        return computeClimate(x, z)[0];
    }

    public static String formatDist(int diff) {
        if (diff == 0) return "Here";
        return Math.abs(diff) + (diff > 0 ? "N" : "S");
    }

    public static int nearestLandmark(int z, int landmarkOffset, int period) {
        long diff = (long) z - landmarkOffset;
        long k = Math.round((double) diff / period);
        return (int) (landmarkOffset + k * period);
    }

    public static int nearestLandmarkNorth(int z, int offset, int period) {
        long k = Math.floorDiv((long) z - offset, period);
        int atOrNorth = (int) (offset + k * period);
        // Include the landmark itself when z is exactly on it (distance == 0).
        return (atOrNorth <= z) ? atOrNorth : (int) (offset + (k - 1) * period);
    }

    public static int nearestLandmarkSouth(int z, int offset, int period) {
        long k = Math.floorDiv((long) z - offset, period);
        // The floor-division landmark is at or north of z; the next one is strictly south.
        // When z is exactly on the landmark (offset + k*period == z), return z itself so the
        // distance is 0 rather than one full period south.
        int atOrSouth = (int) (offset + k * period);
        return (atOrSouth >= z) ? atOrSouth : (int) (offset + (k + 1) * period);
    }
}
