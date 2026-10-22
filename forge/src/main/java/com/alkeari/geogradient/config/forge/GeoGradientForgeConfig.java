package com.alkeari.geogradient.config.forge;

import com.alkeari.geogradient.config.GeoGradientConfig;
import net.minecraftforge.common.ForgeConfigSpec;

public class GeoGradientForgeConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.IntValue GLOBE_SIZE;
    public static final ForgeConfigSpec.IntValue BORDER_AMPLITUDE;
    public static final ForgeConfigSpec.DoubleValue BORDER_FREQUENCY;
    public static final ForgeConfigSpec.DoubleValue SPAWN_LATITUDE;
    public static final ForgeConfigSpec.DoubleValue BLEND_FRACTION;
    public static final ForgeConfigSpec.DoubleValue HUMIDITY_VARIATION;
    public static final ForgeConfigSpec.DoubleValue TEMPERATURE_VARIATION;
    public static final ForgeConfigSpec.BooleanValue SHOW_DEBUG_LINE;
    public static final ForgeConfigSpec.BooleanValue USE_TERRABLENDER_REGIONS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_COMMAND;
    public static final ForgeConfigSpec.BooleanValue CHECK_FOR_UPDATES;
    public static final ForgeConfigSpec.DoubleValue[] BAND_EDGE = new ForgeConfigSpec.DoubleValue[6];
    public static final ForgeConfigSpec.DoubleValue[] BAND_TEMPERATURE = new ForgeConfigSpec.DoubleValue[7];
    public static final ForgeConfigSpec.DoubleValue[] BAND_HUMIDITY = new ForgeConfigSpec.DoubleValue[7];

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("GeoGradient turns the Z axis into latitude: the climate runs from a pole through the bands to an equator and back. Every change here affects newly generated chunks only.").push("climate");

        GLOBE_SIZE = builder
                .comment("Distance in blocks from a pole to the nearest equator. Spawn (Z = 0) sits halfway, at latitude 45 degrees, and the climate repeats every 2 x globe_size blocks.")
                .defineInRange("globe_size", 10000, 1000, 1000000);

        SPAWN_LATITUDE = builder
                .comment("Moves the world spawn the first time a world loads. 0.0 leaves it at Z = 0 (latitude 45 degrees); -1.0 moves it to the equator and 1.0 to the north pole, with values between in proportion.")
                .defineInRange("spawn_latitude", 0.0, -1.0, 1.0);

        BORDER_AMPLITUDE = builder
                .comment("How far band borders wander north and south, in blocks. 0 makes every border a straight line along X.")
                .defineInRange("border_amplitude", 200, 0, 10000);

        BORDER_FREQUENCY = builder
                .comment("How tightly band borders wiggle. Lower values give long, gentle curves; higher values give short ripples.")
                .defineInRange("border_frequency", 0.002, 0.0001, 0.1);

        BLEND_FRACTION = builder
                .comment("Width of the gradual change between two neighboring bands, as a fraction of the narrower band. 0.0 switches climate at a hard line.")
                .defineInRange("blend_fraction", 0.15, 0.0, 0.5);

        TEMPERATURE_VARIATION = builder
                .comment("How much of vanilla's own temperature noise is added to each band's temperature. 0.0 gives every band one temperature; higher values mix in neighboring biomes, such as savanna inside the desert band.")
                .defineInRange("temperature_variation", GeoGradientConfig.DEFAULT_TEMPERATURE_VARIATION, 0.0, 1.0);

        HUMIDITY_VARIATION = builder
                .comment("How much of vanilla's own humidity noise is added to each band's humidity. 0.0 gives every band one humidity; higher values mix wetter and drier biomes, such as Dark Forest and VanillaBackport's Pale Garden in the temperate band.")
                .defineInRange("humidity_variation", GeoGradientConfig.DEFAULT_HUMIDITY_VARIATION, 0.0, 1.0);

        builder.comment("The seven climate bands, equator to pole. temperature and humidity use vanilla's scale from -1.0 to 1.0. Temperature: below -0.45 frozen, -0.45 to -0.15 cold, -0.15 to 0.2 temperate, 0.2 to 0.55 warm, above 0.55 hot. Humidity: below -0.35 arid, -0.35 to -0.1 dry, -0.1 to 0.1 neutral, 0.1 to 0.3 wet, above 0.3 humid. If the ends_at_latitude values do not climb from the equator toward 90, the default bands are used and the log says why.").push("bands");
        String[] names = {"Tropical Wet", "Tropical Dry", "Subtropical", "Temperate", "Subarctic", "Tundra", "Polar"};
        for (int i = 0; i < 7; i++) {
            builder.comment("The " + names[i] + " band.").push(GeoGradientConfig.BAND_IDS[i]);
            if (i < 6) {
                BAND_EDGE[i] = builder
                        .comment("Latitude in degrees where this band ends and the next band toward the pole begins (0 = equator, 90 = pole).")
                        .defineInRange("ends_at_latitude", GeoGradientConfig.DEFAULT_BAND_EDGES[i], 0.0, 90.0);
            }
            BAND_TEMPERATURE[i] = builder
                    .comment("Temperature at the center of this band.")
                    .defineInRange("temperature", GeoGradientConfig.DEFAULT_BAND_TEMPERATURE[i], -1.0, 1.0);
            BAND_HUMIDITY[i] = builder
                    .comment("Humidity at the center of this band.")
                    .defineInRange("humidity", GeoGradientConfig.DEFAULT_BAND_HUMIDITY[i], -1.0, 1.0);
            builder.pop();
        }
        builder.pop();

        builder.pop();

        builder.comment("Extras GeoGradient adds beside the climate itself.").push("features");
        SHOW_DEBUG_LINE = builder
                .comment("Adds a line to the F3 debug screen with the climate, band and distances to each pole and the equator where you stand.")
                .define("show_debug_line", true);
        USE_TERRABLENDER_REGIONS = builder
                .comment("When TerraBlender is installed, picks each biome through TerraBlender's regions so biome mods built on it keep their biomes. Off uses vanilla's biome lookup.")
                .define("use_terrablender_regions", true);
        ENABLE_COMMAND = builder
                .comment("Registers the /geogradient info command, which reports the climate at your position or at given coordinates. Takes effect when a world loads.")
                .define("enable_command", true);
        CHECK_FOR_UPDATES = builder
                .comment("Once per game start, reads https://alkeari.com/updates/geogradient.json and says in chat when a newer GeoGradient is out for your Minecraft version and loader. Nothing else is sent. Takes effect at the next start.")
                .define("check_for_updates", true);
        builder.pop();
        COMMON_SPEC = builder.build();
    }

    /** Copies the loaded values into GeoGradientConfig. */
    public static void apply() {
        GeoGradientConfig.globeSize            = GLOBE_SIZE.get();
        GeoGradientConfig.borderAmplitude      = BORDER_AMPLITUDE.get();
        GeoGradientConfig.borderFrequency      = BORDER_FREQUENCY.get();
        GeoGradientConfig.spawnLatitude        = SPAWN_LATITUDE.get();
        GeoGradientConfig.blendFraction        = BLEND_FRACTION.get();
        GeoGradientConfig.humidityVariation    = HUMIDITY_VARIATION.get();
        GeoGradientConfig.temperatureVariation = TEMPERATURE_VARIATION.get();
        GeoGradientConfig.showDebugLine          = SHOW_DEBUG_LINE.get();
        GeoGradientConfig.useTerraBlenderRegions = USE_TERRABLENDER_REGIONS.get();
        GeoGradientConfig.enableCommand          = ENABLE_COMMAND.get();
        double[] edges = new double[6], temperature = new double[7], humidity = new double[7];
        for (int i = 0; i < 7; i++) {
            if (i < 6) edges[i] = BAND_EDGE[i].get();
            temperature[i] = BAND_TEMPERATURE[i].get();
            humidity[i] = BAND_HUMIDITY[i].get();
        }
        GeoGradientConfig.applyBands(edges, temperature, humidity);
    }
}
