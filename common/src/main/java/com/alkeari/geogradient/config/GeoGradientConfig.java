package com.alkeari.geogradient.config;

import com.alkeari.geogradient.GeoGradient;

public class GeoGradientConfig {
    public static volatile int    globeSize       = 10000;
    public static volatile int    borderAmplitude = 200;
    public static volatile double borderFrequency = 0.002;
    public static volatile double spawnLatitude   = 0.0;
    public static volatile double blendFraction   = 0.15;
    public static final double DEFAULT_HUMIDITY_VARIATION = 0.7;
    public static final double DEFAULT_TEMPERATURE_VARIATION = 0.4;
    public static volatile double humidityVariation = DEFAULT_HUMIDITY_VARIATION;
    public static volatile double temperatureVariation = DEFAULT_TEMPERATURE_VARIATION;
    public static volatile boolean showDebugLine = true;
    public static volatile boolean useTerraBlenderRegions = true;
    public static volatile boolean enableCommand = true;

    /** Config keys of the seven bands, equator to pole. */
    public static final String[] BAND_IDS = {"tropical_wet", "tropical_dry", "subtropical", "temperate", "subarctic", "tundra", "polar"};

    // Measured by GeoGradientBiomeSurvey against vanilla's own biome shares, with each band held to its Earth
    // climate and its edges within 7.5 degrees of Earth's: no vanilla surface biome falls below a quarter of its
    // vanilla share, and hot bands cover 40% of the world rather than the 64% of equal-area bands.
    public static final double[] DEFAULT_BAND_EDGES = {19, 30, 36, 66, 76, 84};
    public static final double[] DEFAULT_BAND_TEMPERATURE = {0.25, 0.65, 0.25, -0.05, -0.20, -0.50, -0.85};
    public static final double[] DEFAULT_BAND_HUMIDITY = {0.35, -0.60, -0.20, -0.05, 0.20, 0.05, -0.25};

    // Latitude in degrees (0 = equator, 90 = pole) where each band but Polar ends, equator to pole.
    public static volatile double[] bandEdges = DEFAULT_BAND_EDGES.clone();
    public static volatile double[] bandTemperature = DEFAULT_BAND_TEMPERATURE.clone();
    public static volatile double[] bandHumidity = DEFAULT_BAND_HUMIDITY.clone();

    /**
     * Applies a band setup read from a loader's config, or keeps the defaults and logs why when the edges do not
     * climb from the equator to the pole, since a band of zero or negative width has nowhere to generate.
     */
    public static void applyBands(double[] edges, double[] temperature, double[] humidity) {
        double previous = 0.0;
        for (int i = 0; i < edges.length; i++) {
            if (edges[i] <= previous || edges[i] >= 90.0) {
                GeoGradient.LOGGER.warn("GeoGradient: {} ends_at_latitude {} must be above {} and below 90; using the default bands instead.",
                        BAND_IDS[i], edges[i], previous);
                bandEdges = DEFAULT_BAND_EDGES.clone();
                bandTemperature = DEFAULT_BAND_TEMPERATURE.clone();
                bandHumidity = DEFAULT_BAND_HUMIDITY.clone();
                return;
            }
            previous = edges[i];
        }
        bandEdges = edges.clone();
        bandTemperature = clampAll(temperature);
        bandHumidity = clampAll(humidity);
    }

    private static double[] clampAll(double[] values) {
        double[] out = new double[values.length];
        for (int i = 0; i < values.length; i++) out[i] = Math.max(-1.0, Math.min(1.0, values[i]));
        return out;
    }
}
