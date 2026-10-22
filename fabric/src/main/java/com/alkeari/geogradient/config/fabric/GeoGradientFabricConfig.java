package com.alkeari.geogradient.config.fabric;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import com.alkeari.geogradient.GeoGradient;
import com.alkeari.geogradient.config.GeoGradientConfig;

@Config(name = GeoGradient.MOD_ID)
@SuppressWarnings("CanBeFinal")
public class GeoGradientFabricConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    public int globe_size = 10000;

    @ConfigEntry.Gui.Tooltip
    public double spawn_latitude = 0.0;

    @ConfigEntry.Gui.Tooltip
    public int border_amplitude = 200;

    @ConfigEntry.Gui.Tooltip
    public double border_frequency = 0.002;

    @ConfigEntry.Gui.Tooltip
    public double blend_fraction = 0.15;

    @ConfigEntry.Gui.Tooltip
    public double temperature_variation = GeoGradientConfig.DEFAULT_TEMPERATURE_VARIATION;

    @ConfigEntry.Gui.Tooltip
    public double humidity_variation = GeoGradientConfig.DEFAULT_HUMIDITY_VARIATION;

    @ConfigEntry.Gui.CollapsibleObject
    public Bands bands = new Bands();

    @ConfigEntry.Gui.CollapsibleObject
    public Features features = new Features();

    public static class Features {
        @ConfigEntry.Gui.Tooltip public boolean show_debug_line = true;
        @ConfigEntry.Gui.Tooltip public boolean use_terrablender_regions = true;
        @ConfigEntry.Gui.Tooltip public boolean enable_command = true;
        @ConfigEntry.Gui.Tooltip public boolean check_for_updates = true;
    }

    public static class Bands {
        @ConfigEntry.Gui.CollapsibleObject public Band tropical_wet = new Band(0);
        @ConfigEntry.Gui.CollapsibleObject public Band tropical_dry = new Band(1);
        @ConfigEntry.Gui.CollapsibleObject public Band subtropical = new Band(2);
        @ConfigEntry.Gui.CollapsibleObject public Band temperate = new Band(3);
        @ConfigEntry.Gui.CollapsibleObject public Band subarctic = new Band(4);
        @ConfigEntry.Gui.CollapsibleObject public Band tundra = new Band(5);
        @ConfigEntry.Gui.CollapsibleObject public PolarBand polar = new PolarBand();

        public Band[] edged() {
            return new Band[]{tropical_wet, tropical_dry, subtropical, temperate, subarctic, tundra};
        }
    }

    public static class Band {
        @ConfigEntry.Gui.Tooltip public double ends_at_latitude;
        @ConfigEntry.Gui.Tooltip public double temperature;
        @ConfigEntry.Gui.Tooltip public double humidity;

        public Band() {}

        Band(int i) {
            ends_at_latitude = GeoGradientConfig.DEFAULT_BAND_EDGES[i];
            temperature = GeoGradientConfig.DEFAULT_BAND_TEMPERATURE[i];
            humidity = GeoGradientConfig.DEFAULT_BAND_HUMIDITY[i];
        }
    }

    public static class PolarBand {
        @ConfigEntry.Gui.Tooltip public double temperature = GeoGradientConfig.DEFAULT_BAND_TEMPERATURE[6];
        @ConfigEntry.Gui.Tooltip public double humidity = GeoGradientConfig.DEFAULT_BAND_HUMIDITY[6];
    }

    /** Copies the loaded values into GeoGradientConfig, clamped to the ranges the NeoForge and Forge configs enforce. */
    public void apply() {
        // A hand-edited globe_size of 0 would divide by zero in GeoGradientClimate.computeClimate.
        GeoGradientConfig.globeSize            = Math.max(1000, Math.min(1000000, globe_size));
        GeoGradientConfig.borderAmplitude      = Math.max(0,    Math.min(10000,   border_amplitude));
        GeoGradientConfig.borderFrequency      = Math.max(0.0001, Math.min(0.1,   border_frequency));
        GeoGradientConfig.spawnLatitude        = Math.max(-1.0,   Math.min(1.0,   spawn_latitude));
        GeoGradientConfig.blendFraction        = Math.max(0.0,    Math.min(0.5,   blend_fraction));
        GeoGradientConfig.humidityVariation    = Math.max(0.0,    Math.min(1.0,   humidity_variation));
        GeoGradientConfig.temperatureVariation = Math.max(0.0,    Math.min(1.0,   temperature_variation));
        GeoGradientConfig.showDebugLine          = features.show_debug_line;
        GeoGradientConfig.useTerraBlenderRegions = features.use_terrablender_regions;
        GeoGradientConfig.enableCommand          = features.enable_command;
        Band[] edged = bands.edged();
        double[] edges = new double[6], temperature = new double[7], humidity = new double[7];
        for (int i = 0; i < 6; i++) {
            edges[i] = edged[i].ends_at_latitude;
            temperature[i] = edged[i].temperature;
            humidity[i] = edged[i].humidity;
        }
        temperature[6] = bands.polar.temperature;
        humidity[6] = bands.polar.humidity;
        GeoGradientConfig.applyBands(edges, temperature, humidity);
    }
}
